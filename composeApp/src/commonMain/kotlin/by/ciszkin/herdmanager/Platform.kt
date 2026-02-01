package by.ciszkin.herdmanager

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform