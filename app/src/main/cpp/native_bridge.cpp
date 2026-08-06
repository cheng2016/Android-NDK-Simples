#include "jni_helpers.h"

#include <pthread.h>
#include <unistd.h>
#include <cmath>
#include <sstream>
#include <vector>

using ndk::cache;
using ndk::checkException;
using ndk::jstringToString;
using ndk::stringToJstring;

namespace {

struct ThreadArgs {
    JavaVM *jvm;
    jobject callbackGlobal;
};

void *nativeWorker(void *arg) {
    auto *args = static_cast<ThreadArgs *>(arg);
    JNIEnv *env = nullptr;
    jint status = args->jvm->AttachCurrentThread(&env, nullptr);
    if (status != JNI_OK || env == nullptr) {
        LOGE("AttachCurrentThread failed: %d", status);
        args->jvm->DeleteGlobalRef(args->callbackGlobal);
        delete args;
        return nullptr;
    }

    usleep(300 * 1000);  // simulate work

    std::ostringstream oss;
    oss << "Hello from pthread tid=" << pthread_self()
        << " (AttachCurrentThread -> CallVoidMethod)";
    jstring msg = stringToJstring(env, oss.str());
    env->CallVoidMethod(args->callbackGlobal, cache().callbackOnResult, msg);
    checkException(env);
    if (msg != nullptr) {
        env->DeleteLocalRef(msg);
    }

    env->DeleteGlobalRef(args->callbackGlobal);
    args->jvm->DetachCurrentThread();
    delete args;
    return nullptr;
}

/** Dynamically registered static method: (JNIEnv*, jclass). */
jstring dynamicPing(JNIEnv *env, jclass /*clazz*/) {
    return stringToJstring(
            env,
            "Registered via RegisterNatives in JNI_OnLoad - no Java_xxx mangled name needed.");
}

const JNINativeMethod kDynamicMethods[] = {
        {"dynamicPing", "()Ljava/lang/String;", reinterpret_cast<void *>(dynamicPing)},
};

}  // namespace

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    cache().jvm = vm;
    if (!ndk::initCache(env)) {
        return JNI_ERR;
    }

    jclass bridge = env->FindClass("com/example/chengzj/ndk/simple/NativeBridge");
    if (bridge == nullptr) {
        LOGE("FindClass NativeBridge failed");
        return JNI_ERR;
    }
    if (env->RegisterNatives(bridge, kDynamicMethods,
                             sizeof(kDynamicMethods) / sizeof(kDynamicMethods[0])) != 0) {
        LOGE("RegisterNatives failed");
        return JNI_ERR;
    }
    env->DeleteLocalRef(bridge);
    LOGI("JNI_OnLoad OK");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void * /*reserved*/) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_OK) {
        ndk::releaseCache(env);
    }
}

// 1) Hello string
JNIEXPORT jstring JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_stringFromJNI(JNIEnv *env, jclass /*clazz*/) {
    return stringToJstring(env, "Hello from native C++ (NDK)!");
}

// 2) Primitives
JNIEXPORT jint JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_add(JNIEnv * /*env*/, jclass /*clazz*/,
                                                     jint a, jint b) {
    return a + b;
}

// 3) String in / out
JNIEXPORT jstring JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_greet(JNIEnv *env, jclass /*clazz*/,
                                                       jstring name) {
    std::string n = jstringToString(env, name);
    if (n.empty()) {
        n = "anonymous";
    }
    return stringToJstring(env, "Hello, " + n + "! - string built in native.");
}

// 4) Int array sum
JNIEXPORT jint JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_sumArray(JNIEnv *env, jclass /*clazz*/,
                                                          jintArray arr) {
    if (arr == nullptr) {
        return 0;
    }
    jsize len = env->GetArrayLength(arr);
    jint *elems = env->GetIntArrayElements(arr, nullptr);
    if (elems == nullptr) {
        return 0;
    }
    jint sum = 0;
    for (jsize i = 0; i < len; ++i) {
        sum += elems[i];
    }
    env->ReleaseIntArrayElements(arr, elems, JNI_ABORT);
    return sum;
}

// 5) Reverse bytes in place
JNIEXPORT void JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_reverseBytesInPlace(JNIEnv *env, jclass /*clazz*/,
                                                                     jbyteArray data) {
    if (data == nullptr) {
        return;
    }
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return;
    }
    for (jsize i = 0; i < len / 2; ++i) {
        jbyte tmp = bytes[i];
        bytes[i] = bytes[len - 1 - i];
        bytes[len - 1 - i] = tmp;
    }
    env->ReleaseByteArrayElements(data, bytes, 0);  // commit changes
}

// 6) XOR "encrypt"
JNIEXPORT jbyteArray JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_xorBytes(JNIEnv *env, jclass /*clazz*/,
                                                          jbyteArray data, jbyte key) {
    if (data == nullptr) {
        return nullptr;
    }
    jsize len = env->GetArrayLength(data);
    jbyte *src = env->GetByteArrayElements(data, nullptr);
    if (src == nullptr) {
        return nullptr;
    }
    jbyteArray out = env->NewByteArray(len);
    std::vector<jbyte> buf(static_cast<size_t>(len));
    for (jsize i = 0; i < len; ++i) {
        buf[static_cast<size_t>(i)] = static_cast<jbyte>(src[i] ^ key);
    }
    env->SetByteArrayRegion(out, 0, len, buf.data());
    env->ReleaseByteArrayElements(data, src, JNI_ABORT);
    return out;
}

