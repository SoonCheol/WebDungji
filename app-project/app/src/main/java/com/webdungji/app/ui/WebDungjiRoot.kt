package com.webdungji.app.ui

import android.graphics.BitmapFactory
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webdungji.app.BuildConfig
import com.webdungji.app.data.WebAppEntity
import com.webdungji.app.domain.UrlPolicy
import com.webdungji.app.network.SiteMetadataLoader
import com.webdungji.app.web.BrowserScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDungjiRoot(model: AppViewModel) {
 val apps by model.apps.collectAsStateWithLifecycle()
 val appearance by model.appearance.collectAsStateWithLifecycle()
 val dark = when(appearance.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
 val colors = if(dark) darkColorScheme(primary = Color(0xFF8DD9BB), secondary = Color(0xFFF7C77E), background = Color(0xFF101C19), surface = Color(0xFF15231F))
  else lightColorScheme(primary = Color(0xFF246653), secondary = Color(0xFF8A5B28), background = Color(0xFFF8FAF5), surface = Color(0xFFF8FAF5), primaryContainer = Color(0xFFDDF0E4))
 var screen by rememberSaveable { mutableStateOf("home") }
 var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
 var editingId by rememberSaveable { mutableStateOf<String?>(null) }
 var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
 var failure by remember { mutableStateOf(false) }
 val scope = rememberCoroutineScope()
 CompositionLocalProvider(LocalEnglish provides (appearance.language == "en")) {
  MaterialTheme(colorScheme = colors) {
   Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
    BackHandler(screen != "home" && screen != "browser") { screen = "home" }
    when(screen) {
     "editor" -> key(editingId) { EditorScreen(model, apps.find { it.id == editingId }, { screen = "home" }) }
     "settings" -> SettingsScreen(model, { screen = "home" }, { editingId = it; screen = "editor" })
     "browser" -> {
      val selected = apps.find { it.id == selectedId }
      if(selected != null) BrowserScreen(selected, { screen = "home" })
      else LaunchedEffect(apps) { screen = "home" }
     }
     else -> Scaffold(
      topBar = { TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically) {
       Icon(Icons.Outlined.Cottage, null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
       Spacer(Modifier.width(10.dp)); Text(label("웹둥지", "WebDungji"), fontWeight = FontWeight.Bold)
      } }, actions = { IconButton(onClick = { screen = "settings" }, modifier = Modifier.testTag("settings")) { Icon(Icons.Outlined.Settings, label("설정", "Settings")) } }) },
      floatingActionButton = { ExtendedFloatingActionButton(onClick = { editingId = null; screen = "editor" }, modifier = Modifier.testTag("add_app"), icon = { Icon(Icons.Outlined.Add, null) }, text = { Text(label("웹앱 추가", "Add web app")) }) }
     ) { padding ->
      Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp)) {
       Text(label("자주 찾는 곳을, 한 둥지에.", "Your favorite places, together."), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
       Spacer(Modifier.height(8.dp))
       Text(label("내 웹앱 · ${apps.size}개", "My web apps · ${apps.size}"), color = MaterialTheme.colorScheme.onSurfaceVariant)
       Spacer(Modifier.height(24.dp))
       if(apps.isEmpty()) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
         Icon(Icons.Outlined.Cottage, null, Modifier.size(76.dp), tint = MaterialTheme.colorScheme.primary)
         Spacer(Modifier.height(20.dp))
         Text(label("아직 둥지가 비어 있어요", "Your nest is ready"), style = MaterialTheme.typography.titleLarge)
         Spacer(Modifier.height(8.dp))
         Text(label("웹앱 추가를 눌러 자주 찾는 사이트를 모아 보세요.", "Add a web app to keep your favorite sites here."), style = MaterialTheme.typography.bodyMedium)
        }
       } else LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(apps, key = { it.id }) { app ->
         var menu by remember { mutableStateOf(false) }
         Card(onClick = { selectedId = app.id; screen = "browser" }, modifier = Modifier.testTag("app_${app.id}"), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
          Column(Modifier.fillMaxWidth().padding(16.dp)) {
           Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.iconRef, model, Modifier.size(48.dp))
            Box { IconButton(onClick = { menu = true }, modifier = Modifier.testTag("menu_${app.id}")) { Icon(Icons.Outlined.MoreVert, label("웹앱 메뉴", "Web app menu")) }
             DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
              DropdownMenuItem(text = { Text(label("편집", "Edit")) }, onClick = { menu = false; editingId = app.id; screen = "editor" })
              DropdownMenuItem(text = { Text(label("삭제", "Delete")) }, onClick = { menu = false; deleteId = app.id })
             }
            }
           }
           Spacer(Modifier.height(14.dp))
           Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
           Spacer(Modifier.height(4.dp))
           Text(runCatching { java.net.URI(app.startUrl).host }.getOrDefault(""), maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
         }
        }
       }
      }
     }
    }
    apps.find { it.id == deleteId }?.let { app ->
     AlertDialog(onDismissRequest = { deleteId = null }, title = { Text(label("웹앱을 삭제할까요?", "Delete web app?")) }, text = { Text(label("‘${app.name}’ 등록 정보만 삭제합니다. 사이트의 로그인 상태는 유지됩니다.", "Only the registration for ‘${app.name}’ will be deleted. Site sign-in data stays on this device.")) }, confirmButton = {
      TextButton(onClick = { scope.launch { runCatching { model.repository.delete(app.id) }.onFailure { failure = true }; deleteId = null } }, modifier = Modifier.testTag("confirm_delete")) { Text(label("삭제", "Delete")) }
     }, dismissButton = { TextButton(onClick = { deleteId = null }) { Text(label("취소", "Cancel")) } })
    }
    if(failure) AlertDialog(onDismissRequest = { failure = false }, title = { Text(label("저장하지 못했습니다", "Could not save")) }, text = { Text(label("다시 시도해 주세요. 등록 정보는 그대로 유지됩니다.", "Try again. Your registrations have been kept.")) }, confirmButton = { TextButton(onClick = { failure = false }) { Text(label("확인", "OK")) } })
   }
  }
 }
}

