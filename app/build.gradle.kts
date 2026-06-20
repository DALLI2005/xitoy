plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.commander.xitoy"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.commander.xitoy"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.google.firebase:firebase-messaging")
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-analytics")
    // Retrofit (Internetdan ma'lumot olish uchun)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // JSON formatini tushunish uchun (Gson)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Ikonkalar (Savatcha va boshqalar) uchun
    implementation("androidx.compose.material:material-icons-extended")
    // Lucide ikonkalari (professional ikonkalar emojilar o'rniga)
    implementation("com.composables:icons-lucide:1.0.0")
    // Ekranlar o'rtasida harakatlanish (Navigation) uchun
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // Hilt va KSP
    implementation("com.google.dagger:hilt-android:2.56.2")
    ksp("com.google.dagger:hilt-compiler:2.56.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // Buni albatta qo'shish kerak

    // Tarmoq so'rovlari va Server API bilan ishlash uchun (Retrofit)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // VPS dagi tovar rasmlarini Jetpack Compose'da ko'rsatish uchun (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Arxitektura uchun ViewModel va State boshqaruvi
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Android va Compose'ning standart kutubxonalari
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Test kutubxonalari
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}