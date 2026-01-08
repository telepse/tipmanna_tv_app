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
     * Pusher App ID.
     * Get this from your Pusher dashboard.
     * Note: This is typically only needed on the server side.
     */
    const val APP_ID = "2067685"
    
    /**
     * Pusher API key for authentication.
     * This is the same as PUSHER_KEY in your backend.
     * Get this from your Pusher dashboard.
     */
    const val API_KEY = "09e014941983d1503d77"
    
    /**
     * Pusher secret key.
     * WARNING: This should NEVER be used on the client side!
     * The secret is only for server-side authorization.
     * Client should use the authorization endpoint instead.
     * This is included here for reference only - DO NOT use it in client code.
     */
    const val SECRET = "5fb85d0b922acb8cfb09"
    
    /**
     * Pusher cluster identifier.
     * Common values: "mt1", "eu", "ap1", "ap2", "ap3", "ap4", "us2", "us3"
     * Get this from your Pusher dashboard.
     */
    const val CLUSTER = "us3"
}

