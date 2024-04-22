package edu.utap.kal

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import java.io.File

// Store files in firebase storage
class Storage {
    // Create a storage reference from our app
    private val photoStorage: StorageReference =
        FirebaseStorage.getInstance().reference.child("images")

    private fun deleteFile(localFile: File, uuid: String) {
        if(localFile.delete()) {
            Log.d(javaClass.simpleName, "Upload FAILED $uuid, file deleted")
        } else {
            Log.d(javaClass.simpleName, "Upload FAILED $uuid, file delete FAILED")
        }
    }
    fun uploadImage(localFile: File, uuid: String, uploadSuccess:()->Unit) {
        //SSS
        val file = Uri.fromFile(localFile)
        val uuidRef = photoStorage.child(uuid)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpg")
            .build()
        val uploadTask = uuidRef.putFile(file, metadata)
        //EEE // XXX Write me

        // Register observers to listen for when the download is done or if it fails
        uploadTask
            .addOnFailureListener {
                // Handle unsuccessful uploads
                deleteFile(localFile, uuid)
            }
            .addOnSuccessListener {
                // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                uploadSuccess()
                deleteFile(localFile, uuid)
            }
    }
    fun deleteImage(pictureUUID: String) {
        // Delete the file
        photoStorage.child(pictureUUID).delete()
            .addOnSuccessListener {
                Log.d(javaClass.simpleName, "Deleted $pictureUUID")
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "Delete FAILED of $pictureUUID")
            }
    }
    fun listAllImages(listSuccess:(List<String>)->Unit) {
        // https://firebase.google.com/docs/storage/android/list-files#list_all_files
        photoStorage.listAll()
            .addOnSuccessListener { listResult ->
                Log.d(javaClass.simpleName, "listAllImages len: ${listResult.items.size}")
                val pictureUUIDs = listResult.items.map {
                    it.name
                }
                listSuccess(pictureUUIDs)
            }
            .addOnFailureListener {
                Log.d(javaClass.simpleName, "listAllImages FAILED")
            }
    }
    fun uuid2StorageReference(pictureUUID: String): StorageReference {
        return photoStorage.child(pictureUUID)
    }
}