# Add project specific ProGuard rules here.

# Stack trace uchun line number ma'lumotlarini saqlash
-keepattributes SourceFile,LineNumberTable

# Hilt
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.android.AndroidEntryPoint <init>(...);
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# Gson
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Loyiha modellari (@SerializedName annotatsiyalari bilan)
-keep class com.commander.xitoy.domain.model.** { *; }
-keep class com.commander.xitoy.data.remote.** { *; }

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