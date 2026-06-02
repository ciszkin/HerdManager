import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
            implementation(libs.jsoup)
        }
    }
}

compose.desktop {
    application {
        mainClass = "by.ciszkin.herdmanager.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "herd-manager"
            packageVersion = "1.0.0"
            description = "AI Model Manager for Ollama"
            copyright = "© 2026 Herd Manager"
            vendor = "Herd Manager"

            macOS {
                bundleID = "by.ciszkin.herdmanager"
            }

            windows {
                menuGroup = "AI Tools"
            }

            linux {
                shortcut = true
            }
        }
    }
}
