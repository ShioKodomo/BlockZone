plugins { id("com.android.application") }

android {
    namespace = "com.blockzone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blockzone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 31
        versionName = "3.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:132.0.20241021175835")
}
