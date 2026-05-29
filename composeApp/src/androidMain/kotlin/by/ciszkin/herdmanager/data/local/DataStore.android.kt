package by.ciszkin.herdmanager.data.local

import by.ciszkin.herdmanager.provideApplicationContext
import java.io.File

actual fun getDataStoreFile(): File {
    return provideApplicationContext().filesDir.resolve("datastore/HerdManager.preferences_pb")
}
