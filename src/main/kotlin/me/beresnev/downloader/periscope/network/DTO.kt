package me.beresnev.downloader.periscope.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * NOTE: not all response properties are present in these DTOs - only those
 * that are needed to download the history. Check examples if you're looking
 * for some particular data.
 */

@Serializable
data class AccessVideoPublicResponse(
    @SerialName("chat_token")
    val chatToken: String,
)

@Serializable
data class AccessChatPublicResponse(
    @SerialName("access_token")
    val accessToken: String,
    val endpoint: String,

    @SerialName("replay_endpoint")
    val replayEndpoint: String,
    @SerialName("replay_access_token")
    val replayAccessToken: String,
) {
    init {
        check(endpoint == replayEndpoint) {
            "endpoint and replayEndpoint mismatch: $endpoint vs $replayEndpoint"
        }
        check(accessToken == replayAccessToken) {
            "accessToken and replayAccessToken mismatch: $accessToken vs $replayAccessToken"
        }
    }
}

@Serializable
data class HistoryRequest(
    @SerialName("access_token")
    val accessToken: String,
    val cursor: String,
    val limit: Int,
    val since: Int,
    val url: String
)

@Serializable
data class HistoryResponse(
    val cursor: String,
    val messages: List<HistoryResponseMessage>
)

@Serializable
data class HistoryResponseMessage(
    val kind: Int,
    val payload: String,
    val signature: String
)
