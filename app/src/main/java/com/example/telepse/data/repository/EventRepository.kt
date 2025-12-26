package com.example.telepse.data.repository

import com.example.telepse.data.model.Event
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val httpClient: HttpClient
) {
    
    companion object {
        private const val BASE_URL = "https://tipmanna.onrender.com"
    }
    
    /**
     * Fetches events by event code from the API
     */
    suspend fun getEventsByCode(eventCode: String): Result<List<Event>> {
        return try {
            val response = httpClient.get("$BASE_URL/api/v1/events/code/$eventCode") {
                // Add any headers if needed
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val events: List<Event> = response.body()
                    Result.success(events)
                }
                HttpStatusCode.NotFound -> {
                    Result.failure(Exception("No events found for code: $eventCode"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch events: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fetches a single event by ID from the API
     */
    suspend fun getEventById(eventId: String): Result<Event> {
        return try {
            val response = httpClient.get("$BASE_URL/api/v1/events/$eventId") {
                // Add any headers if needed
            }
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    val event: Event = response.body()
                    Result.success(event)
                }
                HttpStatusCode.NotFound -> {
                    Result.failure(Exception("Event not found: $eventId"))
                }
                else -> {
                    Result.failure(Exception("Failed to fetch event: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Validates the code entered by the user
     * This now uses the actual API call
     */
    suspend fun validateCode(code: String): Boolean {
        return try {
            val result = getEventsByCode(code)
            result.isSuccess && result.getOrNull()?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }
}
