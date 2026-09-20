plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bombermama"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bombermama"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        // Robolectric по умолчанию подгружает нативный Conscrypt, но его .so
        // для aarch64 требует GLIBC 2.32, тогда как в окружении 2.31.
        // Для наших JVM-тестов криптопровайдер не нужен — отключаем.
        unitTests.all {
            it.systemProperty("robolectric.conscryptmode", "OFF")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Офлайн-репозиторий хранит только POM для этих транзитивных зависимостей
    // (AAR отсутствует), поэтому принудительно поднимаем до версий, которые
    // там полностью представлены.
    configurations.all {
        resolutionStrategy.force("androidx.drawerlayout:drawerlayout:1.1.1")
        resolutionStrategy.force("androidx.customview:customview:1.1.0")
    }

    testImplementation(project(":core"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
