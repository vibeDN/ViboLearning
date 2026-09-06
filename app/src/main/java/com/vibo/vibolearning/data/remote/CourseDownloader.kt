package com.vibo.vibolearning.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class CourseDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    /** Fetches raw text from [url]. Normalizes common GitHub blob links to raw. */
    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val normalized = normalize(url.trim())
        val request = Request.Builder().url(normalized).header("Accept", "application/json, text/plain, */*").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Сервер ответил ${response.code}")
            }
            response.body?.string() ?: throw IOException("Пустой ответ")
        }
    }

    private fun normalize(url: String): String = when {
        url.startsWith("github.com/") -> "https://$url"
        url.contains("://github.com/") && url.contains("/blob/") ->
            url.replace("://github.com/", "://raw.githubusercontent.com/").replace("/blob/", "/")
        url.startsWith("http://") || url.startsWith("https://") -> url
        else -> "https://$url"
    }
}
