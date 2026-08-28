import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.whyy.snapnotes"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.whyy.snapnotes"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 统一签名（三级回退，保证任何机器构建的 APK 签名一致）：
    // 1) 根目录 keystore.properties + 其指向的 jks（最高优先，正式渠道）
    // 2) 本模块入库的 snapnotes.p12 项目共享签名（默认，覆盖安装不冲突的关键）
    // 3) 以上都不存在才退回 Android 默认 debug 签名（各机器不同，仅应急）
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    var keyAliasProp = "snapnotes"
    var keyPasswordProp = "snapnotes-shared-2026"
    var storeFileProp = file("snapnotes.p12")
    var storePasswordProp = "snapnotes-shared-2026"
    if (keystorePropertiesFile.exists()) {
        val keystoreProperties = Properties().apply {
            FileInputStream(keystorePropertiesFile).use { load(it) }
        }
        val storeFilePath = keystoreProperties.getProperty("storeFile")
        if (storeFilePath != null && rootProject.file(storeFilePath).exists()) {
            keyAliasProp = keystoreProperties.getProperty("keyAlias")
            keyPasswordProp = keystoreProperties.getProperty("keyPassword")
            storeFileProp = rootProject.file(storeFilePath)
            storePasswordProp = keystoreProperties.getProperty("storePassword")
        }
    }

    signingConfigs {
        create("unified") {
            keyAlias = keyAliasProp
            keyPassword = keyPasswordProp
            storeFile = storeFileProp
            storePassword = storePasswordProp
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("unified")
        }
        debug {
            signingConfig = signingConfigs.getByName("unified")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/DEPENDENCIES",
                "META-INF/*.kotlin_module",
                "META-INF/androidx.cardview_cardview.version",
                "META-INF/androidx.versionedparcelable.version"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    // 小米穿戴第三方 SDK v1.4 (本地 aar, BLE 通信底层)
    implementation(files("./libs/xms-wearable-lib_1.4_release.aar"))
    implementation(libs.androidx.compose.ui)

    implementation(project(":glasense-ui"))
    implementation(libs.backdrop)
    implementation(libs.shapes)
    implementation(libs.material.color.utilities)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation.graphics)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.work.runtime.ktx)

    implementation("androidx.navigation3:navigation3-runtime:1.0.1")

    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")

    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")

    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.3")

    implementation("top.yukonga.miuix.kmp:miuix-squircle-android:0.9.3")

    implementation("top.yukonga.miuix.kmp:miuix-navigation3-ui-android:0.9.3")

    implementation("com.squareup.okhttp3:okhttp:5.4.0")
implementation(libs.markdown.renderer)
implementation(libs.markdown.renderer.code)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
