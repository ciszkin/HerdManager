package by.ciszkin.herdmanager.data.local

import java.io.File

actual fun getDataStoreFile(): File {
    val dataStorePath = "${System.getProperty("user.home")}/.herdmanager"
    val dataStoreFile = File(dataStorePath, "preferences.preferences_pb")
    dataStoreFile.parentFile?.mkdirs()
    return dataStoreFile
}
