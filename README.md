# Android NDK Simples

现代化的 Android NDK / JNI 学习示例：从「Hello Native」到线程回调、`RegisterNatives`、DirectByteBuffer 等核心知识点，点一下就能跑。

> 原项目（2017）已全面升级：AGP 8.5 + CMake + AndroidX，去掉无关 BottomNavigation，改成可点选的 Demo 列表。

## 环境要求

- Android Studio Hedgehog 或更新版本
- JDK 17
- NDK + CMake（SDK Manager 中安装）
- 建议 NDK r25+ / CMake 3.22.1

## 快速开始

1. 用 Android Studio 打开本仓库根目录
2. 等待 Gradle Sync（首次会下载依赖与 NDK 组件）
3. 连接设备或启动模拟器，Run `app`
4. 在列表中点任意示例，底部面板查看 native 返回结果

命令行（需本机已配置好 wrapper / SDK）：

```bash
./gradlew :app:assembleDebug
```

## 示例一览（17 个）

| # | 主题 | 你能学到 |
|---|------|----------|
| 1 | Hello from Native | `System.loadLibrary`、`NewStringUTF` |
| 2 | 基本类型加法 | JNI 基本类型（`jint` 等） |
| 3 | 字符串往返 | `GetStringUTFChars` / `ReleaseStringUTFChars` |
| 4 | int[] 求和 | `GetIntArrayElements`、`JNI_ABORT` |
| 5 | 原地反转 byte[] | 修改同一块 Java 数组（commit mode=0） |
| 6 | XOR 加密 byte[] | `NewByteArray` / `SetByteArrayRegion` |
| 7 | 读取 Java 对象字段 | `GetFieldID`、`GetObjectField` |
| 8 | Native 创建 Java 对象 | `NewObject`、GlobalRef 缓存 |
| 9 | C++ 回调 Java | `CallVoidMethod` + 接口 |
| 10 | Native 抛异常 | `ThrowNew` → Java `catch` |
| 11 | 静态 native | `jclass` 与 `jobject` 区别 |
| 12 | ABI / 编译信息 | 架构宏、`__ANDROID_API__` |
| 13 | 字符串 UTF 探测 | `GetStringLength` vs `GetStringUTFLength` |
| 14 | Java vs Native 循环 | JNI 调用开销与 native 算力权衡 |
| 15 | Native 线程回调 | `pthread` + `AttachCurrentThread` + GlobalRef |
| 16 | DirectByteBuffer | `GetDirectBufferAddress` 零拷贝 |
| 17 | RegisterNatives | `JNI_OnLoad` 动态注册，无需 `Java_` 符号名 |

## 工程结构

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt          # CMake 构建 libndk_demo.so
│   ├── native_bridge.cpp       # 全部 JNI 实现
│   └── jni_helpers.cpp/.h      # 日志、字符串工具、jclass 缓存
├── java/.../ndk/simple/
│   ├── MainActivity.java       # Demo 列表 UI
│   ├── NativeBridge.java       # native 方法声明
│   ├── NativeCallback.java     # 供 C++ 回调的接口
│   ├── DemoAdapter.java
│   └── model/Person.java       # 字段访问 / NewObject 演示
└── res/layout/
    ├── activity_main.xml
    └── item_demo.xml
```

## 关键知识点速记

### 1. 加载 so

```java
static {
    System.loadLibrary("ndk_demo"); // 对应 libndk_demo.so
}
```

### 2. CMake 片段

```cmake
add_library(ndk_demo SHARED native_bridge.cpp jni_helpers.cpp)
target_link_libraries(ndk_demo log android)
```

`app/build.gradle` 中通过 `externalNativeBuild { cmake { ... } }` 接入。

### 3. 静态注册 vs 动态注册

- **静态**：方法名必须匹配 `Java_包名_类名_方法名`
- **动态**：在 `JNI_OnLoad` 里 `RegisterNatives`，符号名可自定义（见示例 17）

### 4. 跨线程回调注意点

1. `NewGlobalRef` 持有 callback，避免局部引用失效  
2. 工作线程 `AttachCurrentThread` 拿到 `JNIEnv*`  
3. 回调结束后 `DeleteGlobalRef` + `DetachCurrentThread`  
4. UI 更新仍需切回主线程（本 Demo 用 `runOnUiThread`）

### 5. ABI

当前默认打包：`armeabi-v7a`、`arm64-v8a`、`x86`、`x86_64`。真机发布可只保留 ARM：

```gradle
ndk {
    abiFilters "armeabi-v7a", "arm64-v8a"
}
```

## 相对旧版的升级

| 旧 (2017) | 新 (2.0) |
|-----------|----------|
| AGP 2.3 / Gradle 3.3 | AGP 8.5 / Gradle 8.7 |
| Support Library | AndroidX + Material |
| `ndk.moduleName` + Android.mk | CMake `externalNativeBuild` |
| `armeabi` / `mips` / productFlavors | 现代 ABI filters |
| 单一 `java2C()` | 17 个可交互知识点 |
| BottomNavigation 模板壳 | 专注 NDK 的 Demo 列表 |

## License

Apache License 2.0 — 见原作者 Copyright 2016 cheng2016。
