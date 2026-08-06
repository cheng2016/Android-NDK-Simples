#ifndef NDK_DEMO_JNI_HELPERS_H
#define NDK_DEMO_JNI_HELPERS_H

#include <jni.h>
#include <android/log.h>
#include <string>

#define LOG_TAG "NdkDemo"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace ndk {

std::string jstringToString(JNIEnv *env, jstring jstr);

jstring stringToJstring(JNIEnv *env, const std::string &str);

bool checkException(JNIEnv *env);

/** Cache jclass / jmethodID once (call from JNI_OnLoad). */
struct JniCache {
    jclass personClass = nullptr;
    jmethodID personCtor = nullptr;
    jfieldID personNameField = nullptr;
    jfieldID personAgeField = nullptr;

    jclass callbackClass = nullptr;
    jmethodID callbackOnResult = nullptr;

    JavaVM *jvm = nullptr;
};

JniCache &cache();

bool initCache(JNIEnv *env);

void releaseCache(JNIEnv *env);

}  // namespace ndk

#endif
