package com.example.vent.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream

object FileUtils {
    fun prepareFilePart(context: Context, partName: String, fileUri: Uri): MultipartBody.Part? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(fileUri) ?: "application/octet-stream"

        var fileName = "file"
        contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }

        val inputStream: InputStream = contentResolver.openInputStream(fileUri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()

        val requestFile = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(partName, fileName, requestFile)
    }
}