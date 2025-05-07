package com.mcp.oogabooga.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.FileOutputStream
import java.util.*
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp

object FirebaseStorageHelper {
    private val storageRef = Firebase.storage.reference

    fun copyUriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_", null, context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    //Log.d("UploadDebug", "Uploading URI: $fileUri")

    fun uploadFile(context: Context, fileUri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("UploadDebug", "User not signed in")
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("UploadDebug", "Starting file upload for user: ${user.uid}")
        Log.d("UploadDebug", "Incoming URI: $fileUri")

        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(fileUri)

            if (inputStream == null) {
                Log.e("UploadDebug", "InputStream is null for URI: $fileUri")
                Toast.makeText(context, "Failed to open file", Toast.LENGTH_LONG).show()
                return
            }

            val fileBytes = inputStream.readBytes()
            Log.d("UploadDebug", "Read ${fileBytes.size} bytes from InputStream")

            val fileName = UUID.randomUUID().toString()
            val storage = FirebaseStorage.getInstance("gs://cloudbase-ac9e5.appspot.com")
            val fileRef = storage.reference.child("uploads/${user.uid}/$fileName")

            Log.d("UploadDebug", "Uploading to Firebase path: uploads/${user.uid}/$fileName")

            fileRef.putBytes(fileBytes)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        Log.d("UploadDebug", "Upload succeeded. File URL: $downloadUrl")

                        // Store metadata in Firestore
                        val metadata = mapOf(
                            "fileName" to fileName,
                            "downloadUrl" to downloadUrl.toString(),
                            "uploadedAt" to Timestamp.now()
                        )

                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .collection("files")
                            .add(metadata)
                            .addOnSuccessListener {
                                Log.d("UploadDebug", "Metadata saved to Firestore")
                                Toast.makeText(context, "Upload complete!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("UploadDebug", "Failed to save metadata: ${e.message}", e)
                            }

                    }.addOnFailureListener { e ->
                        Log.e("UploadDebug", "Failed to get download URL: ${e.message}", e)
                    }
                }
                .addOnFailureListener {
                    Log.e("UploadDebug", "Upload failed: ${it.message}", it)
                    Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Log.e("UploadDebug", "Exception during upload: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    fun downloadFile(context: Context, fileName: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val fileRef = Firebase.storage.reference.child("uploads/${user.uid}/$fileName")
        val localFile = File(context.cacheDir, fileName)

        fileRef.getFile(localFile)
            .addOnSuccessListener {
                Toast.makeText(context, "File downloaded: ${localFile.absolutePath}", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Download failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

}
