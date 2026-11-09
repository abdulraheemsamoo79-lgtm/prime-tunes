package com.primetunes.music.data

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit

/**
 * OkHttp-backed [Downloader] for NewPipeExtractor (the standard pattern
 * from the NewPipe app). No API key, no account — plain HTTPS requests
 * with a desktop-browser user agent.
 */
class NewPipeDownloaderImpl private constructor(
    private val client: OkHttpClient,
) : Downloader() {

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .url(url)
            .method(
                httpMethod,
                if (dataToSend != null && dataToSend.isNotEmpty()) dataToSend.toRequestBody(null) else null
            )

        headers.forEach { (name, values) ->
            values.forEach { value -> requestBuilder.addHeader(name, value) }
        }
        if (headers["User-Agent"].isNullOrEmpty()) {
            requestBuilder.addHeader("User-Agent", USER_AGENT)
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val body = response.body?.string() ?: ""
        val headerMap = response.headers.toMultimap()
        return Response(
            response.code,
            response.message,
            headerMap,
            body,
            response.request.url.toString()
        )
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:68.0) Gecko/20100101 Firefox/68.0"

        @Volatile
        private var instance: NewPipeDownloaderImpl? = null

        fun getInstance(): NewPipeDownloaderImpl =
            instance ?: synchronized(this) {
                instance ?: NewPipeDownloaderImpl(
                    OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()
                ).also { instance = it }
            }
    }
}
