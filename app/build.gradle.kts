plugins { id("com.android.application") }

android {
    namespace = "com.blockzone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blockzone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 30
        versionName = "3.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("org.mozilla.geckoview:geckoview:156.0.20260921121718")
}
