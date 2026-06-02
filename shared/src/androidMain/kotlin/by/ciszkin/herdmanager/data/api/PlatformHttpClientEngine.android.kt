package by.ciszkin.herdmanager.data.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual val PlatformHttpClientEngine: HttpClientEngine = OkHttp.create()
