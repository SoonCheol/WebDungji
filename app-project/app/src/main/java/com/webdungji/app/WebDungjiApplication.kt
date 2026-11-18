package com.webdungji.app

import android.app.Application
import androidx.room.Room
import com.webdungji.app.data.AppDatabase
import com.webdungji.app.data.AppRepository
import com.webdungji.app.data.PreferencesRepository

class WebDungjiApplication : Application() {
 val database by lazy { Room.databaseBuilder(this, AppDatabase::class.java, "webdungji.db").build() }
 val repository by lazy { AppRepository(database, this) }
 val preferences by lazy { PreferencesRepository(this) }
}
