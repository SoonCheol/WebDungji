package com.webdungji.app.ui

import androidx.compose.runtime.staticCompositionLocalOf

val LocalEnglish = staticCompositionLocalOf { false }
@androidx.compose.runtime.Composable
fun label(ko: String, en: String) = if(LocalEnglish.current) en else ko
