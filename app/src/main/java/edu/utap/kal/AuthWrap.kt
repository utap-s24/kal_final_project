package edu.utap.kal

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase


// This is our abstract concept of a User, which is visible
// outside AuthWrap.  That way, client code will not change
// if we use something other than Firebase for authentication
data class User (private val nullableName: String?,
                 private val nullableEmail: String?,
                 val uid: String) {
    val name: String = nullableName ?: "User logged out"
    val email: String = nullableEmail ?: "User logged out"
}
val invalidUser = User(null, null,
    "User logged out")

// https://firebase.google.com/docs/auth/android/firebaseui
// https://firebase.google.com/docs/auth/android/manage-users
// This auth is a singleton.  It is an object and there is only one of them.
// That allows us to centralize our logic related to authentication.
// Objects have the same lifetime as an application, so they are like
// view models.
// This one exports live data in the form of an abstract user.
// That way, client code won't have to change if we change our
// authentication service.
object AuthWrap : FirebaseAuth.AuthStateListener {
    private const val TAG = "AuthWrap"
    private var liveUser = MutableLiveData<User>().apply {
        this.postValue(invalidUser)
    }
    fun getCurrentUser(): User {
        return liveUser.value!!
    }
    // MainActivity gives this to us, so we can log back in
    // if we are logged out
    lateinit var signInLauncher: ActivityResultLauncher<Intent>
    // When the object is constructed, it listens to FirebaseAuth state
    // That way, if the server logs us out, we know it and change the view
    init {
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }
    // Active live data upon a change of state for our FirebaseUser
    private fun postUserUpdate(firebaseUser: FirebaseUser?) {
        if(firebaseUser == null) {
            // No disconnected operation, if we are logged out, then
            // log back in
            login()
        } else {
            //SSS
            val user = User(firebaseUser.displayName,
                firebaseUser.email, firebaseUser.uid)
            liveUser.postValue(user)
            //EEE

            // Check if user already exists in database... else, make a document for them
            val db = ViewModelDBHelper()
            db.checkAndCreate(firebaseUser)
            db.checkNotesCollection(firebaseUser)
            db.checkCreateChats(firebaseUser)
        }
    }
    // This override makes us a valid FirebaseAuth.AuthStateListener
    override fun onAuthStateChanged(p0: FirebaseAuth) {
        //SSS
        val firebaseUser = p0.currentUser
        postUserUpdate(firebaseUser)
        //EEE // XXX Write me
    }
    private fun user(): FirebaseUser? {
        return Firebase.auth.currentUser
    }

    fun login() {
        if (user() == null) {
            Log.d(TAG, "XXX user null")
            // Choose authentication providers
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build()
            )

            //SSS
            val signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build()
            signInLauncher.launch(signInIntent)
            //EEE // XXX Write me. Create and launch sign-in intent
            // setIsSmartLockEnabled(false) solves some problems
        }
    }

    fun logout() {
        Firebase.auth.signOut()
    }
}
