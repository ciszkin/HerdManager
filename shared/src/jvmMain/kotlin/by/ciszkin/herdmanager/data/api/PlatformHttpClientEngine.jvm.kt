package by.ciszkin.herdmanager.data.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual val PlatformHttpClientEngine: HttpClientEngine = CIO.create()
