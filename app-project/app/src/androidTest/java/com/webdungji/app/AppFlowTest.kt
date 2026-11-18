package com.webdungji.app

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.webdungji.app.sync.LocalSyncSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
 private val instrumentation = InstrumentationRegistry.getInstrumentation()
 private val device = UiDevice.getInstance(instrumentation)
 private val context = instrumentation.targetContext
 private val application get() = context.applicationContext as WebDungjiApplication
 private val runId = InstrumentationRegistry.getArguments().getString("fixtureRun") ?: "baseline"
 private val mainName = "로컬 둥지 · $runId"
 private val initialName = "등록 검증 · $runId"
 private val temporaryName = "삭제 검증 · $runId"
 private val fixture = "http://127.0.0.1:8873/index.html"
 @Before fun launchApp() {
  val intent = context.packageManager.getLaunchIntentForPackage("com.webdungji.app")!!
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  context.startActivity(intent)
  device.wait(Until.hasObject(By.pkg("com.webdungji.app")), 10000)
  device.waitForIdle()
 }
 private fun waitFor(condition: () -> Boolean) {
  val deadline = System.currentTimeMillis() + 15000
  while(System.currentTimeMillis() < deadline) { if(condition()) return; Thread.sleep(150) }
  assertTrue("Timed out waiting for UI state", condition())
 }
 private fun tag(name: String): UiObject2 {
  var found = device.wait(Until.findObject(By.res(name)), 1200)
  if(found == null && device.hasObject(By.scrollable(true))) {
   repeat(3) { if(found == null) { device.swipe(20, 650, 20, 1900, 30); Thread.sleep(300); found = device.findObject(By.res(name)) } }
   repeat(4) { if(found == null) { device.swipe(20, 1900, 20, 650, 30); Thread.sleep(300); found = device.findObject(By.res(name)) } }
  }
  return found ?: error("Missing UI tag: $name")
 }
 private fun tap(name: String) { android.util.Log.i("WebDungjiTest", "tap:$name"); tag(name).click(); device.waitForIdle() }
 private fun text(value: String): UiObject2 = device.wait(Until.findObject(By.text(value)), 10000) ?: error("Missing text: $value")
 private fun input(name: String, value: String) {
  android.util.Log.i("WebDungjiTest", "input:$name")
  val root = tag(name)
  val field = if(root.className == "android.widget.EditText") root else root.findObject(By.clazz("android.widget.EditText")) ?: error("No editable in $name")
  field.click(); field.text = value
  device.waitForIdle()
  Thread.sleep(500)
  instrumentation.runOnMainSync {
   androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).forEach { activity ->
    activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java).hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
   }
  }
  Thread.sleep(300)
  device.waitForIdle()
 }
 private fun save() {
  val button = tag("save_app")
  assertTrue("Save enabled", button.isEnabled)
  button.click(); device.waitForIdle(); Thread.sleep(600)
 }
 private fun active() = runBlocking(Dispatchers.IO) { application.database.webApps().allIncludingDeleted().filter { it.deletedAt == null } }
 private fun main() = active().first { it.name == mainName }

 @After fun captureFailureContext() { device.dumpWindowHierarchy(java.io.File(context.getExternalFilesDir(null), "test-window.xml")) }
 @Test fun stageOneCrudAndNavigation() {
  runBlocking { application.preferences.setLanguage("ko") }
  text("웹둥지")
  tap("add_app")
  input("url_input", "javascript:alert(1)")
  input("name_input", "주소 검증")
  save()
  tag("validation_error")
  input("url_input", fixture)
  input("name_input", "")
  tap("fetch_metadata")
  waitFor { device.hasObject(By.text("로컬 둥지 테스트")) }
  input("name_input", initialName)
  save()
  waitFor { active().any { it.name == initialName } }
  val first = active().first { it.name == initialName }
  android.util.Log.i("WebDungjiTest", "first:stored")
  assertNotNull("Manifest icon saved", first.iconRef)
  assertTrue(application.repository.iconFile(first.iconRef)?.exists() == true)
  tap("app_${first.id}")
  text("LOCAL_NEST_READY")
  text("JAVASCRIPT_ACTIVE")
  text("NEXT_PAGE_LINK").click()
  text("SECOND_PAGE_READY")
  tap("browser_back")
  text("LOCAL_NEST_READY")
  tap("browser_forward")
  text("SECOND_PAGE_READY")
  tap("browser_reload")
  text("SECOND_PAGE_READY")
  tap("browser_home")
  text("LOCAL_NEST_READY")
  tap("current_address")
  text(fixture)
  text("닫기").click()
  tap("browser_close")
  tap("menu_${first.id}")
  text("편집").click()
  input("name_input", mainName)
  save()
  waitFor { active().any { it.id == first.id && it.name == mainName } }
  assertEquals(mainName, active().first { it.id == first.id }.name)
  assertTrue(active().first { it.id == first.id }.revision >= 2)
  tap("add_app")
  input("url_input", fixture)
  input("name_input", temporaryName)
  tap("preset_2")
  tap("js_switch")
  tap("images_switch")
  save()
  waitFor { active().any { it.name == temporaryName } }
  val temporary = active().first { it.name == temporaryName }
  assertFalse(temporary.javaScript)
  assertFalse(temporary.images)
  assertEquals("preset:star", temporary.iconRef)
  tap("menu_${temporary.id}")
  text("삭제").click()
  text("웹앱을 삭제할까요?")
  text("삭제").click(); device.waitForIdle()
  waitFor { active().none { it.id == temporary.id } }
  runBlocking(Dispatchers.IO) {
   val deleted = application.database.webApps().find(temporary.id)!!
   assertNotNull(deleted.deletedAt)
   assertTrue(deleted.dirty)
   assertTrue(application.database.webApps().changes().any { it.appId == temporary.id && it.operation == "delete" })
   val sync = LocalSyncSource(application.repository).pending()
   assertEquals(1, sync.schemaVersion)
   assertNotNull(sync.apps.first { it.id == first.id }.iconPngBase64)
   assertNotNull(sync.apps.first { it.id == temporary.id }.deletedAt)
   UUID.fromString(first.id)
   assertTrue(first.createdAt > 0)
  }
  tap("settings")
  tap("theme_dark")
  tap("language_en")
  text("Settings")
  tap("settings_back")
  text("WebDungji")
 }

 @Test fun stageTwoRestartPersistenceAndKorean() {
  text("WebDungji")
  val saved = main()
  tap("app_${saved.id}")
  text("SESSION_RESTORED")
  text("STORAGE_RESTORED")
  device.pressBack()
  tag("settings")
  tap("settings")
  assertEquals("en", runBlocking { application.preferences.values.first().language })
  assertEquals("dark", runBlocking { application.preferences.values.first().theme })
  tap("language_ko")
  text("설정")
  tap("theme_system")
  tap("clear_site_data")
  text("사이트 데이터를 지울까요?")
  text("취소").click(); device.waitForIdle()
  tap("settings_back")
  text("웹둥지")
  tap("app_${saved.id}")
  text("SESSION_RESTORED")
  tap("browser_close")
  assertEquals("ko", runBlocking { application.preferences.values.first().language })
 }

 @Test fun syncAckKeepsNewerLocalChanges() = runBlocking(Dispatchers.IO) {
  val repository = application.repository
  val id = UUID.randomUUID().toString()
  try {
   val first = repository.save(com.webdungji.app.data.WebAppEntity(id, "동기화 검증 (임시)", fixture, displayOrder = 0, createdAt = 0, updatedAt = 0))
   val source = LocalSyncSource(repository)
   val sent = source.pending().let { it.copy(apps = it.apps.filter { item -> item.id == id }) }
   val second = repository.save(first.copy(name = "동기화 검증 수정 (임시)"))
   source.acknowledge(sent)
   val current = application.database.webApps().find(id)!!
   assertTrue(current.dirty)
   assertEquals(second.revision, current.revision)
   assertTrue(application.database.webApps().changes().any { it.appId == id && it.revision == second.revision })
  } finally { repository.delete(id) }
 }

 @Test fun galleryImportIsBoundedAndLocal() {
  val resolver = context.contentResolver
  val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "webdungji-owned-fixture-${UUID.randomUUID()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.IS_PENDING, 1) }
  val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
  try {
   val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.rgb(36,102,83)) }
   resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
   resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
   val ref = runBlocking { application.repository.importImage(uri) }
   assertTrue(application.repository.iconFile(ref)?.exists() == true)
  } finally { resolver.delete(uri, null, null) }
 }
}
