plugins {
    id("com.android.application")
}

android {
    namespace = "com.kanreddyjp.walletsmsimporter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kanreddyjp.walletsmsimporter"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}