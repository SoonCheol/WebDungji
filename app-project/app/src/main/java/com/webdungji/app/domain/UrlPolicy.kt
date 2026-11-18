package com.webdungji.app.domain

import java.net.URI
import java.net.IDN

object UrlPolicy {
 fun normalize(input: String): String? = runCatching {
  if(input.any { it.isISOControl() }) return null
  var value = input.trim()
  if (value.isEmpty() || value.length > 8192 || value.any { it.isISOControl() } || value.contains('\\')) return null
  if (!Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(value)) value = "https://$value"
  val uri = URI(value)
  if (uri.scheme.lowercase() !in setOf("http", "https") || uri.rawUserInfo != null) return null
  val host = uri.host ?: return null
  if (host.isBlank() || uri.port < -1 || uri.port > 65535 || uri.port == 0) return null
  val safeHost = if (host.startsWith("[")) host else IDN.toASCII(host).lowercase()
  val authority = safeHost + if(uri.port >= 0) ":${uri.port}" else ""
  val path = uri.rawPath.ifEmpty { "/" }
  val query = uri.rawQuery?.let { "?$it" } ?: ""
  val fragment = uri.rawFragment?.let { "#$it" } ?: ""
  URI("${uri.scheme.lowercase()}://$authority$path$query$fragment").toASCIIString()
 }.getOrNull()
 /** Registration metadata must not become a container for authentication secrets. */
 fun registration(input: String): String? {
  val safe = normalize(input) ?: return null
  val uri = URI(safe)
  val sensitive = setOf("access_token", "refresh_token", "id_token", "token", "password", "passwd", "authorization", "authorization_code", "api_key", "api-key")
  val keys = listOfNotNull(uri.rawQuery, uri.rawFragment).flatMap { it.split('&') }.map {
   runCatching { java.net.URLDecoder.decode(it.substringBefore('='), "UTF-8").lowercase() }.getOrDefault("")
  }
  return safe.takeUnless { keys.any { it in sensitive } || ("code" in keys && "state" in keys) }
 }
 fun isWeb(url: String) = normalize(url) != null
 fun origin(url: String): String? = runCatching { val u = URI(normalize(url) ?: return null); "${u.scheme}://${u.rawAuthority}" }.getOrNull()
}
