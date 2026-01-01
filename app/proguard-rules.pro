# Memo Widget ProGuard Rules

# Keep data models for serialization
-keep class com.memo.widget.data.** { *; }

# Keep widget provider
-keep class com.memo.widget.ui.MemoWidgetProvider { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generic signatures
-keepattributes Signature
