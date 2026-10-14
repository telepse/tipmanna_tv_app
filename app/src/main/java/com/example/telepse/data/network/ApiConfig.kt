package com.example.telepse.data.network

import com.example.telepse.BuildConfig

/**
 * API origin for the active product flavor (staging or prod).
 * Values come from [BuildConfig.API_BASE_URL] — do not hard-code hosts here.
 */
object ApiConfig {
    val BASE_URL: String = BuildConfig.API_BASE_URL
}
