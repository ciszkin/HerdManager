package by.ciszkin.herdmanager.di

import android.content.Context
import android.content.ComponentName

lateinit var localApplicationContext: Context

var mainActivityComponentName: ComponentName? = null

fun provideApplicationContext(): Context =
    if (::localApplicationContext.isInitialized) localApplicationContext
    else throw IllegalStateException("Application context not initialized. Make sure MainActivity is created first.")
