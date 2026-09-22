import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lianghao.dots"
    compileSdk = 31

    defaultConfig {
        applicationId = "com.lianghao.dots"
        minSdk = 23
        targetSdk = 31
        versionCode = 2
        versionName = "1.1"
    }

    // 发行版 APK 命名：Dots-v<versionName>.apk（debug 同样生效）
    applicationVariants.all {
        outputs.all {
            val outputImpl = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl.outputFileName = "Dots-v${versionName}.apk"
        }
    }

    buildTypes {
        // 签名信息在 local.properties（gitignored），缺失时回退默认行为
        // （release 未签名 / debug 用 ~/.android/debug.keystore）
        val props = rootProject.file("local.properties")
        var dotsSigning: com.android.build.gradle.internal.dsl.SigningConfig? = null
        if (props.exists()) {
            val lp = Properties().apply { props.inputStream().use { load(it) } }
            val storeFile = lp.getProperty("DOTS_STORE_FILE")
            if (storeFile != null) {
                dotsSigning = signingConfigs.create("dotsRelease") {
                    this.storeFile = rootProject.file(storeFile)
                    this.storePassword = lp.getProperty("DOTS_STORE_PASSWORD")
                    this.keyAlias = lp.getProperty("DOTS_KEY_ALIAS")
                    this.keyPassword = lp.getProperty("DOTS_KEY_PASSWORD")
                }
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            dotsSigning?.let { signingConfig = it }
        }
        debug {
            // debug 也用 release keystore 签名：与 release 包互相覆盖安装，免卸载
            dotsSigning?.let { signingConfig = it }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7") // takePhoto EXIF 旋转转正
    implementation("androidx.recyclerview:recyclerview:1.2.1") // 九宫格
    implementation("com.squareup.okhttp3:okhttp:4.10.0") // pickAndUploadFiles/downloadFile 网络传输

    testImplementation("junit:junit:4.13.2")
    // 本地单测里 org.json 是 android.jar 桩（抛 Stub!），引入真实实现供 JVM 测试
    testImplementation("org.json:json:20220320")
}
