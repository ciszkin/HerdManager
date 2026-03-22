package by.ciszkin.herdmanager.data.local

import android.content.Context
import java.io.File

lateinit var applicationContext: Context

actual fun getDataStoreFile(): File {
    return applicationContext.filesDir.resolve("datastore/HerdManager.preferences_pb")
}
