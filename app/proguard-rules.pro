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
# Without these attributes, suspend endpoints and List<T> response fields can be
# reduced to raw classes in release builds, causing ParameterizedType cast errors.
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# Keep Retrofit interfaces that declare HTTP endpoints. allow* lets R8 still
# optimize names where safe, while preserving the reflective method metadata.
-keep,allowshrinking,allowoptimization,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep this app's Retrofit API and DTO fields so Gson can map JSON responses
# after R8 minification, including unannotated fields such as groupId/name.
-keep interface com.histefanhere.actualwidgets.data.api.ActualApiService { *; }
-keep class com.histefanhere.actualwidgets.data.api.** { *; }

# Keep app model classes that are serialized/deserialized through Gson for
# widget state snapshots.
-keep class com.histefanhere.actualwidgets.model.** { *; }

# Standard Retrofit/R8 compatibility noise suppressions.
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
