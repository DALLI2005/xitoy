# Add project specific ProGuard rules here.

# === KRITIK: Gson + Retrofit generic types ===
# Bu qoidalarsiz release build'da
# "Class cannot be cast to ParameterizedType" xatosi chiqadi

# Generic signature'larni saqlash (eng muhimi)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Retrofit — interfeyslarning generic signature'larini saqlash
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Retrofit servicelar
-keep interface com.commander.xitoy.data.remote.** { *; }
-keep class com.commander.xitoy.data.remote.** { *; }

# TypeToken (Gson) - generic kalit
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep public class * extends com.google.gson.reflect.TypeToken

# Barcha data class va modellarni saqlash
-keep class com.commander.xitoy.domain.model.** { *; }
-keepclassmembers class com.commander.xitoy.domain.model.** {
    <fields>;
    <init>(...);
}

# Kotlin metadata
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$$serializer { *; }

# ===================================================

# Stack trace uchun line number ma'lumotlarini saqlash
-keepattributes SourceFile,LineNumberTable

# Hilt
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.AndroidEntryPoint <init>(...);
}

# Retrofit — qo'shimcha qoidalar
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Firebase FCM
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Har ehtimolga qarshi — barcha loyiha sinflari
-keep class com.commander.xitoy.** { *; }
