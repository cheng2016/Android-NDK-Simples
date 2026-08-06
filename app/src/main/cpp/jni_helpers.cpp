#include "jni_helpers.h"

#include <cstring>

namespace ndk {

static JniCache g_cache;

JniCache &cache() {
    return g_cache;
}

std::string jstringToString(JNIEnv *env, jstring jstr) {
    if (jstr == nullptr) {
        return {};
    }
    const char *chars = env->GetStringUTFChars(jstr, nullptr);
    if (chars == nullptr) {
        return {};
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

jstring stringToJstring(JNIEnv *env, const std::string &str) {
    return env->NewStringUTF(str.c_str());
}

bool checkException(JNIEnv *env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

bool initCache(JNIEnv *env) {
    jclass personLocal = env->FindClass("com/example/chengzj/ndk/simple/model/Person");
    if (personLocal == nullptr) {
        LOGE("FindClass Person failed");
        return false;
    }
    g_cache.personClass = reinterpret_cast<jclass>(env->NewGlobalRef(personLocal));
    env->DeleteLocalRef(personLocal);
    g_cache.personCtor = env->GetMethodID(g_cache.personClass, "<init>", "(Ljava/lang/String;I)V");
    g_cache.personNameField = env->GetFieldID(g_cache.personClass, "name", "Ljava/lang/String;");
    g_cache.personAgeField = env->GetFieldID(g_cache.personClass, "age", "I");

    jclass cbLocal = env->FindClass("com/example/chengzj/ndk/simple/NativeCallback");
    if (cbLocal == nullptr) {
        LOGE("FindClass NativeCallback failed");
        return false;
    }
    g_cache.callbackClass = reinterpret_cast<jclass>(env->NewGlobalRef(cbLocal));
    env->DeleteLocalRef(cbLocal);
    g_cache.callbackOnResult = env->GetMethodID(
            g_cache.callbackClass, "onResult", "(Ljava/lang/String;)V");

    if (g_cache.personCtor == nullptr || g_cache.personNameField == nullptr ||
        g_cache.personAgeField == nullptr || g_cache.callbackOnResult == nullptr) {
        LOGE("Failed to resolve Person / NativeCallback members");
        return false;
    }
    LOGI("JNI cache initialized");
    return true;
}

void releaseCache(JNIEnv *env) {
    if (g_cache.personClass != nullptr) {
        env->DeleteGlobalRef(g_cache.personClass);
        g_cache.personClass = nullptr;
    }
    if (g_cache.callbackClass != nullptr) {
        env->DeleteGlobalRef(g_cache.callbackClass);
        g_cache.callbackClass = nullptr;
    }
    g_cache.personCtor = nullptr;
    g_cache.personNameField = nullptr;
    g_cache.personAgeField = nullptr;
    g_cache.callbackOnResult = nullptr;
}

}  // namespace ndk
