plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseStorePath = providers.environmentVariable("TAKT_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("TAKT_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("TAKT_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("TAKT_RELEASE_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { !it.isNullOrBlank() }

android {
    namespace = "com.kpyruy.takt"
    compileSdk = 35
    testBuildType = providers.gradleProperty("taktTestBuildType").getOrElse("debug")

    defaultConfig {
        applicationId = "com.kpyruy.takt"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "com.kpyruy.takt.app.TaktTestRunner"
        manifestPlaceholders["appLabel"] = "Takt"
    }

    buildTypes.create("glassPreview") {
        initWith(buildTypes.getByName("debug"))
        applicationIdSuffix = ".glass"
        versionNameSuffix = "+glass.preview"
        manifestPlaceholders["appLabel"] = "Takt Glass"
        matchingFallbacks += listOf("debug")
    }

    // Same install identity as the local preview, with production runtime behavior.
    // Release dependencies also package their baseline profiles; debuggable previews
    // stay available for instrumentation and source inspection.
    buildTypes.create("glassOptimized") {
        initWith(buildTypes.getByName("release"))
        isDebuggable = false
        isMinifyEnabled = false
        signingConfig = signingConfigs.getByName("debug")
        applicationIdSuffix = ".glass"
        versionNameSuffix = "+glass.preview"
        manifestPlaceholders["appLabel"] = "Takt Glass"
        matchingFallbacks += listOf("release")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("androidTest").assets.srcDir("$rootDir/core/database/schemas")
    sourceSets.getByName("androidTest").assets.srcDir("$rootDir/core/data/src/test/resources/teaching")
    sourceSets.getByName("androidTest").assets.srcDir("$rootDir/core/data/src/test/resources/documents")
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    if (releaseSigningReady) {
        signingConfigs.create("release") {
            storeFile = file(requireNotNull(releaseStorePath))
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:subjects"))
    implementation(project(":feature:studyplan"))
    implementation(project(":feature:settings"))

    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.biometric)
    implementation(libs.fragment.ktx)
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(libs.room.runtime)
    androidTestImplementation("androidx.documentfile:documentfile:1.0.1")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("com.squareup.okhttp3:okhttp-tls:4.12.0")
    debugImplementation(libs.compose.ui.test.manifest)
    add("glassPreviewImplementation", libs.compose.ui.test.manifest)
}
