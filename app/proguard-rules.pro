# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class com.freevpn.androidtv.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.freevpn.androidtv.network.** {
    *** Companion;
}

# Retrofit
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp platform warnings
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (pulled in by androidx.security:security-crypto) references Google ErrorProne annotations that
# are compile-only and absent from the runtime classpath. They are pure annotations, so it is safe to
# tell R8 to ignore the missing references instead of shipping them.
-dontwarn com.google.errorprone.annotations.**

# WireGuard tunnel: the GoBackend reaches native code and config parser types via JNI/reflection.
-keep class com.wireguard.** { *; }
-dontwarn com.wireguard.**

# Never log raw VPN config — make sure model toString is not relied upon in release.
