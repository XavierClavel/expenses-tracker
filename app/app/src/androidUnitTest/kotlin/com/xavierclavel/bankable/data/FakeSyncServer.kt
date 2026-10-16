package com.xavierclavel.bankable.data

import com.xavierclavel.bankable.api.SyncChanges
import com.xavierclavel.bankable.api.SyncPull
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Stands in for the backend's /sync endpoints: records what the app pushes and the cursors it
 * pulls with, and answers pulls with the queued pages (an empty page once they run out).
 */
class FakeSyncServer {
    val pushes = mutableListOf<SyncChanges>()
    val pulledSince = mutableListOf<Long>()
    val pages = ArrayDeque<SyncPull>()

    /** Runs while a push is in flight, before it is recorded. */
    var duringPush: suspend () -> Unit = {}

    /** When set, every request fails with this status. */
    var failWith: HttpStatusCode? = null

    /** When true, every request fails as if the server couldn't be reached. */
    var unreachable = false

    val requestCount get() = pushes.size + pulledSince.size + failedRequests
    private var failedRequests = 0

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun MockRequestHandleScope.handle(request: HttpRequestData): HttpResponseData {
        if (unreachable) {
            failedRequests++
            throw IOException("Unable to resolve host")
        }
        failWith?.let {
            failedRequests++
            return respond("", it)
        }
        return when {
            request.method == HttpMethod.Post && request.url.encodedPath == "/sync/push" -> {
                duringPush()
                pushes += json.decodeFromString<SyncChanges>((request.body as TextContent).text)
                respond("", HttpStatusCode.OK)
            }
            request.method == HttpMethod.Get && request.url.encodedPath == "/sync/pull" -> {
                val since = request.url.parameters["since"]!!.toLong()
                pulledSince += since
                val page = pages.removeFirstOrNull() ?: SyncPull(revision = since, hasMore = false, changes = SyncChanges())
                respond(
                    json.encodeToString(SyncPull.serializer(), page),
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
            else -> respond("", HttpStatusCode.NotFound)
        }
    }
}
