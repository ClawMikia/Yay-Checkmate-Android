# Add project specific ProGuard rules here.
-keep class com.yaycheckmate.data.entity.** { *; }
-keep class com.yaycheckmate.data.dao.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.mlkit.**
