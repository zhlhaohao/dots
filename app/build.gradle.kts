plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lianghao.myapp"
    compileSdk = 31

    defaultConfig {
        applicationId = "com.lianghao.myapp"
        minSdk = 23
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    testImplementation("junit:junit:4.13.2")
    // 本地单测里 org.json 是 android.jar 桩（抛 Stub!），引入真实实现供 JVM 测试
    testImplementation("org.json:json:20220320")
}
