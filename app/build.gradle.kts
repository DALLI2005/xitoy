import java.io.FileInputStream
import java.util.Properties

// ── Release imzolash kaliti ────────────────────────────────────────────────────
// Kalit ma'lumotlari (store path, parollar, alias) KODGA YOZILMAYDI.
// Ular local.properties dan o'qiladi (bu fayl .gitignore'da), yoki CI uchun
// bir xil nomdagi muhit o'zgaruvchilaridan (environment variables).
//
// local.properties ga qo'shiladigan satrlar (namuna):
//   XITOY_STORE_FILE=/Users/a1/keystores/xitoy-upload.jks
//   XITOY_STORE_PASSWORD=...
//   XITOY_KEY_ALIAS=xitoy-upload
//   XITOY_KEY_PASSWORD=...
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}

val missingSigningKeys = mutableListOf<String>()

fun signingValue(key: String): String? {
    val v = (localProps.getProperty(key) ?: System.getenv(key))?.trim()
    if (v.isNullOrEmpty()) {
        missingSigningKeys += key
        return null
    }
    return v
}

val storeFilePath = signingValue("XITOY_STORE_FILE")
val storePasswordValue = signingValue("XITOY_STORE_PASSWORD")
val keyAliasValue = signingValue("XITOY_KEY_ALIAS")
val keyPasswordValue = signingValue("XITOY_KEY_PASSWORD")

// Nisbiy yo'l berilsa — loyiha ildiziga nisbatan hisoblanadi.
val resolvedStoreFile = storeFilePath?.let {
    val f = File(it)
    if (f.isAbsolute) f else rootProject.file(it)
}

val hasReleaseSigning = missingSigningKeys.isEmpty() && resolvedStoreFile?.exists() == true

// Release artefakt yig'ilayotgan bo'lsa-yu kalit topilmasa — tushunarli xato bilan
// to'xtaymiz (debug kalit bilan imzolangan .aab ni Google Play qabul qilmaydi).
gradle.taskGraph.whenReady {
    // Faqat haqiqiy imzolash task'lari — AGP'ning ichki `packageReleaseResources`
    // kabi task'lari bilan adashtirmaslik uchun nomlar aniq ko'rsatilgan.
    val buildingRelease = allTasks.any { t ->
        t.name == "packageRelease" || t.name == "packageReleaseBundle" ||
            t.name.startsWith("signRelease")
    }
    if (buildingRelease && !hasReleaseSigning) {
        val reason = when {
            missingSigningKeys.isNotEmpty() ->
                "local.properties (yoki environment) da yo'q: ${missingSigningKeys.joinToString(", ")}"
            else -> "keystore fayli topilmadi: $resolvedStoreFile"
        }
        throw GradleException(
            """
            |Release imzolash kaliti sozlanmagan — $reason
            |
            |Tuzatish: upload keystore yarating va local.properties ga quyidagilarni qo'shing:
            |  XITOY_STORE_FILE=/absolute/path/to/xitoy-upload.jks
            |  XITOY_STORE_PASSWORD=<store paroli>
            |  XITOY_KEY_ALIAS=xitoy-upload
            |  XITOY_KEY_PASSWORD=<key paroli>
            |
            |local.properties .gitignore'da — parollar repositoriyga tushmaydi.
            """.trimMargin()
        )
    }
}

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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.commander.xitoy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Kalit sozlanmagan bo'lsa config umuman yaratilmaydi — shunda debug
        // build'lar (va `gradlew tasks` kabi buyruqlar) kalitsiz ham ishlayveradi.
        if (hasReleaseSigning) {
            create("release") {
                storeFile = resolvedStoreFile
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
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
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            isMinifyEnabled = false
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

    // (Retrofit yuqorida allaqachon qo'shilgan)
    // Network logging (Logcat'da so'rovlarni ko'rish uchun)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // VPS dagi tovar rasmlarini Jetpack Compose'da ko'rsatish uchun (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Arxitektura uchun ViewModel va State boshqaruvi
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Lottie animatsiyalari (onboarding uchun)
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    // GPS lokatsiya (Rishton hududi tekshiruvi uchun)
    implementation("com.google.android.gms:play-services-location:21.3.0")

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