package me.beresnev.downloader.periscope

import io.ktor.client.*
import me.beresnev.downloader.periscope.network.*
import java.io.File

/**
 * @param broadcastId id of the broadcast that can be found in the broadcast URL
 * @param broadcastOutDir output directory specific to the given broadcast, must exist
 */
class SingleBroadcastChatDownloader(
    private val httpClient: HttpClient,
    private val broadcastId: String,
    private val broadcastOutDir: File
) {
    init {
        check(broadcastOutDir.exists() && broadcastOutDir.isDirectory) {
            "Expected broadcast out dir to exist and be a directory"
        }
    }

    // Algorithm:
    // 1. Make a get request for access video to get chat_token; id taken from url: https://proxsee.pscp.tv/api/v2/accessVideoPublic?broadcast_id=1ypKdNrmERQJW&replay_redirect=false
    // 2. Make a get request for chat using chat_token: https://proxsee.pscp.tv/api/v2/accessChatPublic?chat_token=$TOKEN
    // 3. Make a post request to get chat history: https://chatman-replay-us-east-1.pscp.tv/chatapi/v1/history
    // 4. Keep making history post requests with sliding "cursor" that you get from the history response
    suspend fun download() {
        println("Starting download of broadcast $broadcastId")

        println("-- Getting the chat token")
        val chatToken = getChatToken()

        println("-- Getting chat history credentials")
        val (historyEndpoint, token) = getChatHistoryCredentials(chatToken)

        val historyRequest = HistoryRequest(
            accessToken = token,
            cursor = "",
            limit = 1000,
            since = 0,
            url = historyEndpoint
        )

        var index = 0
        var currentRequest: HistoryRequest = historyRequest
        do {
            println("-- Chat history request #${index}")
            val historyResponse = getHistory(index++, currentRequest)
            currentRequest = currentRequest.copy(cursor = historyResponse.cursor)
        } while (historyResponse.messages.isNotEmpty())

        println("Finished downloading files for broadcast $broadcastId")
        println("--------------------------")
    }

    private suspend fun getChatToken(): String {
        val url = "https://proxsee.pscp.tv/api/v2/accessVideoPublic?broadcast_id=$broadcastId&replay_redirect=false"
        val responseJsonFile = broadcastOutDir.resolve("accessVideoPublic.json")
        return httpClient.jsonGetRequest<AccessVideoPublicResponse>(url, dumpResponseTo = responseJsonFile).chatToken
    }

    /**
     * @return endpoint to access token
     */
    private suspend fun getChatHistoryCredentials(chatToken: String): Pair<String, String> {
        val url = "https://proxsee.pscp.tv/api/v2/accessChatPublic?chat_token=$chatToken"
        val responseJsonFile = broadcastOutDir.resolve("accessChatPublic.json")
        val response = httpClient.jsonGetRequest<AccessChatPublicResponse>(url, dumpResponseTo = responseJsonFile)

        // only the base endpoint url is returned, probably done so for load balancing
        val endpoint = response.endpoint + "/chatapi/v1/history"
        return endpoint to response.accessToken
    }

    private suspend fun getHistory(index: Int, request: HistoryRequest): HistoryResponse {
        return httpClient.jsonPostRequest<HistoryRequest, HistoryResponse>(
            url = request.url,
            body = request,
            dumpResponseTo = broadcastOutDir.resolve("history-$index.json")
        )
    }
}

private fun String?.or(default: String) = this.takeIf { it?.isNotBlank() == true } ?: default