@Composable
fun AppIcon(ref: String?, model: AppViewModel, modifier: Modifier = Modifier) {
 var bitmap by remember(ref) { mutableStateOf<android.graphics.Bitmap?>(null) }
 LaunchedEffect(ref) { bitmap = withContext(Dispatchers.IO) { model.repository.iconFile(ref)?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) } } }
 Box(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
  val image = bitmap
  if(image != null) Image(image.asImageBitmap(), null, Modifier.fillMaxSize().padding(3.dp))
  else Icon(when(ref) { "preset:star" -> Icons.Outlined.StarOutline; "preset:globe" -> Icons.Outlined.Public; else -> Icons.Outlined.Cottage }, null, Modifier.fillMaxSize().padding(11.dp), tint = MaterialTheme.colorScheme.primary)
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(model: AppViewModel, original: WebAppEntity?, close: () -> Unit) {
 val id = rememberSaveable { original?.id ?: UUID.randomUUID().toString() }
 var address by rememberSaveable(original?.id) { mutableStateOf(UrlPolicy.withoutWebScheme(original?.startUrl ?: "")) }
 var useHttps by rememberSaveable(original?.id) { mutableStateOf(original?.startUrl?.startsWith("http://", ignoreCase = true) != true) }
 var name by rememberSaveable { mutableStateOf(original?.name ?: "") }
 var icon by rememberSaveable { mutableStateOf(original?.iconRef) }
 var javaScript by rememberSaveable { mutableStateOf(original?.javaScript ?: true) }
 var images by rememberSaveable { mutableStateOf(original?.images ?: true) }
 var zoom by rememberSaveable { mutableFloatStateOf((original?.textZoom ?: 100).toFloat()) }
 var busy by remember { mutableStateOf(false) }
 var message by remember { mutableStateOf("") }
 var invalid by remember { mutableStateOf(false) }
 val scope = rememberCoroutineScope()
 val successMessage = label("사이트 정보를 가져왔습니다. 이름과 아이콘을 확인해 주세요.", "Site details loaded. Review the name and icon.")
 val failedMessage = label("자동 조회하지 못했습니다. 이름을 직접 입력하고 기본 아이콘으로 저장할 수 있습니다.", "Could not fetch details. Enter a name and use a default icon.")
 val imageFailed = label("이미지를 열 수 없습니다. 2MB 이하의 일반 이미지 파일을 선택해 주세요.", "Could not open image. Choose a standard image under 2 MB.")
 val saveFailed = label("저장하지 못했습니다. 다시 시도해 주세요.", "Could not save. Please try again.")
 val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
  if(uri != null) scope.launch { busy = true; runCatching { model.repository.importImage(uri) }.onSuccess { icon = it }.onFailure { message = imageFailed }; busy = false }
 }
 val submit: () -> Unit = {
    val normalizedAddress = UrlPolicy.registrationWithScheme(address, useHttps)
    if(normalizedAddress == null || name.isBlank()) { invalid = true } else scope.launch {
     busy = true
     val draft = (original ?: WebAppEntity(id, name, normalizedAddress, displayOrder = 0, createdAt = 0, updatedAt = 0)).copy(name = name, startUrl = normalizedAddress, iconRef = icon, javaScript = javaScript, images = images, textZoom = zoom.toInt())
     runCatching { model.repository.save(draft) }.onSuccess { close() }.onFailure { message = saveFailed }
     busy = false
    }

 }
 Scaffold(topBar = { TopAppBar(title = { Text(if(original == null) label("웹앱 추가", "Add web app") else label("웹앱 편집", "Edit web app")) }, navigationIcon = { IconButton(onClick = close) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, label("뒤로", "Back")) } }, actions = { TextButton(onClick = submit, enabled = !busy, modifier = Modifier.testTag("save_app")) { Text(label("저장", "Save")) } }) }) { padding ->
  Column(Modifier.padding(padding).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   OutlinedTextField(
    value = address,
    onValueChange = { address = UrlPolicy.withoutWebScheme(it); invalid = false; message = "" },
    label = { Text(label("웹사이트 주소", "Website address")) },
    placeholder = { Text("example.com") },
    leadingIcon = {
     Row(verticalAlignment = Alignment.CenterVertically) {
      val httpsLabel = label("HTTPS 사용", "Use HTTPS")
      Checkbox(checked = useHttps, onCheckedChange = { useHttps = it; invalid = false }, enabled = !busy, modifier = Modifier.testTag("https_checkbox").semantics { contentDescription = httpsLabel })
      Text(if(useHttps) "https://" else "http://", style = MaterialTheme.typography.bodyMedium, color = if(busy) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
      Spacer(Modifier.width(4.dp))
     }
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    singleLine = true,
    isError = invalid && UrlPolicy.registrationWithScheme(address, useHttps) == null,
    enabled = !busy,
    modifier = Modifier.fillMaxWidth().testTag("url_input")
   )
   if(!useHttps) Text(label("HTTP 주소는 암호화되지 않습니다. 가능한 HTTPS를 사용하세요.", "HTTP is not encrypted. Use HTTPS when available."), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
   OutlinedButton(onClick = {
    val url = UrlPolicy.registrationWithScheme(address, useHttps)
    if(url == null) { invalid = true } else scope.launch {
     busy = true; message = ""
     runCatching { SiteMetadataLoader().load(url) }.onSuccess { details ->
      if(name.isBlank()) name = details.title
      if(icon == null) details.icon?.let { icon = model.repository.storeIcon(it) }
      address = UrlPolicy.withoutWebScheme(url); message = successMessage
     }.onFailure { message = failedMessage }
     busy = false
    }
   }, enabled = !busy, modifier = Modifier.testTag("fetch_metadata")) { Icon(Icons.Outlined.TravelExplore, null); Spacer(Modifier.width(8.dp)); Text(label("사이트 정보 가져오기", "Fetch site details")) }
   if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
   if(message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
   OutlinedTextField(value = name, onValueChange = { name = it.take(120); invalid = false }, label = { Text(label("웹앱 이름", "Web app name")) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("name_input"), isError = invalid && name.isBlank())
   if(invalid) Text(label("HTTP/HTTPS 주소와 웹앱 이름을 확인해 주세요. 사용자 정보가 포함된 주소는 저장할 수 없습니다.", "Check the HTTP/HTTPS address and name. Addresses containing user credentials are not accepted."), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("validation_error"))
   Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    AppIcon(icon, model, Modifier.size(60.dp))
    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy, modifier = Modifier.testTag("gallery_icon")) { Text(label("갤러리 이미지", "Gallery image")) }
   }
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("preset:nest" to Icons.Outlined.Cottage, "preset:globe" to Icons.Outlined.Public, "preset:star" to Icons.Outlined.StarOutline).forEachIndexed { index, (ref, shape) ->
     FilledTonalIconButton(onClick = { icon = ref }, modifier = Modifier.testTag("preset_$index")) { Icon(shape, label("기본 아이콘 ${index + 1}", "Default icon ${index + 1}")) }
    }
   }
   Text(label("로그인 프로필: 기본", "Sign-in profile: Default"), fontWeight = FontWeight.SemiBold)
   Text(label("같은 프로필의 웹앱은 동일 사이트의 로그인 상태와 사이트 데이터를 공유합니다.", "Web apps in this profile share sign-in state and data for the same site."), style = MaterialTheme.typography.bodySmall)
   HorizontalDivider()
   Text(label("이 웹앱 설정", "This web app"), style = MaterialTheme.typography.titleMedium)
   SettingSwitch(label("JavaScript 실행", "Run JavaScript"), javaScript, { javaScript = it }, "js_switch")
   SettingSwitch(label("이미지 표시", "Show images"), images, { images = it }, "images_switch")
   Text(label("글자 크기 · ${zoom.toInt()}%", "Text size · ${zoom.toInt()}%"))
   Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 50f..200f, steps = 14, modifier = Modifier.testTag("zoom_slider"))
   Spacer(Modifier.height(16.dp))
  }
 }
}

