package com.example.queuenow.data.service

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.queuenow.utils.Constants
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryService {

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val config = mapOf(
            "cloud_name" to Constants.CLOUDINARY_CLOUD_NAME,
            "api_key"    to Constants.CLOUDINARY_API_KEY,
            "api_secret" to Constants.CLOUDINARY_API_SECRET
        )
        MediaManager.init(context, config)
        initialized = true
    }

    suspend fun uploadImage(uri: Uri, folder: String = "queuenow"): String =
        suspendCancellableCoroutine { cont ->
            MediaManager.get().upload(uri)
                .option("folder", folder)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String ?: ""
                        if (cont.isActive) cont.resume(url)
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        if (cont.isActive)
                            cont.resumeWithException(Exception(error.description))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()
        }
}