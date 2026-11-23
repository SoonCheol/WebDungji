package com.webdungji.app.ui

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.webdungji.app.BuildConfig
import com.webdungji.app.data.LocalChange
import com.webdungji.app.data.WebAppEntity
import com.webdungji.app.domain.UrlPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private suspend fun createBackup(model: AppViewModel): String = withContext(Dispatchers.IO) {
 val items = JSONArray()
 model.repository.db.webApps().allIncludingDeleted().filter { it.deletedAt == null }.forEach { app ->
  val item = JSONObject().put("id", app.id).put("name", app.name).put("url", app.startUrl)
   .put("order", app.displayOrder).put("javascript", app.javaScript).put("images", app.images)
   .put("zoom", app.textZoom).put("createdAt", app.createdAt)
  item.put("iconRef", app.iconRef ?: JSONObject.NULL)
  model.repository.iconFile(app.iconRef)?.takeIf { it.exists() }?.let {
   item.put("iconData", Base64.encodeToString(it.readBytes(), Base64.NO_WRAP))
  }
  items.put(item)
 }
 JSONObject().put("format", "WebDungjiBackup").put("schema", 1).put("apps", items).toString(2)
}

private suspend fun importBackup(model: AppViewModel, source: String, replace: Boolean): Int = withContext(Dispatchers.IO) {
 require(source.toByteArray(Charsets.UTF_8).size <= 25_000_000)
 val doc = JSONObject(source)
 require(doc.getString("format") == "WebDungjiBackup" && doc.getInt("schema") == 1)
 val apps = doc.getJSONArray("apps")
 require(apps.length() <= 3000)
 val seen = hashSetOf<String>()
 val rows = mutableListOf<WebAppEntity>()
 for (i in 0 until apps.length()) {
  val item = apps.getJSONObject(i)
  val id = item.getString("id")
  require(UUID.fromString(id).toString() == id && seen.add(id))
  val name = item.getString("name").trim()
  require(name.isNotBlank() && name.length <= 120)
  val url = UrlPolicy.registration(item.getString("url")) ?: error("Invalid URL")
  val zoom = item.getInt("zoom")
  require(zoom in 50..200)
  val icon = if (item.has("iconData")) {
   val encoded = item.getString("iconData")
   require(encoded.length < 3_000_000)
   model.repository.storeIcon(Base64.decode(encoded, Base64.DEFAULT)) ?: error("Invalid icon")
  } else item.optString("iconRef").takeIf { it in setOf("preset:nest","preset:globe","preset:star") }
  rows.add(WebAppEntity(id=id, name=name, startUrl=url, iconRef=icon,
   displayOrder=item.getLong("order").coerceAtLeast(0), javaScript=item.getBoolean("javascript"),
   images=item.getBoolean("images"), textZoom=zoom, createdAt=item.getLong("createdAt"),
   updatedAt=System.currentTimeMillis(), dirty=true))
 }
 model.repository.db.withTransaction {
  val dao = model.repository.db.webApps()
  if (replace) { dao.deleteChanges(); dao.deleteAll() }
  rows.forEach { row ->
   val old = dao.find(row.id)
   if (replace || old == null || old.deletedAt != null) {
    val updated = row.copy(revision=(old?.revision ?: 0)+1)
    dao.upsert(updated)
    dao.record(LocalChange(UUID.randomUUID().toString(), row.id, updated.revision, System.currentTimeMillis(), "upsert"))
   }
  }
 }
 rows.size
}

private data class Latest(val version: String, val url: String)
private suspend fun fetchLatest(): Latest = withContext(Dispatchers.IO) {
 val conn = URL("https://api.github.com/repos/SoonCheol/WebDungji/releases/latest").openConnection() as HttpURLConnection
 try {
  conn.connectTimeout=10000; conn.readTimeout=10000
  conn.setRequestProperty("Accept","application/vnd.github+json")
  conn.setRequestProperty("User-Agent","WebDungji")
  check(conn.responseCode == 200)
  val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
  val version = json.getString("tag_name").removePrefix("v")
  require(Regex("""\d+\.\d+\.\d+""").matches(version))
  val assets = json.getJSONArray("assets")
  for (i in 0 until assets.length()) {
   val asset = assets.getJSONObject(i)
   if (asset.getString("name") == "WebDungji-v${version}-debug.apk") {
    val url = asset.getString("browser_download_url")
    require(url.startsWith("https://github.com/SoonCheol/WebDungji/releases/download/"))
    return@withContext Latest(version,url)
   }
  }
  error("APK not available")
 } finally { conn.disconnect() }
}
private fun isNewer(remote:String, installed:String):Boolean {
 val a=remote.split('.').mapNotNull { it.toIntOrNull() }
 val b=installed.split('.').mapNotNull { it.toIntOrNull() }
 if(a.size!=3 || b.size!=3) return false
 for(i in 0..2) if(a[i]!=b[i]) return a[i]>b[i]
 return false
}

