# قواعد ProGuard لمشروع REMI8
-keep class com.remi8.** { *; }
-keep class com.remi8.remiscript.** { *; }
-keep class com.remi8.engine.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-dontwarn com.remi8.**
