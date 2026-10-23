plugins { id("com.android.application") }

android {
    namespace = "com.blockzone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blockzone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 36
        versionName = "3.4.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("io.github.mlm-games:adblock:0.1.3")
    implementation("androidx.webkit:webkit:1.16.0")
}
