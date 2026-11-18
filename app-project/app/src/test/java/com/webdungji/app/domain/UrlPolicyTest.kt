package com.webdungji.app.domain
import org.junit.Assert.*
import org.junit.Test
class UrlPolicyTest {
 @Test fun rejectsUnsafeSchemesAndCredentials() {
  listOf("javascript:alert(1)","file:///etc/passwd","content://test/a","intent://x","https://user:password@example.com", "https://example.com/\n").forEach { assertNull(it, UrlPolicy.normalize(it)) }
 }
 @Test fun keepsAuthenticationSecretsOutOfRegistrationsButAllowsWebNavigation() {
  assertNull(UrlPolicy.registration("https://example.com/?access_token=secret"))
  assertNull(UrlPolicy.registration("https://example.com/#id_token=secret"))
  assertNull(UrlPolicy.registration("https://example.com/?code=secret&state=login"))
  assertTrue(UrlPolicy.isWeb("https://example.com/?access_token=secret"))
 }
 @Test fun preservesEncodedQueryAndDefaultsToHttps() {
  assertEquals("https://example.com/", UrlPolicy.normalize("example.com"))
  assertEquals("https://example.com/?q=a%26b", UrlPolicy.normalize("https://example.com/?q=a%26b"))
  assertEquals("http://127.0.0.1:8873/", UrlPolicy.normalize("http://127.0.0.1:8873"))
 }
}
