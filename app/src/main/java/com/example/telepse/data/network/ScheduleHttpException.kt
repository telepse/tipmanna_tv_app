package com.example.telepse.data.network

class ScheduleHttpException(
    val httpStatus: Int,
    val authPresent: Boolean,
    message: String = "Schedule HTTP $httpStatus",
) : Exception(message)
