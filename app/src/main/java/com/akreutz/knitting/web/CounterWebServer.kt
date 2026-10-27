package com.akreutz.knitting.web

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest

/**
 * A minimal HTTP server that lets a browser on the same network read and change the counters.
 *
 * `GET /` serves [page]; `GET /api/state` and `POST /api/step` need the [pin] in an `X-Pin` header, so a page from
 * another site cannot use them (the header forces a preflight, which this server never allows).
 */
class CounterWebServer(
    private val port: Int,
    private val pin: () -> String,
    private val page: () -> ByteArray,
    private val state: suspend () -> String,
    private val step: suspend (WebStep) -> Unit,
) {
    /** The port the server listens on, once it does; differs from the requested one when that is 0. */
    val boundPort = CompletableDeferred<Int>()

    /** Runs until cancelled. Fails if the port cannot be bound. */
    suspend fun run(): Unit = coroutineScope {
        val server = try {
            withContext(Dispatchers.IO) {
                ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(port))
                }
            }
        } catch (e: IOException) {
            boundPort.completeExceptionally(e)
            throw e
        }
        boundPort.complete(server.localPort)
        launch(Dispatchers.IO) {
            while (isActive) {
                val socket = try {
                    server.accept()
                } catch (e: IOException) {
                    break
                }
                launch { handle(socket) }
            }
        }
        // Closing the socket is what ends the blocking accept above.
        try {
            awaitCancellation()
        } finally {
            server.close()
        }
    }

    private suspend fun handle(socket: Socket): Unit = socket.use {
        socket.soTimeout = READ_TIMEOUT_MS
        try {
            val request = readRequest(BufferedInputStream(socket.getInputStream())) ?: return
            val response = try {
                respond(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Response(500, "text/plain", "Internal error".toByteArray())
            }
            socket.getOutputStream().apply {
                write(response.toBytes())
                flush()
            }
        } catch (e: IOException) {
            // The browser went away or was too slow; nothing to answer.
        }
    }

    private suspend fun respond(request: Request): Response {
        val api = request.path == "/api/state" || request.path == "/api/step"
        if (api && !pinMatches(request.headers["x-pin"])) {
            // Slows down guessing the PIN without blocking other connections.
            delay(WRONG_PIN_DELAY_MS)
            return Response(401, "text/plain", "Wrong PIN".toByteArray())
        }
        return when {
            request.method == "GET" && (request.path == "/" || request.path == "/index.html") ->
                Response(200, "text/html; charset=utf-8", page())
            request.method == "GET" && request.path == "/api/state" -> json(state())
            request.method == "POST" && request.path == "/api/step" -> {
                val tap = WebProtocol.parseStep(request.body) ?: return Response(400, "text/plain", "Bad request".toByteArray())
                step(tap)
                json(state())
            }
            api -> Response(405, "text/plain", "Method not allowed".toByteArray())
            else -> Response(404, "text/plain", "Not found".toByteArray())
        }
    }

    private fun json(body: String) = Response(200, "application/json; charset=utf-8", body.toByteArray())

    private fun pinMatches(given: String?): Boolean =
        given != null && MessageDigest.isEqual(given.toByteArray(), pin().toByteArray())

    private class Request(val method: String, val path: String, val headers: Map<String, String>, val body: String)

    private class Response(val status: Int, val contentType: String, val body: ByteArray) {
        fun toBytes(): ByteArray {
            val reason = when (status) {
                200 -> "OK"
                400 -> "Bad Request"
                401 -> "Unauthorized"
                404 -> "Not Found"
                405 -> "Method Not Allowed"
                else -> "Internal Server Error"
            }
            val head = "HTTP/1.1 $status $reason\r\nContent-Type: $contentType\r\nContent-Length: ${body.size}\r\n" +
                "Cache-Control: no-store\r\nConnection: close\r\n\r\n"
            return head.toByteArray() + body
        }
    }

    private fun readRequest(input: InputStream): Request? {
        val requestLine = input.readLine() ?: return null
        val parts = requestLine.split(' ')
        if (parts.size < 2) return null
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = input.readLine() ?: return null
            if (line.isEmpty()) break
            if (headers.size >= MAX_HEADERS) return null
            val colon = line.indexOf(':')
            if (colon > 0) headers[line.substring(0, colon).trim().lowercase()] = line.substring(colon + 1).trim()
        }
        val length = headers["content-length"]?.toIntOrNull() ?: 0
        if (length !in 0..MAX_BODY) return null
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n < 0) return null
            read += n
        }
        return Request(parts[0], parts[1].substringBefore('?'), headers, String(body))
    }

    private fun InputStream.readLine(): String? {
        val line = StringBuilder()
        while (true) {
            val b = read()
            if (b < 0) return if (line.isEmpty()) null else line.toString()
            if (b == '\n'.code) return line.toString()
            if (b != '\r'.code) line.append(b.toChar())
            if (line.length > MAX_LINE) throw IOException("Line too long")
        }
    }

    private companion object {
        const val READ_TIMEOUT_MS = 5_000
        const val WRONG_PIN_DELAY_MS = 1_000L
        const val MAX_HEADERS = 64
        const val MAX_LINE = 8_192
        const val MAX_BODY = 1_024
    }
}