@Composable
fun BackupUpdateSection(model:AppViewModel) {
 val context=LocalContext.current
 val scope=rememberCoroutineScope()
 var filename by remember { mutableStateOf("웹둥지-백업") }
 var message by remember { mutableStateOf("") }
 var toRestore by remember { mutableStateOf<String?>(null) }
 var latest by remember { mutableStateOf<Latest?>(null) }
 var checking by remember { mutableStateOf(true) }
 var retry by remember { mutableIntStateOf(0) }
 LaunchedEffect(retry) {
  checking=true
  latest=runCatching { fetchLatest() }.getOrNull()
  checking=false
 }
 val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri:Uri? ->
  if(uri!=null) scope.launch {
   runCatching {
    val bytes=createBackup(model).toByteArray(Charsets.UTF_8)
    withContext(Dispatchers.IO) {
     context.contentResolver.openOutputStream(uri,"wt")?.use { it.write(bytes) } ?: error("Cannot write file")
    }
   }.onSuccess { message="백업 파일을 저장했습니다." }
    .onFailure { message="백업 실패: ${it.message}" }
  }
 }
 val load=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri:Uri? ->
  if(uri!=null) scope.launch {
   runCatching {
    withContext(Dispatchers.IO) {
     val bytes=context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Cannot read file")
     require(bytes.size<=25_000_000)
     bytes.toString(Charsets.UTF_8)
    }
   }.onSuccess { toRestore=it }.onFailure { message="읽기 실패: ${it.message}" }
  }
 }
 HorizontalDivider()
 Text("앱 업데이트",style=MaterialTheme.typography.titleMedium)
 Text("현재 버전 ${BuildConfig.VERSION_NAME} (빌드 ${BuildConfig.VERSION_CODE})")
 Text(if(checking) "최신 버전 확인 중…" else latest?.let { "최신 버전 ${it.version}" } ?: "최신 버전 확인 실패")
 val hasUpdate=latest?.let { isNewer(it.version,BuildConfig.VERSION_NAME) }==true
 if(!checking && latest!=null) Text(if(hasUpdate) "새 버전이 있습니다." else "최신 버전입니다.")
 Button(onClick={
  latest?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.url))) }
 },enabled=hasUpdate,modifier=Modifier.fillMaxWidth()) { Text("업데이트 다운로드") }
 OutlinedButton(onClick={ retry++ },modifier=Modifier.fillMaxWidth()) { Text("업데이트 다시 확인") }
 HorizontalDivider()
 Text("백업 및 복원",style=MaterialTheme.typography.titleMedium)
 Text("웹앱 목록·아이콘·웹앱별 설정을 저장합니다. 쿠키와 로그인 정보는 포함되지 않습니다.",style=MaterialTheme.typography.bodySmall)
 OutlinedTextField(value=filename,onValueChange={filename=it.take(80)},label={Text("백업 파일 이름")},singleLine=true,modifier=Modifier.fillMaxWidth())
 Button(onClick={
  val safe=filename.trim().replace(Regex("""[\\/:*?"<>|]"""),"_").ifBlank { "웹둥지-백업" }
  save.launch("${safe}.json")
 },modifier=Modifier.fillMaxWidth()) { Text("백업 저장 (Google Drive 선택 가능)") }
 OutlinedButton(onClick={load.launch(arrayOf("application/json","application/octet-stream"))},modifier=Modifier.fillMaxWidth()) { Text("백업 선택 및 복원") }
 Text("Google Drive 앱을 파일 저장 위치로 선택할 수 있습니다. 자동 동기화는 아직 구현되지 않았습니다.",style=MaterialTheme.typography.bodySmall)
 if(message.isNotBlank()) Text(message,style=MaterialTheme.typography.bodySmall)
 if(toRestore!=null) AlertDialog(
  onDismissRequest={toRestore=null},title={Text("복원 방식 선택")},
  text={Text("병합: 기존 항목을 유지하고 없는 항목만 추가.\n전체 교체: 등록된 웹앱 목록을 백업본으로 교체 (로그인 데이터는 유지).")},
  confirmButton={TextButton(onClick={
   val data=toRestore ?: return@TextButton
   toRestore=null
   scope.launch {
    runCatching { importBackup(model,data,false) }
     .onSuccess { message="${it}개 항목을 병합했습니다." }
     .onFailure { message="복원 실패: ${it.message}" }
   }
  }) { Text("병합") }},
  dismissButton={Row {
   TextButton(onClick={toRestore=null}) { Text("취소") }
   TextButton(onClick={
    val data=toRestore ?: return@TextButton
    toRestore=null
    scope.launch {
     runCatching { importBackup(model,data,true) }
      .onSuccess { message="${it}개 항목으로 교체했습니다." }
      .onFailure { message="복원 실패: ${it.message}" }
    }
   }) { Text("전체 교체") }
  }}
 )
}
