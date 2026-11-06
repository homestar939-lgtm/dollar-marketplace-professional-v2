package com.dollar.marketplace.b2b

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import java.net.URLEncoder

/** Ktor Server-Sent Events connection for the authenticated B2B session. */
class B2bRealtime(
    private val apiBaseUrl: String,
    private val tokenProvider: () -> String?
) {
    var connected by mutableStateOf(false)
        private set

    private val client = HttpClient { install(SSE) }

    /** Keeps the stream alive, retries interrupted connections, and forwards named events. */
    suspend fun collect(onEvent: (String) -> Unit) {
        val base = apiBaseUrl.trimEnd('/')
        while (currentCoroutineContext().isActive) {
            val token = tokenProvider()?.takeIf { it.isNotBlank() }
            if (token == null) {
                connected = false
                return
            }

            try {
                val encodedToken = URLEncoder.encode(token, Charsets.UTF_8.name())
                client.sse(urlString = "$base/v3/events?token=$encodedToken") {
                    connected = true
                    incoming.collect { event ->
                        val type = event.event
                        if (type != null && type in EVENT_TYPES) onEvent(type)
                    }
                }
            } catch (cancelled: CancellationException) {
                connected = false
                throw cancelled
            } catch (_: Exception) {
                // The loop reconnects after transient Wi-Fi/network failures.
            } finally {
                connected = false
            }

            if (currentCoroutineContext().isActive) delay(RETRY_DELAY_MS)
        }
    }

    fun updateDisconnected() {
        connected = false
    }

    private companion object {
        const val RETRY_DELAY_MS = 3_000L
        val EVENT_TYPES = setOf("order.created", "invoice.approved", "receipt.confirmed")
    }
}
