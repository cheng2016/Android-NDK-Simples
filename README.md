# Android-NDK-Simples
Android ndk的一个简单的例子



### 使用步骤

1. #### 搭建ndk环境



2. #### 创建jni调用类，定义本地方法

   ```
   public class Java2CJNI {
       
       public native String java2C();
       
   }
   ```



3. #### 通过javah命令获取jni调用类头文件

   > 在项目根目录下，进入main->java目录，全选文件目录栏，直接输入cmd命令并按回车键进入docs命令

   `javah -classpath . -jni -d jni com.example.chengzj.ndk.simple.Java2CJNI`

4. #### 创建实现头文件的.c源文件，并实现头文件的本地方法

   ```
   #include "com_example_chengzj_ndk_simple_Java2CJNI.h"

   JNIEXPORT jstring JNICALL
    Java_com_example_chengzj_ndk_simple_Java2CJNI_java2C
           (JNIEnv *env, jobject jobj){
       return (*env)->NewStringUTF(env,"i am from native C.");
   }
   ```

5. #### 在jni目录下创建Android.mk文件，写入以下内容

   > ​	LOCAL_PATH := $(call my-dir)                                      //固定写法，把路径赋给LOCAL_PATH变量
   >
   > ​	include $(CLEAR_VARS)                                                //清除其他LOCAL变量
   >
   > ​	LOCAL_MODULE    := Java2C                                      //这个模块的名字，最后生成的.so的名字就是它
   >
   > ​	LOCAL_SRC_FILES := Java2C.so                                  //这里是要编译的文件
   >
   > ​	include $(PREBUILT_SHARED_LIBRARY)                   //SHARED_LIBRARY就是动态库，即.so文件

6. #### 配置并生成so文件

   ```
   ndk {
       moduleName "Java2C" //so文件名
       abiFilters "armeabi", "armeabi-v7a", "x86" //CPU类型
   }
   ```

7. #### 加载so库

   ```
   static {
       System.loadLibrary("Java2C");
   }
   ```

8. #### 执行调用本地方法

   ```
   String result = new Java2CJNI().java2C();
   Toast.makeText(MainActivity.this,result,Toast.LENGTH_LONG).show();
   ```

