package edu.utap.kal

import android.text.SpannableString
import android.util.Log
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.utap.kal.model.Note

class ViewModelDBHelper() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collectionRoot = "allNotes"
    private val collectionUser = "allUsers"

    private fun elipsizeString(string: String) : String {
        if(string.length < 10)
            return string
        return string.substring(0..9) + "..."
    }

    fun fetchInitialNotes(notesList: MutableLiveData<List<Note>>,
                          userUID: String,
                          callback:()->Unit) {
        dbFetchNotes(notesList, userUID, callback)
    }
    /////////////////////////////////////////////////////////////
    // Interact with Firestore db
    // https://firebase.google.com/docs/firestore/query-data/get-data
    //
    // If we want to listen for real time updates use this
    // .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
    // But be careful about how listener updates live data
    // and noteListener?.remove() in onCleared
    private fun dbFetchNotes(notesList: MutableLiveData<List<Note>>,
                             userUID: String,
                             callback:()->Unit = {}) {
        db.collection(collectionRoot).document(userUID).collection("Notes")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { result ->
                Log.d(javaClass.simpleName, "allNotes fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                notesList.postValue(result.documents.mapNotNull {
                    it.toObject(Note::class.java)
                })
                callback()
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "allNotes fetch FAILED ", it)
                callback()
            }
    }

    fun fetchOtherNotes(notesList : MutableLiveData<List<Note>>, userUID: String) {
        db.collection(collectionRoot).document(userUID).collection("Notes")
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { result ->
                Log.d(javaClass.simpleName, "other notes fetch ${result!!.documents.size}")
                // NB: This is done on a background thread
                notesList.postValue(result.documents.mapNotNull {
                    it.toObject(Note::class.java)
                })
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "other notes fetch FAILED ", it)
            }
    }

    // After we successfully modify the db, we refetch the contents to update our
    // live data.  Hence the dbFetchNotes() calls below.
    fun updateNote(
        note: Note,
        notesList: MutableLiveData<List<Note>>,
        userUID: String
    ) {
        val pictureUUIDs = note.pictureUUIDs
        //SSS
        db.collection(collectionRoot).document(userUID).collection("Notes")
            .document(note.firestoreID)
            .set(note)
                //EEE // XXX Writing a note
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "Note update \"${elipsizeString(note.text)}\" len ${pictureUUIDs.size} id: ${note.firestoreID}"
                )
                dbFetchNotes(notesList, userUID)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "Note update FAILED \"${elipsizeString(note.text)}\"")
                Log.w(javaClass.simpleName, "Error ", e)
            }
    }

    fun createNote(
        note: Note,
        notesList: MutableLiveData<List<Note>>,
        userUID: String
    ) {
        // We can get ID locally
        // note.firestoreID = db.collection("allNotes").document().id

        val collectionRef = db.collection(collectionRoot).document(userUID).collection("Notes")

        collectionRef
            .add(note)
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "Note create \"${elipsizeString(note.text)}\" id: ${note.firestoreID}"
                )
                dbFetchNotes(notesList, userUID)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "Note create FAILED \"${elipsizeString(note.text)}\"")
                Log.w(javaClass.simpleName, "Error ", e)
            }
    }

    fun removeNote(
        note: Note,
        notesList: MutableLiveData<List<Note>>,
        userUID: String
    ) {
        db.collection(collectionRoot).document(userUID).collection("Notes")
            .document(note.firestoreID)
            .delete()
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "Note delete \"${elipsizeString(note.text)}\" id: ${note.firestoreID}"
                )
                dbFetchNotes(notesList, userUID)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "Note deleting FAILED \"${elipsizeString(note.text)}\"")
                Log.w(javaClass.simpleName, "Error adding document", e)
            }
    }


    fun dbFetchFollowing(
        followingList: MutableLiveData<List<UserCard>>,
        userUID: String
    ) {
        db.collection(collectionUser).document(userUID)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val followingUIDs = documentSnapshot.get("followingUIDs") as? List<String>
                    if (followingUIDs != null) {
                        val userCards = mutableListOf<UserCard>()
                        if (followingUIDs.isNotEmpty()) { // Check if followingUIDs is not empty
                            // iterate through each UID
                            for (followingUID in followingUIDs) {
                                db.collection(collectionUser).document(followingUID)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            val username = document.getString("username") ?: ""
                                            val bio = document.getString("bio") ?: ""
                                            val usernameSpannable = SpannableString.valueOf(username)
                                            val userCard = UserCard(followingUID, usernameSpannable, bio)
                                            userCards.add(userCard)
                                            if (userCards.size == followingUIDs.size) {
                                                // all user cards have been created
                                                followingList.postValue(userCards)
                                            }
                                        } else {
                                            Log.d(javaClass.simpleName, "User $followingUID does not exist")
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.d(javaClass.simpleName, "Failed to fetch document with UID $followingUID", e)
                                    }
                            }
                        } else {
                            // check if followingUIDs is empty
                            Log.d("XXX", "followingUIDs list is empty")
                            followingList.postValue(emptyList())
                        }
                    } else {
                        Log.d(javaClass.simpleName, "FollowingUIDs field is null or not found")
                    }
                } else {
                    Log.d(javaClass.simpleName, "User document with UID $userUID does not exist")
                }
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Following fetch FAILED")
            }
    }

    fun addToFollowing(
        userUID: String,
        otherUID: String,
        followingList: MutableLiveData<List<UserCard>>
    ) {

        db.collection(collectionUser).document(userUID)
            .update("followingUIDs", FieldValue.arrayUnion(otherUID))
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "Add user $otherUID to following"
                )
                dbFetchFollowing(followingList, userUID)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "User follow FAILED")
                Log.w(javaClass.simpleName, "Error ", e)
            }
    }

    fun removeFromFollowing(
        userUID: String,
        otherUID: String,
        followingList: MutableLiveData<List<UserCard>>
    ) {

        db.collection(collectionUser).document(userUID)
            .update("followingUIDs", FieldValue.arrayRemove(otherUID))
            .addOnSuccessListener {
                Log.d(
                    javaClass.simpleName,
                    "Removed user $otherUID from following"
                )
                // remove from the actual live data as well
                dbFetchFollowing(followingList, userUID)
            }
            .addOnFailureListener { e ->
                Log.d(javaClass.simpleName, "User remove follow FAILED")
                Log.w(javaClass.simpleName, "Error ", e)
            }
    }

    fun fetchNameAndBio(uid: String, nameEditText: EditText, bioEditText: EditText) {
        val userDocRef = db.collection(collectionUser).document(uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("username")
                    val bio = document.getString("bio")

                    // Update the EditText fields with the retrieved name and bio
                    Log.d("XXX", "username: $name, bio: $bio")
                    nameEditText.setText(name)
                    bioEditText.setText(bio)
                } else {
                    Log.d("XXX", "User document not found")
                }
            }
            .addOnFailureListener { e ->
                Log.w("XXX", "Error getting user document", e)
            }
    }

    fun checkAndCreate(user: FirebaseUser) {
        val userDocRef = db.collection(collectionUser).document(user.uid)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d("XXX", "User document already exists")
                } else {
                    // Create a new user document with the UID as the document ID
                    val newUser = hashMapOf(
                        "name" to user.displayName,
                        "email" to user.email,
                        "bio" to "",
                        "followingUIDs" to emptyList<String>()
                    )

                    userDocRef.set(newUser)
                        .addOnSuccessListener {
                            Log.d("XXX", "User document created successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.w("XXX", "Error creating user document", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("XXX", "Error checking user document", e)
            }
    }

    fun checkNotesCollection(user: FirebaseUser) {

        val docRef = db.collection(collectionRoot).document(user.uid)
        docRef.get()
            .addOnSuccessListener {
                if (!it.exists()) {
                    // Document doesn't exist, so create it
                    docRef.set(hashMapOf<String, Any>())
                        .addOnSuccessListener {
                            Log.d("XXX", "User document created successfully")
                            // Create a collection within this called "Notes"
                            val notesRef = docRef.collection("Notes")
                            notesRef.get()
                                .addOnSuccessListener {
                                    // Create the Notes collection
                                    if (it.isEmpty) {
                                        notesRef.add(hashMapOf<String, Any>())
                                            .addOnSuccessListener {
                                                Log.d("XXX", "Notes collection created successfully")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.w("XXX", "Error creating Notes collection", e)
                                            }
                                    } else {
                                        Log.d("XXX", "Notes collection already exists")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.w("XXX", "Error creating Notes collection", e)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w("XXX", "Error creating user document", e)
                        }
                }
            }

    }

    fun updateName(uid : String, newName : String) {
        val userDocRef = db.collection(collectionUser).document(uid)

        userDocRef.update("username", newName)
            .addOnSuccessListener {
                Log.d("XXX", "Username updated successfully")
            }
            .addOnFailureListener { e ->
                Log.w("XXX", "Error updating username", e)
            }
    }

    fun updateBio(uid : String, newName : String) {
        val userDocRef = db.collection(collectionUser).document(uid)

        userDocRef.update("bio", newName)
            .addOnSuccessListener {
                Log.d("XXX", "Bio updated successfully")
            }
            .addOnFailureListener { e ->
                Log.w("XXX", "Error updating bio", e)
            }
    }

    fun fetchAllUsers(callback: (List<UserCard>) -> Unit) {
        db.collection(collectionUser).get()
            .addOnSuccessListener { querySnapshot ->
                val userCards = mutableListOf<UserCard>()
                for (document in querySnapshot.documents) {
                    val uid = document.id
                    val username = document.getString("username") ?: ""
                    val bio = document.getString("bio") ?: ""
                    val usernameSpannable = SpannableString.valueOf(username)
                    val userCard = UserCard(uid, usernameSpannable, bio)
                    userCards.add(userCard)
                }
                callback(userCards)
            }
            .addOnFailureListener { e ->
                Log.w("XXX", "Error fetching all users", e)
            }
    }


}