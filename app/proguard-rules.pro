# Keep JNI entry points and fields accessed from native.
-keep class com.example.chengzj.ndk.simple.NativeBridge { *; }
-keep class com.example.chengzj.ndk.simple.NativeCallback { *; }
-keep class com.example.chengzj.ndk.simple.model.Person { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
