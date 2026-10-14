package com.example.telepse.data.realtime

import android.util.Log
import com.example.telepse.data.network.ApiConfig
import com.pusher.client.Authorizer
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Authorizes private Pusher channels via POST /api/v1/tv-app/pusher/auth (no client secret).
 */
class PusherHttpAuthorizer(
    private val httpClient: HttpClient,
    private val eventCodeProvider: () -> String?,
) : Authorizer {

    override fun authorize(channelName: String, socketId: String): String {
        val eventCode = eventCodeProvider()
        return try {
            val response =
                kotlinx.coroutines.runBlocking {
                    httpClient.post("${ApiConfig.BASE_URL}/api/v1/tv-app/pusher/auth") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            PusherAuthRequest(
                                socket_id = socketId,
                                channel_name = channelName,
                                eventCode = eventCode,
                            ),
                        )
                    }.bodyAsText()
                }
            Log.d(TAG, "Pusher auth OK for $channelName")
            response
        } catch (e: Exception) {
            Log.e(TAG, "Pusher auth failed for $channelName", e)
            throw e
        }
    }

    @Serializable
    private data class PusherAuthRequest(
        val socket_id: String,
        val channel_name: String,
        val eventCode: String? = null,
    )

    companion object {
        private const val TAG = "PusherHttpAuthorizer"
    }
}
