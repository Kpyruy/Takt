plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    namespace = "com.kpyruy.takt.core.data"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(libs.room.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
    implementation("androidx.documentfile:documentfile:1.0.1")
    testImplementation(libs.junit)
}
