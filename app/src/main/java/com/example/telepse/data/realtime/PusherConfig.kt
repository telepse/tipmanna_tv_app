package com.example.telepse.data.realtime

/**
 * Configuration for Pusher real-time service.
 * 
 * TODO: Replace these values with your actual Pusher credentials.
 * These should ideally be stored in a secure configuration file or
 * retrieved from a secure source (e.g., BuildConfig, remote config).
 */
object PusherConfig {
    /**
     * Pusher API key for authentication.
     * Get this from your Pusher dashboard.
     */
    const val API_KEY = "09e014941983d1503d77"
    
    /**
     * Pusher cluster identifier.
     * Common values: "mt1", "eu", "ap1", "ap2", "ap3", "ap4", "us2", "us3"
     * Get this from your Pusher dashboard.
     */
    const val CLUSTER = "us3"
}

