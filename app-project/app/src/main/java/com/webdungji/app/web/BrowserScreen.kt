package com.webdungji.app.web

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.doOnLayout
import android.view.ViewGroup.LayoutParams
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.webdungji.app.BuildConfig
import com.webdungji.app.data.WebAppEntity
import com.webdungji.app.domain.UrlPolicy
import com.webdungji.app.ui.label

private data class HttpAuthPrompt(val host: String, val handler: HttpAuthHandler)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(app: WebAppEntity, close: () -> Unit) {
 val context = LocalContext.current
 val owner = LocalLifecycleOwner.current
 var authPrompt by remember { mutableStateOf<HttpAuthPrompt?>(null) }
 DisposableEffect(Unit) { onDispose { authPrompt?.handler?.cancel(); authPrompt = null } }
 var webView by remember { mutableStateOf<WebView?>(null) }
 var progress by remember { mutableIntStateOf(0) }
 var canBack by remember { mutableStateOf(false) }
 var canForward by remember { mutableStateOf(false) }
 var address by rememberSaveable(app.id) { mutableStateOf(app.startUrl) }
 var showAddress by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<String?>(null) }
 var external by remember { mutableStateOf<Uri?>(null) }
 var generation by remember { mutableIntStateOf(0) }
 var rendererGone by remember { mutableStateOf(false) }
 var savedState by rememberSaveable(app.id) { mutableStateOf<Bundle?>(null) }
 val connectionError = label("페이지를 열지 못했습니다. 연결을 확인한 후 다시 시도해 주세요.", "Could not open the page. Check your connection and try again.")
 val authError = label("인증이 필요합니다. HTTP 401", "Authentication required. HTTP 401")
 val safeError = label("안전한 연결을 확인할 수 없어 페이지를 열지 않았습니다.", "The page was blocked because a secure connection could not be verified.")
 val blockedError = label("이 종류의 링크는 지원하지 않습니다.", "This link type is not supported.")
 val rendererError = label("페이지 표시가 중단되었습니다. 다시 열어 주세요.", "The page stopped responding. Open it again.")
 val externalError = label("이 링크를 열 수 있는 앱이 없습니다.", "No app can open this link.")
 BackHandler {
  if(!rendererGone && webView?.canGoBack() == true) webView?.goBack() else close()
 }
 DisposableEffect(owner, webView) {
  val view = webView
  val observer = LifecycleEventObserver { _, event ->
   when(event) {
    Lifecycle.Event.ON_RESUME -> if(!rendererGone) view?.onResume()
    Lifecycle.Event.ON_PAUSE -> if(!rendererGone) { authPrompt?.handler?.cancel(); authPrompt = null; view?.onPause(); CookieManager.getInstance().flush(); savedState = Bundle().also { view?.saveState(it) } }
    else -> Unit
   }
  }
  owner.lifecycle.addObserver(observer)
  onDispose { owner.lifecycle.removeObserver(observer) }
 }
 Scaffold(
  topBar = { TopAppBar(title = { Text(app.name, maxLines = 1) }, navigationIcon = { IconButton(onClick = close, modifier = Modifier.testTag("browser_close")) { Icon(Icons.Outlined.Apps, label("웹앱 목록", "Web apps")) } }, actions = {
   IconButton(onClick = { showAddress = true }, modifier = Modifier.testTag("current_address")) { Icon(Icons.Outlined.Info, label("현재 주소", "Current address")) }
  }) },
  bottomBar = { BottomAppBar {
   IconButton(onClick = { webView?.goBack() }, enabled = canBack && !rendererGone, modifier = Modifier.weight(1f).testTag("browser_back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, label("뒤로 가기", "Back")) }
   IconButton(onClick = { webView?.goForward() }, enabled = canForward && !rendererGone, modifier = Modifier.weight(1f).testTag("browser_forward")) { Icon(Icons.AutoMirrored.Outlined.ArrowForward, label("앞으로 가기", "Forward")) }
   IconButton(onClick = { error = null; if(rendererGone) { savedState = null; rendererGone = false; generation++ } else webView?.reload() }, modifier = Modifier.weight(1f).testTag("browser_reload")) { Icon(Icons.Outlined.Refresh, label("새로고침", "Reload")) }
   IconButton(onClick = { error = null; if(rendererGone) { savedState = null; rendererGone = false; generation++ } else webView?.loadUrl(app.startUrl) }, modifier = Modifier.weight(1f).testTag("browser_home")) { Icon(Icons.Outlined.Home, label("시작 주소", "Start address")) }
  } }
 ) { padding ->
  Column(Modifier.padding(padding).fillMaxSize()) {
   if(progress in 0..99 && error == null) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
   Box(Modifier.weight(1f)) {
    key(app.id, generation) {
     AndroidView(factory = {
      WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
      WebView(it).apply {
       // AndroidView defaults to WRAP_CONTENT even with fillMaxSize; WebView uses
       // that native flag for its CSS viewport. Keep both dimensions parent-sized.
       layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
       webView = this
       settings.apply {
        javaScriptEnabled = app.javaScript
        domStorageEnabled = true
        loadsImagesAutomatically = app.images
        blockNetworkImage = !app.images
        textZoom = app.textZoom
        allowFileAccess = false
        allowContentAccess = false
        @Suppress("DEPRECATION")
        allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        allowUniversalAccessFromFileURLs = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(false)
        setGeolocationEnabled(false)
        safeBrowsingEnabled = true
        mediaPlaybackRequiresUserGesture = true
        builtInZoomControls = true
        displayZoomControls = false
       }
       CookieManager.getInstance().setAcceptCookie(true)
       CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
       webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
         val url = request.url.toString()
         if(UrlPolicy.isWeb(url)) return false
         val scheme = request.url.scheme?.lowercase()
         if(request.isForMainFrame && request.hasGesture() && scheme in setOf("mailto", "tel", "sms", "geo")) external = request.url
         else if(request.isForMainFrame) error = blockedError
         return true
        }
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
         if(request.url.scheme?.lowercase() in setOf("file", "content", "javascript", "intent")) return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), java.io.ByteArrayInputStream(byteArrayOf()))
         return null
        }
        override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String?) {
         val current = Uri.parse(address)
         if(current.scheme != "https" || !host.equals(current.host, ignoreCase = true)) { handler.cancel(); return }
         authPrompt?.handler?.cancel()
         authPrompt = HttpAuthPrompt(host, handler)
        }
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
         if(authPrompt != null && Uri.parse(url ?: "").host != authPrompt?.host) { authPrompt?.handler?.cancel(); authPrompt = null }
         progress = 0; error = null
         if(url != null && UrlPolicy.isWeb(url)) address = url
        }
        override fun onPageFinished(view: WebView, url: String?) {
         progress = 100; canBack = view.canGoBack(); canForward = view.canGoForward()
         if(url != null && UrlPolicy.isWeb(url)) address = url
         CookieManager.getInstance().flush()
        }
        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) { canBack = view.canGoBack(); canForward = view.canGoForward() }
        override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) { if(BuildConfig.DEBUG) android.util.Log.d("WebDungjiWeb", "load code=${failure.errorCode} main=${request.isForMainFrame}"); if(request.isForMainFrame) { error = "$connectionError (${failure.errorCode})"; progress = 100 } }
        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) { if(BuildConfig.DEBUG) android.util.Log.d("WebDungjiWeb", "http status=${response.statusCode} main=${request.isForMainFrame}"); if(request.isForMainFrame && response.statusCode >= 400) { error = if(response.statusCode == 401) authError else "$connectionError (HTTP ${response.statusCode})"; progress = 100 } }
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, failure: SslError) { handler.cancel(); error = safeError; progress = 100 }
        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
         view.destroy(); rendererGone = true; error = rendererError; savedState = null; progress = 100
         return true
        }
       }
       webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(message: ConsoleMessage): Boolean {
         // Never log message text or source URLs: either may contain credentials.
         if(BuildConfig.DEBUG) android.util.Log.d("WebDungjiWeb", "console level=${message.messageLevel()} line=${message.lineNumber()}")
         return true
        }
        override fun onProgressChanged(view: WebView, value: Int) { progress = value }
        override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) { callback.invoke(origin, false, false) }
        override fun onShowFileChooser(view: WebView, callback: android.webkit.ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean { callback.onReceiveValue(null); return true }
       }
       // One-shot: resize/recomposition must not reload the document or lose its state.
       doOnLayout {
        if(webView === this && !rendererGone && width > 0 && height > 0) {
         if(BuildConfig.DEBUG) android.util.Log.d("WebDungjiViewport", "initialLoad measured=$measuredWidth,$measuredHeight actual=$width,$height lp=${layoutParams?.width},${layoutParams?.height}")
         val bundle = savedState
         if(bundle == null || restoreState(bundle) == null) loadUrl(app.startUrl)
        }
       }
      }
     }, onRelease = { view ->
      if(!rendererGone) { CookieManager.getInstance().flush(); view.stopLoading(); view.webChromeClient = null; view.destroy() }
      if(webView === view) webView = null
     }, modifier = Modifier.fillMaxSize().testTag("web_content"))
    }
    error?.let { message ->
     Surface(Modifier.fillMaxSize()) {
      Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
       Text(label("페이지를 열 수 없습니다", "Unable to open page"), style = MaterialTheme.typography.titleLarge)
       Spacer(Modifier.height(12.dp)); Text(message)
       Spacer(Modifier.height(16.dp)); Button(onClick = { error = null; if(rendererGone) { rendererGone = false; generation++; savedState = null } else webView?.loadUrl(address) }, modifier = Modifier.testTag("retry_page")) { Text(label("다시 시도", "Try again")) }
      }
     }
    }
   }
  }
 }
 authPrompt?.let { prompt ->
  key(prompt) {
   var username by remember { mutableStateOf("") }
   var password by remember { mutableStateOf("") }
   fun cancelAuth() { prompt.handler.cancel(); username = ""; password = ""; authPrompt = null }
   AlertDialog(onDismissRequest = { cancelAuth() }, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
    title = { Text(label("HTTP 인증", "HTTP authentication")) },
    text = { Column {
     Text("https://${prompt.host}")
     Text(label("이 사이트가 사용자 이름과 비밀번호를 요청합니다. 입력 정보는 저장하지 않습니다.", "This site requests a username and password. Credentials are not saved."))
     OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text(label("사용자 이름", "Username")) }, singleLine = true)
     OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(label("비밀번호", "Password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
    } },
    confirmButton = { TextButton(enabled = username.isNotBlank() && password.isNotEmpty(), onClick = {
     error = null; prompt.handler.proceed(username, password); username = ""; password = ""; authPrompt = null
    }) { Text(label("로그인", "Sign in")) } },
    dismissButton = { TextButton(onClick = { cancelAuth() }) { Text(label("취소", "Cancel")) } })
  }
 }
 if(showAddress) AlertDialog(onDismissRequest = { showAddress = false }, title = { Text(label("현재 주소", "Current address")) }, text = { Text(address) }, confirmButton = { TextButton(onClick = { if(UrlPolicy.isWeb(address)) { external = Uri.parse(address); showAddress = false } }) { Text(label("외부 브라우저로 열기", "Open in browser")) } }, dismissButton = { TextButton(onClick = { showAddress = false }) { Text(label("닫기", "Close")) } })
 external?.let { target ->
  AlertDialog(onDismissRequest = { external = null }, title = { Text(label("다른 앱에서 열까요?", "Open another app?")) }, text = { Text(target.toString()) }, confirmButton = { TextButton(onClick = {
   if(target.scheme?.lowercase() in setOf("http", "https", "mailto", "tel", "sms", "geo")) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, target).addCategory(Intent.CATEGORY_BROWSABLE)) }.onFailure { error = externalError }
   external = null
  }) { Text(label("열기", "Open")) } }, dismissButton = { TextButton(onClick = { external = null }) { Text(label("취소", "Cancel")) } })
 }
}
