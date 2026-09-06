# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.vibo.vibolearning.**$$serializer { *; }
-keepclassmembers class com.vibo.vibolearning.** {
    *** Companion;
}
-keepclasseswithmembers class com.vibo.vibolearning.** {
    kotlinx.serialization.KSerializer serializer(...);
}
