package com.example.telepse.data.realtime

/**
 * Resolves Pusher client credentials: tv-app bootstrap hints override embedded defaults.
 */
data class ResolvedPusherCredentials(
    val apiKey: String,
    val cluster: String,
    /** `bootstrap` or `embedded_fallback` */
    val source: String,
    val usedBootstrapKey: Boolean,
    val usedBootstrapCluster: Boolean,
)

fun resolvePusherCredentials(
    bootstrapKey: String?,
    bootstrapCluster: String?,
    embeddedKey: String = PusherConfig.API_KEY,
    embeddedCluster: String = PusherConfig.CLUSTER,
): ResolvedPusherCredentials {
    val key = bootstrapKey?.trim()?.takeIf { it.isNotEmpty() } ?: embeddedKey
    val cluster = bootstrapCluster?.trim()?.takeIf { it.isNotEmpty() } ?: embeddedCluster
    val usedBootstrapKey = bootstrapKey?.trim()?.isNotEmpty() == true
    val usedBootstrapCluster = bootstrapCluster?.trim()?.isNotEmpty() == true
    val source =
        if (usedBootstrapKey || usedBootstrapCluster) "bootstrap" else "embedded_fallback"
    return ResolvedPusherCredentials(
        apiKey = key,
        cluster = cluster,
        source = source,
        usedBootstrapKey = usedBootstrapKey,
        usedBootstrapCluster = usedBootstrapCluster,
    )
}

/** Safe log line — never logs full API key. */
fun formatPusherCredentialDiagnostic(credentials: ResolvedPusherCredentials): String {
    val key = credentials.apiKey
    val maskedKey =
        when {
            key.length <= 8 -> "****"
            else -> "${key.take(4)}…${key.takeLast(4)}"
        }
    val matchesEmbeddedKey = key == PusherConfig.API_KEY
    val matchesEmbeddedCluster = credentials.cluster == PusherConfig.CLUSTER
    return buildString {
        append("source=${credentials.source}")
        append(" cluster=${credentials.cluster}")
        append(" key=$maskedKey")
        append(" bootstrapKey=${credentials.usedBootstrapKey}")
        append(" bootstrapCluster=${credentials.usedBootstrapCluster}")
        append(" matchesEmbeddedKey=$matchesEmbeddedKey")
        append(" matchesEmbeddedCluster=$matchesEmbeddedCluster")
    }
}
