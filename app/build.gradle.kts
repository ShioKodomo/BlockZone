plugins { id("com.android.application") }

android {
    namespace = "com.blockzone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blockzone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 34
        versionName = "3.3.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:158.0.20260918163558")
}
