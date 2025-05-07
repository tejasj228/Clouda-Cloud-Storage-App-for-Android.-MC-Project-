package com.mcp.oogabooga.supabase

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.buffer
import okio.sink
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import android.os.Handler
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import java.net.URLConnection
import java.time.Instant

object SupabaseStorageHelper {

    private const val TAG = "SupabaseStorage"
    private const val SUPABASE_URL = "https://vyrzfnjdkaerkpuotnka.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ5cnpmbmpka2FlcmtwdW90bmthIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDYzNDgyODIsImV4cCI6MjA2MTkyNDI4Mn0.tn9o0A9xqm4B5mZtE0h4EHXs9nNIY1kBwgFFpsLDFXA"
    private const val BUCKET_NAME = "data"

    private val client = OkHttpClient()

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "file"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex != -1) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }

    fun uploadFile(context: Context, uri: Uri, onSuccess: (() -> Unit)? = null) {
        val contentResolver = context.contentResolver

        val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                cursor.getString(nameIndex)
            } else {
                uri.lastPathSegment ?: "uploaded_file"
            }
        } ?: uri.lastPathSegment ?: "uploaded_file"

        val inputStream = contentResolver.openInputStream(uri)
        if (inputStream == null) {
            showToast(context, "Failed to open file stream.")
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val path = "$userId/$fileName"

        val fileBytes = inputStream.readBytes()
        inputStream.close()

        val requestBody = RequestBody.create(
            "application/octet-stream".toMediaTypeOrNull(),
            fileBytes
        )

        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$path")
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .put(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Upload failed: ${e.message}", e)
                showToast(context, "Upload failed: ${e.message}")
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        showToast(context, "Upload successful")
                        saveFileMetadata(path) // If this relies on the name, it'll now be correct
                        onSuccess?.let {
                            Handler(Looper.getMainLooper()).post {
                                it()
                            }
                        }
                    } else {
                        Log.e(TAG, "Upload failed: ${response.code}")
                        showToast(context, "Upload failed: ${response.message}")
                    }
                }
            }
        })
    }

    fun downloadAndShareFile(context: Context, fileName: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val path = "$userId/$fileName"

        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$path")
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(context, "Share failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.byteStream()?.use { inputStream ->
                    try {
                        // ✅ Ensure cache directory exists
                        val safeFileName = fileName.substringAfterLast('/')
                        val file = File(context.cacheDir, safeFileName)

                        file.parentFile?.mkdirs() // ✅ Ensure parent directory exists

                        file.outputStream().use { output ->
                            inputStream.copyTo(output)
                        }

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        val chooser = Intent.createChooser(shareIntent, "Share file via")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)

                    } catch (e: Exception) {
                        showToast(context, "Failed to share file: ${e.message}")
                    }
                } ?: showToast(context, "Unable to share file.")
            }
        })
    }


    fun renameFile(context: Context, oldPath: String, newFileName: String, onSuccess: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

        // 🔍 Extract original extension
        val originalFileName = oldPath.substringAfterLast('/')
        val extension = originalFileName.substringAfterLast('.', "")

        // ✅ Ensure newFileName includes the extension
        val safeNewFileName = if (newFileName.endsWith(".$extension", ignoreCase = true)) {
            newFileName
        } else {
            "$newFileName.$extension"
        }

        val newPath = "$userId/$safeNewFileName"

        val json = """
        {
            "bucketId": "$BUCKET_NAME",
            "sourceKey": "$oldPath",
            "destinationKey": "$newPath"
        }
    """.trimIndent()

        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/copy")
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Rename failed: ${e.message}", e)
                showToast(context, "Rename failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        // ✅ Delete the old file
                        deleteFile(context, oldPath)
                        showToast(context, "File renamed successfully")
                        updateFileMetadata(oldPath, newPath)

                        Handler(Looper.getMainLooper()).post {
                            onSuccess()
                        }
                    } else {
                        val errorBody = it.body?.string()
                        Log.e(TAG, "Rename failed: ${response.code} $errorBody")
                        showToast(context, "Rename failed: ${response.message}")
                    }
                }
            }
        })
    }

    fun uploadFolderToSupabase(context: Context, folderUri: Uri) {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        folder.listFiles()
            .filter { it.isFile }
            .forEach { file ->
                file.uri?.let { uploadFile(context, it) }
            }
    }



    fun downloadFile(context: Context, fileName: String) {
        val url = "$SUPABASE_URL/storage/v1/object/sign/$BUCKET_NAME"

        val json = """
        {
            "expiresIn": 3600,
            "paths": ["$fileName"]
        }
    """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get signed URL: ${e.message}", e)
                showToast(context, "Signed URL request failed")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string()
                        Log.e(TAG, "Signed URL fetch failed: ${it.code}, body: $errorBody")
                        showToast(context, "Error fetching download URL")
                        return
                    }

                    val body = it.body?.string() ?: return

                    try {
                        val jsonArray = JSONArray(body)
                        if (jsonArray.length() == 0) {
                            showToast(context, "No signed URL returned")
                            return
                        }

                        val signedUrl = jsonArray.getJSONObject(0).getString("signedURL")
                        val fullUrl = "$SUPABASE_URL/storage/v1$signedUrl"


                        Log.d(TAG, "Got signed URL: $fullUrl")
                        downloadFromSignedUrl(context, fullUrl, fileName)

                    } catch (e: JSONException) {
                        Log.e(TAG, "Failed to parse signed URL: ${e.message}", e)
                        showToast(context, "Signed URL parsing error")
                    }

                }
            }
        })
    }

    private fun downloadFromSignedUrl(context: Context, fullUrl: String, fileName: String) {
        val request = Request.Builder().url(fullUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Download failed: ${e.message}", e)
                showToast(context, "Download failed: ${e.message}")
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        val errorBody = it.body?.string()
                        Log.e(TAG, "139. Download failed. Code: ${it.code}, message: ${it.message}, body: $errorBody")
                        showToast(context, "139. Download error: ${it.message}")
                        return
                    }

                    try {
                        val simpleName = fileName.substringAfterLast("/")
                        val resolver = context.contentResolver
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, simpleName)
                            put(MediaStore.Downloads.MIME_TYPE, URLConnection.guessContentTypeFromName(simpleName) ?: "application/octet-stream")
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }

                        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        val fileUri = resolver.insert(collection, contentValues)

                        if (fileUri != null) {
                            resolver.openOutputStream(fileUri)?.use { outputStream ->
                                val sink = outputStream.sink().buffer()
                                sink.writeAll(it.body!!.source())
                                sink.close()
                            }

                            contentValues.clear()
                            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                            resolver.update(fileUri, contentValues, null, null)

                            Log.d(TAG, "File downloaded to: $fileUri")
                            showToast(context, "Downloaded to Downloads folder")
                            openFile(context, fileUri)
                        } else {
                            Log.e(TAG, "Failed to insert into MediaStore")
                            showToast(context, "Download failed: Couldn't create file entry")
                        }

                    } catch (ex: Exception) {
                        Log.e(TAG, "Write failed: ${ex.message}", ex)
                        showToast(context, "Write failed: ${ex.message}")
                    }
                }
            }
        })
    }

    private fun openFile(context: Context, fileUri: Uri) {
        val mimeType = context.contentResolver.getType(fileUri) ?: "*/*"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file: ${e.message}", e)
            showToast(context, "No app found to open this file.")
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveFileMetadata(fileName: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val currentTimestamp = Instant.now().toString() // ISO 8601

        val json = """
        {
          "file_name": "$fileName",
          "user_id": "$userId",
          "uploaded_at": "$currentTimestamp"
        }
""".trimIndent()


        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json)

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/files")
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .header("Content-Type", "application/json")
            .post(body)
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Metadata save failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Metadata save failed: ${it.code} ${it.message}")
                    } else {
                        Log.d(TAG, "Metadata saved for $fileName")
                    }
                }
            }
        })
    }

    fun deleteFileMetadata(fileName: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/files?file_name=eq.$fileName&user_id=eq.$userId")
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Metadata delete failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Metadata delete failed: ${it.code} ${it.message}")
                    } else {
                        Log.d(TAG, "Metadata deleted for $fileName")
                    }
                }
            }
        })
    }

    data class FileMeta(
        val file_name: String,
        val uploaded_at: String
    )


    fun getUploadedFiles(onResult: (List<FileMeta>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/files?select=file_name,uploaded_at&user_id=eq.$userId&order=uploaded_at.desc")
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to fetch files: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Fetch failed: ${it.code}")
                        onResult(emptyList())
                        return
                    }

                    val body = it.body?.string() ?: "[]"
                    try {
                        val jsonArray = JSONArray(body)
                        val files = mutableListOf<FileMeta>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val name = obj.getString("file_name")
                            val time = obj.getString("uploaded_at")
                            files.add(FileMeta(file_name = name, uploaded_at = time))
                        }
                        onResult(files)
                    } catch (e: JSONException) {
                        Log.e(TAG, "Failed to parse file metadata: ${e.message}")
                        onResult(emptyList())
                    }
                }
            }
        })
    }

    fun updateFileMetadata(oldPath: String, newPath: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val json = """
        {
          "file_name": "$newPath"
        }
    """.trimIndent()

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), json)

        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/files?file_name=eq.$oldPath&user_id=eq.$userId")
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .header("Content-Type", "application/json")
            .patch(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Metadata update failed: ${e.message}", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e(TAG, "Metadata update failed: ${it.code} ${it.message}")
                    } else {
                        Log.d(TAG, "Metadata updated from $oldPath to $newPath")
                    }
                }
            }
        })
    }

    fun deleteFile(context: Context, fileName: String) {
        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$BUCKET_NAME/$fileName")
            .delete()
            .header("apikey", SUPABASE_KEY)
            .header("Authorization", "Bearer $SUPABASE_KEY")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Delete failed: ${e.message}")
                showToast(context, "Delete failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.d(TAG, "File deleted: $fileName")
                        showToast(context, "Deleted $fileName")
                        deleteFileMetadata(fileName);
                    } else {
                        Log.e(TAG, "Delete failed: ${it.code} ${it.message}")
                        showToast(context, "Delete failed: ${it.message}")
                    }
                }
            }
        })
    }

    private fun showToast(context: Context, message: String) {
        android.os.Handler(context.mainLooper).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
