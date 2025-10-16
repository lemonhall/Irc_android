package com.lsl.irc_android.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 图片上传管理器
 * 负责处理图片的保存、压缩和上传到图床的逻辑
 */
class ImageUploadManager(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 获取缓存目录中的图片临时文件
     */
    private fun getImageCacheFile(): File {
        val cacheDir = File(context.cacheDir, "images")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "img_${dateFormat.format(Date())}.jpg")
    }

    /**
     * 从相机 URI 读取图片并保存到缓存
     */
    suspend fun saveImageFromUri(uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            return@withContext saveImageFile(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 保存 Bitmap 到文件
     */
    private fun saveImageFile(bitmap: Bitmap): File? {
        return try {
            val file = getImageCacheFile()
            val compressed = compressBitmap(bitmap, 80)
            FileOutputStream(file).use { fos ->
                compressed.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 压缩 Bitmap，返回压缩后的 Bitmap
     *
     * @param bitmap 原始 Bitmap
     */
    private fun compressBitmap(bitmap: Bitmap, @Suppress("UNUSED_PARAMETER") quality: Int): Bitmap {
        // 如果原始 Bitmap 较大，先缩放
        val maxWidth = 1920
        val maxHeight = 1920
        var width = bitmap.width
        var height = bitmap.height

        if (width > maxWidth || height > maxHeight) {
            val ratio = width.toFloat() / height.toFloat()
            if (ratio > 1) {
                width = maxWidth
                height = (width / ratio).toInt()
            } else {
                height = maxHeight
                width = (height * ratio).toInt()
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
        return bitmap
    }

    /**
     * 上传图片到图床
     * 这是一个通用的实现，支持常见的公共图床（如 ImgBB、Imgur 等）
     *
     * @param imageFile 图片文件
     * @param uploadUrl 图床的上传 API 地址（如 https://api.imgbb.com/1/upload）
     * @param apiKey 图床的 API Key（如有需要）
     * @return 上传成功返回图片 URL，失败返回 null
     */
    suspend fun uploadImageToHost(
        imageFile: File,
        uploadUrl: String,
        apiKey: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 读取图片文件
            val imageBytes = imageFile.readBytes()
            
            // 构建 Multipart 请求
            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            
            // 添加文件
            multipartBuilder.addFormDataPart(
                "image",
                imageFile.name,
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            
            // 如果有 API Key，添加它（根据不同的图床可能需要调整参数名）
            if (apiKey != null && apiKey.isNotBlank()) {
                multipartBuilder.addFormDataPart("key", apiKey)
            }
            
            val requestBody = multipartBuilder.build()
            
            val request = Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    // 解析返回的 JSON，获取图片 URL
                    // 这里使用简单的字符串解析，实际项目可以用 JSON 库
                    parseImageUrlFromResponse(responseBody)
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 从响应体中解析图片 URL
     * 这是一个简单的实现，支持常见格式
     * 实际项目中应该根据使用的图床 API 来调整
     */
    private fun parseImageUrlFromResponse(responseBody: String): String? {
        return try {
            // 移除转义字符
            val cleanBody = responseBody.replace("\\", "")
            
            // 尝试从 ImgBB API 响应中提取 URL
            if (cleanBody.contains("\"url\"")) {
                val urlMatch = Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").find(cleanBody)
                val url = urlMatch?.groupValues?.get(1)
                if (url != null && url.isNotBlank()) {
                    return url
                }
            }
            
            // 尝试从其他常见格式中提取 URL
            if (cleanBody.contains("\"data\"")) {
                val urlMatch = Regex("\"link\"\\s*:\\s*\"([^\"]+)\"").find(cleanBody)
                if (urlMatch != null) {
                    return urlMatch.groupValues[1]
                }
            }
            
            // 如果还是没有，直接用正则找任何看起来像 URL 的东西
            val urlPatternMatch = Regex("https?://[^\"\\s,]+").find(cleanBody)
            if (urlPatternMatch != null) {
                return urlPatternMatch.value
            }
            
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 清理缓存中的图片文件
     */
    fun cleanCacheImages() {
        try {
            val cacheDir = File(context.cacheDir, "images")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach {
                    it.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
