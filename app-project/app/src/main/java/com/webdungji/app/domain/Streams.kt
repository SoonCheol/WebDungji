package com.webdungji.app.domain

import java.io.InputStream
import java.io.ByteArrayOutputStream

fun InputStream.readLimited(maximum: Int): ByteArray {
 val result = ByteArrayOutputStream()
 val buffer = ByteArray(8192)
 var total = 0
 while(true) {
  val count = read(buffer, 0, minOf(buffer.size, maximum + 1 - total))
  if(count < 0) break
  total += count
  require(total <= maximum) { "Content too large" }
  result.write(buffer, 0, count)
 }
 return result.toByteArray()
}
