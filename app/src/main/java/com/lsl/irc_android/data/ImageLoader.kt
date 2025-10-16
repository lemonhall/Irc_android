package com.lsl.irc_android.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * 图片加载和缓存管理器
 */
class ImageLoader(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val cacheDir = File(context.cacheDir, "image_cache")

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * 从 URL 加载图片到 ImageView
     */
    suspend fun loadImage(url: String, imageView: ImageView, maxWidth: Int = 1080) {
        return withContext(Dispatchers.IO) {
            try {
                // 先尝试从缓存加载
                val cachedBitmap = loadFromCache(url)
                if (cachedBitmap != null) {
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(cachedBitmap)
                    }
                    return@withContext
                }

                // 从网络下载
                val bitmap = downloadAndCacheBitmap(url, maxWidth)
                if (bitmap != null) {
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 从缓存加载图片
     */
    private fun loadFromCache(url: String): Bitmap? {
        return try {
            val cacheFile = getCacheFile(url)
            if (cacheFile.exists()) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 下载图片并缓存
     */
    private fun downloadAndCacheBitmap(url: String, maxWidth: Int): Bitmap? {
        return try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return null
            }

            val bitmap = response.body?.byteStream()?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return null

            // 缩放图片
            val scaledBitmap = scaleBitmap(bitmap, maxWidth)

            // 保存到缓存
            val cacheFile = getCacheFile(url)
            cacheFile.outputStream().use { fos ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }

            scaledBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 根据最大宽度缩放 Bitmap
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth) {
            return bitmap
        }

        val ratio = height.toFloat() / width.toFloat()
        val newHeight = (maxWidth * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    /**
     * 获取缓存文件
     */
    private fun getCacheFile(url: String): File {
        val fileName = url.hashCode().toString() + ".jpg"
        return File(cacheDir, fileName)
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        try {
            cacheDir.listFiles()?.forEach {
                it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
