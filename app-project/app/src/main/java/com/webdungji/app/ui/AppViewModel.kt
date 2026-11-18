package com.webdungji.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.webdungji.app.WebDungjiApplication
import com.webdungji.app.data.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AppViewModel(application: Application) : AndroidViewModel(application) {
 private val app = application as WebDungjiApplication
 val repository = app.repository
 val preferences = app.preferences
 val apps = repository.apps.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
 val appearance = preferences.values.stateIn(viewModelScope, SharingStarted.Eagerly, AppPreferences())
}
