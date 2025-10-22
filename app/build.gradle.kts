plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.botonpanicovertice"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.botonpanicovertice"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "PANIC_ALERT_URL",
            "\"https://prototipo-vertice-production.up.railway.app/api/v1/alertas/panico/\""
        )
        buildConfigField(
            "String",
            "PANIC_ALERT_TOKEN",
            "\"4ZUEJQnaSzT54Q0Dq61Pk5iCiFkC6Mj_hw0-rf7lN4X8kvABhYEIc97SIOVmYG_E\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.volley)
    implementation(libs.play.services.location)
}