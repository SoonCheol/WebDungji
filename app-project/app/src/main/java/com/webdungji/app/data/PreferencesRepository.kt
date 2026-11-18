package com.webdungji.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import java.io.IOException

private val Context.preferenceStore by preferencesDataStore("appearance")
data class AppPreferences(val language: String = "ko", val theme: String = "system")
class PreferencesRepository(context: Context) {
 private val store = context.preferenceStore
 private val language = stringPreferencesKey("language")
 private val theme = stringPreferencesKey("theme")
 val values: Flow<AppPreferences> = store.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map {
  AppPreferences(it[language] ?: "ko", it[theme] ?: "system")
 }
 suspend fun setLanguage(value: String) { require(value in setOf("ko", "en")); store.edit { it[language] = value } }
 suspend fun setTheme(value: String) { require(value in setOf("system", "light", "dark")); store.edit { it[theme] = value } }
}
