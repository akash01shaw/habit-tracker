# Add project-specific ProGuard rules here.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.loopwise.app.**$$serializer { *; }
-keepclassmembers class com.loopwise.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.loopwise.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
