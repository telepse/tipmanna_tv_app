package com.example.telepse.data.realtime

import android.util.Log
import com.example.telepse.data.auth.SessionExpiryHandler
import com.example.telepse.data.auth.SessionStore
import com.example.telepse.data.network.ApiConfig
import com.example.telepse.data.network.AuthenticatedHttpResponseSupport
import com.pusher.client.Authorizer
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Authorizes private Pusher channels via POST /api/v1/tv-app/pusher/auth (no client secret).
 */
class PusherHttpAuthorizer(
    private val httpClient: HttpClient,
    private val sessionStore: SessionStore,
    private val sessionExpiryHandler: SessionExpiryHandler,
    private val eventCodeProvider: () -> String?,
) : Authorizer {

    override fun authorize(channelName: String, socketId: String): String {
        val eventCode = eventCodeProvider()
        return try {
            val responseBody =
                kotlinx.coroutines.runBlocking {
                    val response =
                        httpClient.post("${ApiConfig.BASE_URL}/api/v1/tv-app/pusher/auth") {
                            contentType(ContentType.Application.Json)
                            setBody(
                                PusherAuthRequest(
                                    socket_id = socketId,
                                    channel_name = channelName,
                                    eventCode = eventCode,
                                ),
                            )
                        }
                    if (!response.status.isSuccess()) {
                        val failure =
                            AuthenticatedHttpResponseSupport.failureException(
                                httpStatus = response.status.value,
                                sessionStore = sessionStore,
                                operation = "pusherAuth",
                            )
                        sessionExpiryHandler.invalidateSessionIfNeeded(failure)
                        throw failure
                    }
                    response.bodyAsText()
                }
            Log.d(TAG, "Pusher auth OK for $channelName")
            responseBody
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
