# Memo Phase 0 - ProGuard Rules

# Keep widget provider
-keep class com.memo.widget.MemoWidgetProvider { *; }

# Keep overlay activity
-keep class com.memo.widget.EditorOverlayActivity { *; }

# Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.** { *; }
