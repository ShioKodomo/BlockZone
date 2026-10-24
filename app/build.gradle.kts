plugins { id("com.android.application") }

android {
    namespace = "com.blockzone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gothtime.optimizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(files("libs/adblock-0.1.3.jar"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.10")
    implementation("androidx.webkit:webkit:1.16.0")
}
