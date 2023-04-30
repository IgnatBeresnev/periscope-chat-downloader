package me.beresnev.downloader.periscope.network

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36"

private val JSON = Json {
    ignoreUnknownKeys = true
}

internal suspend inline fun <reified R> HttpClient.jsonGetRequest(
    url: String,
    dumpResponseTo: File? = null
): R {
    return this.get(url)
        .also { it.assertOk() }
        .also { dumpResponseTo?.writeJsonBody(it) }
        .let { JSON.decodeFromString<R>(it.bodyAsText()) }
}

internal suspend inline fun <reified T, reified R> HttpClient.jsonPostRequest(
    url: String,
    body: T,
    dumpResponseTo: File? = null
): R {
    val httpResponse = this.post(url) {
        contentType(ContentType.Application.Json)
        userAgent(USER_AGENT)
        setBody(JSON.encodeToString(body))
    }

    return httpResponse
        .also { it.assertOk() }
        .also { dumpResponseTo?.writeJsonBody(httpResponse) }
        .let { JSON.decodeFromString<R>(httpResponse.bodyAsText()) }
}

internal fun HttpResponse.assertOk(): Unit = check(this.status == HttpStatusCode.OK) {
    "Expected status to be OK, got $this"
}

internal suspend fun File.writeJsonBody(response: HttpResponse) {
    check(!this.exists() && !this.isDirectory) {
        "Expected the file to not exist or not be a directory: $this"
    }
    if (!this.createNewFile()) {
        throw IllegalStateException("Unable to create a file: $this")
    }
    this.writeText(response.bodyAsText())
}
