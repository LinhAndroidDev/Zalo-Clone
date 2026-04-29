package com.example.messageapp.utils

import com.example.messageapp.model.DiaryLinkPreview
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Fetches a URL and parses basic Open Graph / HTML title for a link preview card.
 * Runs blocking I/O — call from a background dispatcher.
 */
object LinkPreviewFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val MAX_HTML_BYTES = 512 * 1024

    fun fetch(rawInput: String): Result<DiaryLinkPreview> {
        val normalized = normalizeUrl(rawInput.trim())
            ?: return Result.failure(IllegalArgumentException("invalid_url"))

        val request = Request.Builder()
            .url(normalized)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            )
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(IllegalStateException("http_${response.code}"))
                }
                val finalUrl = response.request.url.toString()
                val baseHttp = finalUrl.toHttpUrlOrNull()
                val body = response.body ?: return Result.failure(IllegalStateException("empty_body"))
                val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
                val bytes = body.bytes()
                if (bytes.isEmpty()) {
                    return Result.failure(IllegalStateException("empty_body"))
                }
                val take = bytes.size.coerceAtMost(MAX_HTML_BYTES)
                val html = String(bytes, 0, take, charset)

                val titleOg = metaContent(html, "property", "og:title")
                    ?: metaContent(html, "name", "twitter:title")
                val descOg = metaContent(html, "property", "og:description")
                    ?: metaContent(html, "name", "twitter:description")
                val imgRaw = metaContent(html, "property", "og:image")
                    ?: metaContent(html, "name", "twitter:image")

                val titleTag = extractTitleTag(html)
                val host = baseHttp?.host ?: normalized

                val title = listOf(titleOg, titleTag, host)
                    .map { it?.trim().orEmpty() }
                    .firstOrNull { it.isNotEmpty() }
                    .orEmpty()
                    .decodeBasicEntities()

                val description = descOg?.trim()?.decodeBasicEntities().orEmpty()

                val imageUrl = imgRaw?.trim()?.takeIf { it.isNotEmpty() }?.let { src ->
                    resolveUrl(baseHttp, src)
                }

                Result.success(
                    DiaryLinkPreview(
                        url = finalUrl,
                        title = title.ifBlank { host },
                        description = description,
                        imageUrl = imageUrl
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun normalizeUrl(trimmed: String): String? {
        if (trimmed.isEmpty()) return null
        val withScheme = if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return withScheme.toHttpUrlOrNull()?.toString()
    }

    private fun metaContent(html: String, attr: String, value: String): String? {
        val escaped = Pattern.quote(value)
        val p1 = Pattern.compile(
            "<meta[^>]+\\b$attr\\s*=\\s*[\"']$escaped[\"'][^>]+\\bcontent\\s*=\\s*[\"']([^\"']*)[\"']",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        var m = p1.matcher(html)
        if (m.find()) return m.group(1)
        val p2 = Pattern.compile(
            "<meta[^>]+\\bcontent\\s*=\\s*[\"']([^\"']*)[\"'][^>]+\\b$attr\\s*=\\s*[\"']$escaped[\"']",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        m = p2.matcher(html)
        return if (m.find()) m.group(1) else null
    }

    private fun extractTitleTag(html: String): String? {
        val p = Pattern.compile("<title[^>]*>([^<]*)</title>", Pattern.CASE_INSENSITIVE)
        val m = p.matcher(html)
        return if (m.find()) m.group(1)?.trim() else null
    }

    private fun resolveUrl(base: okhttp3.HttpUrl?, relative: String): String {
        val t = relative.trim()
        if (t.startsWith("http://", true) || t.startsWith("https://", true)) return t
        if (base == null) return t
        return base.resolve(t)?.toString() ?: t
    }

    private fun String.decodeBasicEntities(): String {
        return this
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/")
    }
}
