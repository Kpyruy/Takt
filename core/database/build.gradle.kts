plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    namespace = "com.kpyruy.takt.core.database"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(project(":core:model"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.coroutines.core)
    ksp(libs.room.compiler)
}
