package com.example.chengzj.ndk.simple;

/**
 * Bridge between Java and native code. Each method maps to a classic JNI topic.
 */
public final class NativeBridge {

    static {
        System.loadLibrary("ndk_demo");
    }

    private NativeBridge() {
    }

    /** Basic: return a string from C++. */
    public static native String stringFromJNI();

    /** Primitives: jint in / out. */
    public static native int add(int a, int b);

    /** Strings: jstring in / out (GetStringUTFChars / NewStringUTF). */
    public static native String greet(String name);

    /** Arrays: GetIntArrayElements + release. */
    public static native int sumArray(int[] values);

    /** Arrays: mutate jbyteArray in place (mode=0 commit). */
    public static native void reverseBytesInPlace(byte[] data);

    /** Arrays: allocate new jbyteArray from native. */
    public static native byte[] xorBytes(byte[] data, byte key);

    /** Objects: GetFieldID / GetObjectField / GetIntField. */
    public static native String describePerson(com.example.chengzj.ndk.simple.model.Person person);

    /** Objects: NewObject from cached jclass + ctor. */
    public static native com.example.chengzj.ndk.simple.model.Person createPerson(String name, int age);

    /** Callbacks: CallVoidMethod on a Java interface. */
    public static native void invokeCallback(NativeCallback callback, String payload);

    /** Exceptions: ThrowNew -> caught in Java. */
    public static native void throwIfNegative(int value);

    /** Static native method (jclass instead of jobject). */
    public static native int staticNativeMultiply(int a, int b);

    /** ABI / compile-time info from native. */
    public static native String getNativeBuildInfo();

    /** Hot loop for Java vs native timing comparison. */
    public static native long nativeChecksum(int iterations);

    /** String encoding: GetStringLength vs GetStringUTFLength. */
    public static native String inspectString(String text);

    /** Threads: pthread + AttachCurrentThread + GlobalRef. */
    public static native void startNativeThread(NativeCallback callback);

    /** NIO: GetDirectBufferAddress on a DirectByteBuffer. */
    public static native int sumDirectBuffer(java.nio.ByteBuffer buffer);

    /**
     * Dynamically registered in JNI_OnLoad via RegisterNatives
     * (no Java_com_example_... mangled symbol required).
     */
    public static native String dynamicPing();
}
