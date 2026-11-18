package com.webdungji.app.network

import com.webdungji.app.domain.UrlPolicy
import com.webdungji.app.domain.readLimited
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class SiteMetadata(val title: String, val icon: ByteArray?)
class SiteMetadataLoader {
 private fun fetch(address: String, maximum: Int): Pair<String, ByteArray> {
  var current = requireNotNull(UrlPolicy.normalize(address))
  repeat(5) {
   val connection = URL(current).openConnection() as HttpURLConnection
   connection.instanceFollowRedirects = false
   connection.connectTimeout = 6000
   connection.readTimeout = 6000
   connection.setRequestProperty("User-Agent", "WebDungji/0.1 metadata")
   try {
    val status = connection.responseCode
    if(status in 300..399) {
     val next = URI(current).resolve(connection.getHeaderField("Location") ?: error("Redirect missing")).toString()
     val safe = requireNotNull(UrlPolicy.normalize(next))
     require(!(current.startsWith("https:") && safe.startsWith("http:")))
     current = safe
    } else {
     require(status in 200..299)
     require(connection.contentLengthLong <= maximum)
     return current to connection.inputStream.use { it.readLimited(maximum) }
    }
   } finally { connection.disconnect() }
  }
  error("Too many redirects")
 }
 suspend fun load(address: String): SiteMetadata = withContext(Dispatchers.IO) {
  val (finalUrl, bytes) = fetch(address, 1_000_000)
  val document = Jsoup.parse(bytes.inputStream(), null, finalUrl)
  val candidates = mutableListOf<String>()
  val manifestUrl = document.selectFirst("link[rel=manifest]")?.absUrl("href")
  if(!manifestUrl.isNullOrBlank()) runCatching {
   val (resolved, manifestBytes) = fetch(manifestUrl, 100_000)
   val icons = JSONObject(String(manifestBytes, Charsets.UTF_8)).optJSONArray("icons")
   if(icons != null) for(index in 0 until minOf(icons.length(), 8)) {
    val item = icons.optJSONObject(index) ?: continue
    val src = item.optString("src")
    if(src.isNotBlank()) candidates += URI(resolved).resolve(src).toString()
   }
  }
  document.select("link[rel~=^(?i)(shortcut )?icon$],link[rel=apple-touch-icon]").take(4).forEach { candidates += it.absUrl("href") }
  candidates += URI(finalUrl).resolve("/favicon.ico").toString()
  var icon: ByteArray? = null
  for(candidate in candidates.distinct().take(5)) {
   if(!UrlPolicy.isWeb(candidate)) continue
   val image = runCatching { fetch(candidate, 2_000_000).second }.getOrNull()
   if(image != null) { icon = image; break }
  }
  SiteMetadata(document.title().trim().take(120), icon)
 }
}
