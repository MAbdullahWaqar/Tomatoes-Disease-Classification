package com.tomato.disease.classifier.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object CameraHelper {
    private const val IMAGE_FILE_NAME = "tomato_disease_image.jpg"
    private const val CACHE_DIR = "camera_images"

    fun createImageUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        cacheDir.mkdirs()
        val imageFile = File(cacheDir, IMAGE_FILE_NAME)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    fun getBitmapFromFile(context: Context): Bitmap? {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        val imageFile = File(cacheDir, IMAGE_FILE_NAME)
        return if (imageFile.exists()) {
            BitmapFactory.decodeFile(imageFile.absolutePath)
        } else {
            null
        }
    }

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Handle EXIF rotation
            bitmap = rotateImageIfRequired(context, bitmap, uri)

            // Scale if too large
            bitmap = scaleBitmap(bitmap, 256, 256)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val rotation = when (exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            if (rotation == 0) bitmap else rotateBitmap(bitmap, rotation)
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, CACHE_DIR)
            cacheDir.mkdirs()
            val imageFile = File(cacheDir, IMAGE_FILE_NAME)
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
