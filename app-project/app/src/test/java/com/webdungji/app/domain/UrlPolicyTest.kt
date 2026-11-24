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
 @Test fun followsHttpsCheckboxEvenForPastedSchemes() {
  assertEquals("https://example.com/", UrlPolicy.registrationWithScheme("example.com", true))
  assertEquals("http://example.com/", UrlPolicy.registrationWithScheme("example.com", false))
  assertEquals("https://example.com/", UrlPolicy.registrationWithScheme("http://example.com", true))
  assertEquals("http://example.com/", UrlPolicy.registrationWithScheme("https://example.com", false))
  assertEquals("example.com/path", UrlPolicy.withoutWebScheme("HTTPS://example.com/path"))
  assertEquals("https://127.0.0.1:8873/path?q=1", UrlPolicy.registrationWithScheme("http://127.0.0.1:8873/path?q=1", true))
 }
 @Test fun rejectsInvalidAddressForSelectedProtocol() {
  listOf("", "https://", "http://", "https://http://example.com", "ftp://example.com", "https:/example.com", "example .com", "/path", "user:pass@example.com", "example.com/?access_token=secret")
   .forEach { assertNull(it, UrlPolicy.registrationWithScheme(it, true)) }
 }
 @Test fun preservesEncodedQueryAndDefaultsToHttps() {
  assertEquals("https://example.com/", UrlPolicy.normalize("example.com"))
  assertEquals("https://example.com/?q=a%26b", UrlPolicy.normalize("https://example.com/?q=a%26b"))
  assertEquals("http://127.0.0.1:8873/", UrlPolicy.normalize("http://127.0.0.1:8873"))
 }
}
