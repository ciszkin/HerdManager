import com.android.build.api.variant.AndroidComponentsExtension

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "by.ciszkin.herdmanager"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "by.ciszkin.herdmanager"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.jsoup)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    debugImplementation(libs.compose.uiTooling)
}

// Workaround: with the new KMP Android plugin (AGP 9.0.1–9.3.1), the shared
// module's compose resources reach the app's asset merge for the release build
// but not for the debug build (the plugin never wires the debug asset copy).
// Copy the shared module's prepared commonMain compose resources into the app's
// generated assets under their runtime package path so the debug APK ships them
// (the release APK already contains them). The package path matches the shared
// module's resources package: <module>.generated.resources.
val sharedComposeResourcesCopy = tasks.register<Copy>("copySharedComposeResourcesForDebug") {
    dependsOn(project(":shared").tasks.named("prepareComposeResourcesTaskForCommonMain"))
    from(project(":shared").layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
    into(layout.buildDirectory.dir("generated/composeAssetsDebug/composeResources/herdmanager.shared.generated.resources"))
}

androidComponents {
    onVariants(selector().withBuildType("debug")) { variant ->
        val appComposeAssets = layout.buildDirectory.dir("generated/composeAssetsDebug").get().asFile
        variant.sources.assets?.addStaticSourceDirectory(appComposeAssets.absolutePath)
    }
}
afterEvaluate {
    tasks.named("mergeDebugAssets") {
        dependsOn(sharedComposeResourcesCopy)
    }
}