# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Retrofit and Gson inspect generic response types and annotations at runtime.
# Without Signature, List<BudgetFile> can be reduced to a raw Class in release
# builds, causing "Class cannot be cast to ParameterizedType" while parsing.
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep Retrofit service method signatures and endpoint annotations.
-keep interface com.histefanhere.actualwidgets.data.api.ActualApiService { *; }

# Keep API DTO fields so Gson can map JSON responses after R8 minification.
-keep class com.histefanhere.actualwidgets.data.api.** { *; }
