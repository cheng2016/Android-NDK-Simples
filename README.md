# Android-NDK-Simples
Android ndk的一个简单的例子



### 使用步骤

1. #### 搭建ndk环境



2. #### 创建jni调用类，定义本地方法



3. #### 通过javah命令获取jni调用类头文件

   `javah -classpath . -jni -d jni com.example.chengzj.ndk.simple.Java2CJNI`

4. #### 创建实现头文件的.c源文件，并实现头文件的本地方法

   ##### 

5. #### 配置并生成so文件



6. #### 加载so库



7. #### 执行调用本地方法