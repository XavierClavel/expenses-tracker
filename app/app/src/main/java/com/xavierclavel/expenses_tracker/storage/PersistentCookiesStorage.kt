package com.xavierclavel.expenses_tracker.storage

import android.content.Context
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PersistentCookiesStorage(context: Context) : CookiesStorage {

    private val prefs = context.getSharedPreferences("http_cookies", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val cache = mutableListOf<Pair<String, Cookie>>()

    @Serializable
    private data class PersistedCookie(
        val name: String,
        val value: String,
        val domain: String? = null,
        val path: String? = null,
    )

    init {
        prefs.all.forEach { (host, value) ->
            val json = value as? String ?: return@forEach
            runCatching {
                Json.decodeFromString<List<PersistedCookie>>(json).forEach { p ->
                    cache.add(host to Cookie(name = p.name, value = p.value, domain = p.domain, path = p.path))
                }
            }
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        cache.filter { (host, _) -> host == requestUrl.host }.map { it.second }
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        val host = requestUrl.host
        cache.removeAll { (h, c) -> h == host && c.name == cookie.name }
        cache.add(host to cookie)

        val toSave = cache
            .filter { it.first == host }
            .map { (_, c) -> PersistedCookie(c.name, c.value, c.domain, c.path) }
        prefs.edit().putString(host, Json.encodeToString(toSave)).apply()
    }

    override fun close() {}
}
