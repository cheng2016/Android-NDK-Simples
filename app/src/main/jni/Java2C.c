//
// Created by chengzj on 2017/5/30.
//

#include "com_example_chengzj_ndk_simple_Java2CJNI.h"

JNIEXPORT jstring JNICALL
 Java_com_example_chengzj_ndk_simple_Java2CJNI_java2C
        (JNIEnv *env, jobject jobj){
    return (*env)->NewStringUTF(env,"i am from native C.");
}