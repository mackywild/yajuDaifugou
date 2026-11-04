plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI SecretsまたはローカルGradleプロパティからAndroid用Firebase公開設定を受け取る。
fun firebaseProperty(name: String): String =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull.orEmpty().also {
        require(it.matches(Regex("[A-Za-z0-9:._-]*"))) { "Firebase設定の形式が不正です: $name" }
    }

val releaseKeystorePath = System.getenv("YAJU_KEYSTORE_PATH")
val releaseStorePassword = System.getenv("YAJU_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("YAJU_KEY_ALIAS")
val releaseKeyPassword = System.getenv("YAJU_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val appVersionCode = providers.gradleProperty("YAJU_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
    ?: 10
val appVersionName = providers.gradleProperty("YAJU_VERSION_NAME")
    .orNull
    ?: "0.5.0"

android {
    namespace = "com.example.daifugo.android"
    compileSdk = 37

    defaultConfig {
        // Google Playで公開後は変更不可。ストアテストからこのIDを正本とする。
        applicationId = "jp.YajuDaifugou.app"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "FIREBASE_ANDROID_API_KEY", "\""+firebaseProperty("FIREBASE_ANDROID_API_KEY")+"\"")
        buildConfigField("String", "FIREBASE_ANDROID_APP_ID", "\""+firebaseProperty("FIREBASE_ANDROID_APP_ID")+"\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\""+firebaseProperty("FIREBASE_PROJECT_ID")+"\"")
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\""+firebaseProperty("FIREBASE_WEB_CLIENT_ID")+"\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("releaseUpload") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("releaseUpload")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":gamecore"))
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.material3:material3")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.4.0"))
    implementation("com.squareup.okhttp3:okhttp")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")

    // Web版と共通UIDで認証する。Firestoreアクセスは既存Spring Boot経由。
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("com.google.android.gms:play-services-games-v2:22.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
}
