package edu.utap.kal

import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.utap.kal.glide.Glide
import edu.utap.kal.model.Note
import edu.utap.kal.view.TakePictureWrapper

class MainViewModel : ViewModel() {
    // Remember the uuid, and hence file name of file camera will create
    private var pictureUUID = ""
    // Only call this from TakePictureWrapper
    fun takePictureUUID(uuid: String) {
        pictureUUID = uuid
    }

    // LiveData for entire note list, all images
    private var notesList = MutableLiveData<List<Note>>()
    private var notesEmpty = MediatorLiveData<Boolean>().apply {
        addSource(notesList) {
            this.value = it.isNullOrEmpty()
        }
    }

    // Remember what is expanded in NoteAdapter
    private var expandedMap = mutableMapOf<String,Boolean>()
    private var allImages = MutableLiveData<List<String>>()
    // Firestore state
    private val storage = Storage()
    // Database access
    private val dbHelp = ViewModelDBHelper()

    // live data for following list
    private var followingList = MutableLiveData<List<UserCard>>()

    /////////////////////////////////////////////////////////////
    // Notes adapter.  With navigation, fragments are all
    // recycled aggressively, so state must live in viewModel
    fun isExpanded(position: Int) : Boolean {
        val id = notesList.value?.get(position)?.firestoreID ?: ""
        return expandedMap[id] == true
    }
    fun isExpandable(position: Int) : Boolean {
        return notesList.value?.get(position)?.pictureUUIDs?.isNotEmpty() ?: false
    }
    fun toggleExpanded(position: Int) {
        if( isExpandable(position) ) {
            val id = notesList.value?.get(position)?.firestoreID ?: ""
            expandedMap[id] = expandedMap[id] != true
        }
    }

    /////////////////////////////////////////////////////////////
    // Notes, memory cache and database interaction
    fun fetchInitialNotes(callback: ()->Unit) {
        dbHelp.fetchInitialNotes(notesList, callback)
    }
    fun observeNotes(): LiveData<List<Note>> {
        return notesList
    }
    fun observeNotesEmpty(): LiveData<Boolean> {
        return notesEmpty
    }
    // Get a note from the memory cache
    fun getNote(position: Int) : Note {
        val note = notesList.value?.get(position)
        Log.d(javaClass.simpleName, "notesList.value ${notesList.value}")
        Log.d(javaClass.simpleName, "getNode $position list len ${notesList.value?.size}")
        return note!!
    }
    // After we successfully modify the db, we refetch the contents to update our
    // live data.  Hence we always pass in notesList
    fun updateNote(position: Int, text: String, pictureUUIDs: List<String>) {
        val note = getNote(position)
        // Have to update text before calling updateNote
        note.text = text
        note.pictureUUIDs = pictureUUIDs
        dbHelp.updateNote(note, notesList)
    }
    fun createNote(text: String, pictureUUIDs: List<String>) {
        val currentUser = AuthWrap.getCurrentUser()
        val note = Note(
            name = currentUser.name,
            ownerUid = currentUser.uid,
            text = text,
            pictureUUIDs = pictureUUIDs,
            // database sets firestoreID
        )
        dbHelp.createNote(note, notesList)
    }
    fun removeNoteAt(position: Int) {
        //SSS
        val note = getNote(position)
        // Delete all pictures on the server, asynchronously
        note.pictureUUIDs.forEach {
            storage.deleteImage(it)
        }
        //EEE // XXX What do to before we delete note?
        Log.d(javaClass.simpleName, "remote note at pos: $position id: ${note.firestoreID}")
        dbHelp.removeNote(note, notesList)
    }

    /////////////////////////////////////////////////////////////
    // Images
    private fun imageListReturns(pictureUUIDs: List<String>) {
        allImages.value = pictureUUIDs
    }
    // NB: Images are not sorted.  We need to add a timestamp if we want that
    fun refreshAllImages() {
        storage.listAllImages(::imageListReturns)
    }
    fun observeAllImages(): LiveData<List<String>> {
        return allImages
    }

    /////////////////////////////////////////////////////////////
    // We can't just schedule the file upload and return.
    // The problem is that our previous picture uploads can still be pending.
    // So a note can have a pictureFileName that does not refer to an existing file.
    // That violates referential integrity, which we really like in our db (and programming
    // model).
    // So we do not add the pictureFileName to the note until the picture finishes uploading.
    // That means a user won't see their picture updates immediately, they have to
    // wait for some interaction with the server.
    // You could imagine dealing with this somehow using local files while waiting for
    // a server interaction, but that seems error prone.
    // Freezing the app during an upload also seems bad.
    fun pictureSuccess(finished: (String)->Unit) {
        val photoFile = TakePictureWrapper.fileNameToFile(pictureUUID)
        //SSS
        // Upload, which deletes local file and finally our memory of its UUID
        storage.uploadImage(photoFile, pictureUUID) {
            finished(pictureUUID)
            pictureUUID = ""
        }
        //EEE // XXX Write me while preserving referential integrity
    }
    fun pictureFailure() {
        // Note, the camera intent will only create the file if the user hits accept
        // so I've never seen this called
        pictureUUID = ""
    }

    fun glideFetch(pictureUUID: String, imageView: ImageView) {
        Glide.fetch(storage.uuid2StorageReference(pictureUUID),
            imageView)
    }

    //////////////////////////////////////////////////////////////////////
    // Following list +
    private var followingSearchTerm = MutableLiveData<String>()
    private var allUsersList = MutableLiveData<List<UserCard>>()
    private var searchFollowing = MediatorLiveData<List<UserCard>>().apply {
        addSource(followingList) {fullList ->
            Log.d("XXX", "i'm observing followingList")
            value = fullList
        }
        addSource(followingSearchTerm) {
            if (it.isEmpty()) {
                value = followingList.value
            } else {
                val filteredList = allUsersList.value?.filter {user ->
                    user.searchFor(it)
                }
                value = filteredList
            }
        }
    }
    fun setSearchTerm(input: String) {
        followingSearchTerm.value = input
    }
    fun fetchInitialFollowing() {
        val currentUser = AuthWrap.getCurrentUser()
        dbHelp.dbFetchFollowing(searchFollowing, currentUser.uid)
        dbHelp.dbFetchFollowing(followingList, currentUser.uid)
    }
    fun fetchInitialAllUsers() {
        dbHelp.fetchAllUsers() { allUsers ->
            allUsersList.postValue(allUsers)
        }
    }
    fun observeFollowing(): MutableLiveData<List<UserCard>> {
        return searchFollowing
    }
    fun observeFollowingListOnly(): MutableLiveData<List<UserCard>> {
        return followingList
    }
    fun addFollowing(newUID: String) {
        val currentUser = AuthWrap.getCurrentUser()
        dbHelp.addToFollowing(currentUser.uid, newUID, followingList)
    }
    fun removeFollowing(otherUser: UserCard) {
        val currentUser = AuthWrap.getCurrentUser()
        dbHelp.removeFromFollowing(currentUser.uid, otherUser.UID, followingList)
    }
}
