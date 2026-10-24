plugins { id("com.android.application") }

android {
    namespace = "com.blockzone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blockzone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 37
        versionName = "3.5.0"
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
