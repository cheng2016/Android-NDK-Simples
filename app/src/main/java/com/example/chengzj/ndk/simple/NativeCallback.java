package com.example.chengzj.ndk.simple;

/**
 * Called from native code (same thread or after AttachCurrentThread).
 */
public interface NativeCallback {
    void onResult(String message);
}
