package com.webdungji.app

import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webdungji.app.ui.WebDungjiRoot

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  setContent { WebDungjiRoot(viewModel()) }
 }
 override fun onStop() {
  super.onStop()
  CookieManager.getInstance().flush()
 }
}
