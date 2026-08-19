package com.mtvs.food.food.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FoodImageCacheRepository(private val context: Context) {
  fun saveCapturedPhoto(bitmap: Bitmap): Uri? {
    return try {
      val capturesDir = File(context.cacheDir, CAPTURES_DIR_NAME).apply { mkdirs() }
      val captureFile = File(capturesDir, "food_capture_${SystemClock.elapsedRealtime()}.png")
      FileOutputStream(captureFile).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, output)
      }
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", captureFile)
    } catch (error: IOException) {
      Log.e(TAG, "Failed to persist captured photo", error)
      null
    }
  }

  fun delete(uri: Uri?) {
    if (uri == null) return
    runCatching { context.contentResolver.delete(uri, null, null) }
        .onFailure { Log.w(TAG, "Failed to delete cached photo: $uri", it) }
  }

  companion object {
    private const val TAG = "FoodImageCache"
    private const val CAPTURES_DIR_NAME = "food-captures"
  }
}