// 7) Read Java object fields
JNIEXPORT jstring JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_describePerson(JNIEnv *env, jclass /*clazz*/,
                                                                jobject person) {
    if (person == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "person is null");
        return nullptr;
    }
    auto nameObj = reinterpret_cast<jstring>(env->GetObjectField(person, cache().personNameField));
    jint age = env->GetIntField(person, cache().personAgeField);
    std::string name = jstringToString(env, nameObj);
    if (nameObj != nullptr) {
        env->DeleteLocalRef(nameObj);
    }
    std::ostringstream oss;
    oss << "Person{name=\"" << name << "\", age=" << age << "} read via GetFieldID";
    return stringToJstring(env, oss.str());
}

// 8) Create Java object from native
JNIEXPORT jobject JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_createPerson(JNIEnv *env, jclass /*clazz*/,
                                                              jstring name, jint age) {
    return env->NewObject(cache().personClass, cache().personCtor, name, age);
}

// 9) Call Java callback from native (same thread)
JNIEXPORT void JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_invokeCallback(JNIEnv *env, jclass /*clazz*/,
                                                                jobject callback,
                                                                jstring payload) {
    if (callback == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "callback is null");
        return;
    }
    std::string p = jstringToString(env, payload);
    jstring msg = stringToJstring(env, "Native -> Java callback: " + p);
    env->CallVoidMethod(callback, cache().callbackOnResult, msg);
    checkException(env);
    if (msg != nullptr) {
        env->DeleteLocalRef(msg);
    }
}

// 10) Throw exception from native
JNIEXPORT void JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_throwIfNegative(JNIEnv *env, jclass /*clazz*/,
                                                                 jint value) {
    if (value < 0) {
        std::ostringstream oss;
        oss << "Native rejected value=" << value << " (must be >= 0)";
        env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), oss.str().c_str());
    }
}

// 11) Static native
JNIEXPORT jint JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_staticNativeMultiply(JNIEnv * /*env*/,
                                                                      jclass /*clazz*/,
                                                                      jint a, jint b) {
    return a * b;
}

// 12) Build / ABI info
JNIEXPORT jstring JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_getNativeBuildInfo(JNIEnv *env,
                                                                    jclass /*clazz*/) {
    std::ostringstream oss;
    oss << "ABI pointer=" << (sizeof(void *) * 8) << "-bit"
        << ", __ANDROID_API__=" << __ANDROID_API__
#if defined(__aarch64__)
        << ", arch=aarch64"
#elif defined(__arm__)
        << ", arch=arm"
#elif defined(__i386__)
        << ", arch=x86"
#elif defined(__x86_64__)
        << ", arch=x86_64"
#else
        << ", arch=unknown"
#endif
        << ", sizeof(jlong)=" << sizeof(jlong)
        << ", sizeof(jint)=" << sizeof(jint);
    return stringToJstring(env, oss.str());
}

// 13) Native compute-heavy loop (for benchmark)
JNIEXPORT jlong JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_nativeChecksum(JNIEnv * /*env*/, jclass /*clazz*/,
                                                                jint iterations) {
    uint64_t acc = 0;
    for (jint i = 0; i < iterations; ++i) {
        acc = (acc * 1315423911u) ^ static_cast<uint64_t>(i);
        acc += static_cast<uint64_t>(std::sin(static_cast<double>(i & 0xFF)) * 1000.0);
    }
    return static_cast<jlong>(acc);
}

// 14) UTF length via GetStringUTFChars / GetStringLength
JNIEXPORT jstring JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_inspectString(JNIEnv *env, jclass /*clazz*/,
                                                               jstring text) {
    if (text == nullptr) {
        return stringToJstring(env, "null");
    }
    jsize utf16Len = env->GetStringLength(text);
    jsize utf8Len = env->GetStringUTFLength(text);
    const char *utf = env->GetStringUTFChars(text, nullptr);
    std::ostringstream oss;
    oss << "GetStringLength(UTF-16 code units)=" << utf16Len
        << ", GetStringUTFLength(Modified UTF-8 bytes)=" << utf8Len
        << ", preview=\"" << (utf ? utf : "") << "\"";
    if (utf != nullptr) {
        env->ReleaseStringUTFChars(text, utf);
    }
    return stringToJstring(env, oss.str());
}

// 15) Start native pthread that callbacks to Java
JNIEXPORT void JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_startNativeThread(JNIEnv *env, jclass /*clazz*/,
                                                                   jobject callback) {
    if (callback == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/NullPointerException"), "callback is null");
        return;
    }
    if (cache().jvm == nullptr) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "JavaVM not cached");
        return;
    }

    auto *args = new ThreadArgs();
    args->jvm = cache().jvm;
    args->callbackGlobal = env->NewGlobalRef(callback);

    pthread_t thread;
    int rc = pthread_create(&thread, nullptr, nativeWorker, args);
    if (rc != 0) {
        env->DeleteGlobalRef(args->callbackGlobal);
        delete args;
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), "pthread_create failed");
        return;
    }
    pthread_detach(thread);
}

// 16) Direct ByteBuffer
JNIEXPORT jint JNICALL
Java_com_example_chengzj_ndk_simple_NativeBridge_sumDirectBuffer(JNIEnv *env, jclass /*clazz*/,
                                                                 jobject buffer) {
    if (buffer == nullptr) {
        return 0;
    }
    void *addr = env->GetDirectBufferAddress(buffer);
    jlong cap = env->GetDirectBufferCapacity(buffer);
    if (addr == nullptr || cap <= 0) {
        return -1;  // not a direct buffer
    }
    auto *bytes = static_cast<unsigned char *>(addr);
    jint sum = 0;
    for (jlong i = 0; i < cap; ++i) {
        sum += bytes[i];
    }
    return sum;
}

}  // extern "C"