@Composable
fun SettingSwitch(title: String, checked: Boolean, change: (Boolean) -> Unit, tag: String) {
 Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
  Text(title, Modifier.weight(1f)); Switch(checked = checked, onCheckedChange = change, modifier = Modifier.testTag(tag))
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: AppViewModel, close: () -> Unit, edit: (String) -> Unit) {
 val preferences by model.appearance.collectAsStateWithLifecycle()
 val apps by model.apps.collectAsStateWithLifecycle()
 val scope = rememberCoroutineScope()
 val context = LocalContext.current
 var clear by remember { mutableStateOf(false) }
 var information by remember { mutableStateOf(false) }
 var cleared by remember { mutableStateOf(false) }
 var failure by remember { mutableStateOf(false) }
 Scaffold(topBar = { TopAppBar(title = { Text(label("설정", "Settings")) }, navigationIcon = { IconButton(onClick = close, modifier = Modifier.testTag("settings_back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, label("뒤로", "Back")) } }) }) { padding ->
  Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   Text(label("언어", "Language"), style = MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    FilterChip(selected = preferences.language == "ko", onClick = { scope.launch { runCatching { model.preferences.setLanguage("ko") }.onFailure { failure = true } } }, label = { Text("한국어") }, modifier = Modifier.testTag("language_ko"))
    FilterChip(selected = preferences.language == "en", onClick = { scope.launch { runCatching { model.preferences.setLanguage("en") }.onFailure { failure = true } } }, label = { Text("English") }, modifier = Modifier.testTag("language_en"))
   }
   Text(label("화면 테마", "Appearance"), style = MaterialTheme.typography.titleMedium)
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf("system" to label("시스템", "System"), "light" to label("밝게", "Light"), "dark" to label("어둡게", "Dark")).forEach { (value, title) ->
     FilterChip(selected = preferences.theme == value, onClick = { scope.launch { runCatching { model.preferences.setTheme(value) }.onFailure { failure = true } } }, label = { Text(title) }, modifier = Modifier.testTag("theme_$value"))
    }
   }
   HorizontalDivider()
   Text(label("웹앱별 설정", "Web app settings"), style = MaterialTheme.typography.titleMedium)
   Text(label("웹앱을 선택해 JavaScript, 이미지와 글자 크기를 조절하세요.", "Choose a web app to change JavaScript, images and text size."), style = MaterialTheme.typography.bodyMedium)
   apps.forEach { app -> OutlinedButton(onClick = { edit(app.id) }, modifier = Modifier.fillMaxWidth()) { Text(app.name) } }
   HorizontalDivider()
   OutlinedButton(onClick = { clear = true }, modifier = Modifier.testTag("clear_site_data")) { Text(label("사이트 데이터 지우기", "Clear site data")) }
   Text(label("기본 프로필 전체에 적용됩니다. 웹앱 등록 목록은 유지됩니다.", "Applies to the entire default profile. Web app registrations are kept."), style = MaterialTheme.typography.bodySmall)
   if(cleared) Text(label("사이트 데이터를 지웠습니다.", "Site data cleared."))
   BackupUpdateSection(model)
   TextButton(onClick = { information = true }) { Text(label("앱 정보", "About")) }
  }
 }
 if(clear) AlertDialog(onDismissRequest = { clear = false }, title = { Text(label("사이트 데이터를 지울까요?", "Clear site data?")) }, text = { Text(label("기본 프로필의 쿠키와 웹 저장소를 지웁니다. 사이트에서 로그아웃되며 되돌릴 수 없습니다. 등록 목록은 유지됩니다.", "Cookies and web storage for the default profile will be deleted. You will be signed out. This cannot be undone. Registrations are kept.")) }, confirmButton = {
  TextButton(onClick = {
   clear = false
   val temporary = WebView(context)
   temporary.clearCache(true); temporary.clearHistory(); temporary.clearFormData(); temporary.destroy()
   WebStorage.getInstance().deleteAllData()
   CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush(); cleared = true }
  }, modifier = Modifier.testTag("confirm_clear_data")) { Text(label("지우기", "Clear")) }
 }, dismissButton = { TextButton(onClick = { clear = false }, modifier = Modifier.testTag("cancel_clear_data")) { Text(label("취소", "Cancel")) } })
 if(information) AlertDialog(onDismissRequest = { information = false }, title = { Text(label("웹둥지", "WebDungji")) }, text = { Text(label("버전 ${BuildConfig.VERSION_NAME}\n개인용 웹앱 모음\n계정 가입·광고·결제 없음\nGoogle Drive 동기화: 연결 준비 단계\n동기화에서 쿠키·비밀번호·사이트 데이터·인증 토큰은 제외합니다.\n\nKotlin, AndroidX: Apache License 2.0; Jsoup: MIT", "Version ${BuildConfig.VERSION_NAME}\nA personal home for web apps\nNo account, ads or payments\nGoogle Drive sync: connection not implemented yet\nCookies, passwords, site data and authentication tokens are excluded from sync.\n\nKotlin, AndroidX: Apache License 2.0; Jsoup: MIT")) }, confirmButton = { TextButton(onClick = { information = false }) { Text(label("확인", "OK")) } })
 if(failure) AlertDialog(onDismissRequest = { failure = false }, title = { Text(label("설정을 저장하지 못했습니다", "Could not save settings")) }, confirmButton = { TextButton(onClick = { failure = false }) { Text(label("확인", "OK")) } })
}
