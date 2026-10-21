plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.balance.monthly"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.balance.monthly"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // 샘플 데이터 화면은 디버그와 측정용 빌드(Baseline Profile 생성, Macrobenchmark)에만 넣는다
    sourceSets {
        listOf("debug", "nonMinifiedRelease", "benchmarkRelease").forEach { name ->
            maybeCreate(name).apply {
                kotlin.directories += "src/sample/java"
                manifest.srcFile("src/sample/AndroidManifest.xml")
            }
        }
    }
}

ksp {
    arg("room.generateKotlin", "true")
}

room {
    schemaDirectory("$projectDir/schemas")
}

baselineProfile {
    // 시작 경로 클래스를 첫 dex에 모으는 Startup Profile도 함께 쓴다
    dexLayoutOptimization = true
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
