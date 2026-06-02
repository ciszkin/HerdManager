package by.ciszkin.herdmanager.di

import android.content.Context

lateinit var localApplicationContext: Context

fun provideApplicationContext(): Context =
    if (::localApplicationContext.isInitialized) localApplicationContext
    else throw IllegalStateException("Application context not initialized. Make sure MainActivity is created first.")
