package com.example.chengzj.ndk.simple;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chengzj.ndk.simple.model.DemoItem;
import com.example.chengzj.ndk.simple.model.Person;
import com.google.android.material.appbar.MaterialToolbar;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView resultView;
    private final StringBuilder log = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        resultView = findViewById(R.id.result_text);
        RecyclerView list = findViewById(R.id.demo_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new DemoAdapter(buildDemos(), this::runDemo));

        appendResult("Ready. 点卡片运行 native 代码。\nLibrary: libndk_demo.so");
    }

    private void runDemo(DemoItem item) {
        appendResult("\n── " + item.title + " ──");
        appendResult(item.purpose);
        try {
            item.action.run();
        } catch (Throwable t) {
            appendResult("ERROR: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void appendResult(String line) {
        if (log.length() > 0) {
            log.append('\n');
        }
        log.append(line);
        if (log.length() > 4096) {
            log.delete(0, log.length() - 4096);
        }
        resultView.setText(log.toString());
        resultView.post(() -> {
            android.view.View parent = (android.view.View) resultView.getParent();
            if (parent instanceof android.widget.ScrollView) {
                ((android.widget.ScrollView) parent).fullScroll(android.view.View.FOCUS_DOWN);
            }
        });
    }

    private List<DemoItem> buildDemos() {
        List<DemoItem> demos = new ArrayList<>();

        demos.add(new DemoItem(
                "基础",
                "1. Hello from Native",
                "从 C++ 返回一句 Hello",
                "用途：验证 so 能加载、JNI 链路通畅，这是所有 NDK 项目的第一步。",
                "场景：App 启动探针、native 模块版本号、崩溃前确认库是否装上。",
                "API: loadLibrary / NewStringUTF",
                R.drawable.demo_01_hello,
                () -> appendResult(NativeBridge.stringFromJNI())
        ));

        demos.add(new DemoItem(
                "基础",
                "2. 基本类型加法",
                "把两个 int 丢进 native 做加法",
                "用途：搞清 Java int ↔ jint 等基本类型怎么映射，没有对象开销。",
                "场景：传感器数值换算、协议字段解析、简单打分/校验和逻辑下沉。",
                "API: jint 参数与返回值",
                R.drawable.demo_02_add,
                () -> appendResult("add(40, 2) = " + NativeBridge.add(40, 2))
        ));

        demos.add(new DemoItem(
                "基础",
                "3. 字符串往返",
                "Java String → C++ → 新 String",
                "用途：学会 GetStringUTFChars / NewStringUTF，避免字符串泄漏与乱码。",
                "场景：路径拼接、日志格式化、把 native 错误信息带回 UI。",
                "API: GetStringUTFChars / NewStringUTF",
                R.drawable.demo_03_string,
                () -> appendResult(NativeBridge.greet("NDK 学员"))
        ));

        demos.add(new DemoItem(
                "数组",
                "4. int[] 求和",
                "批量读取 Java int 数组并求和",
                "用途：掌握 GetIntArrayElements / Release，理解临界区与 JNI_ABORT。",
                "场景：音频采样统计、特征数组聚合、批量评分——比逐个 JNI 调用快得多。",
                "API: GetIntArrayElements + JNI_ABORT",
                R.drawable.demo_04_array,
                () -> {
                    int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
                    appendResult("sum" + Arrays.toString(values) + " = " + NativeBridge.sumArray(values));
                }
        ));

        demos.add(new DemoItem(
                "数组",
                "5. 原地反转 byte[]",
                "直接改同一块 Java 字节数组",
                "用途：体会 ReleaseByteArrayElements(mode=0) 如何把修改写回 Java。",
                "场景：字节序翻转、简易加扰、对网络包/文件块做 in-place 处理。",
                "API: Get/ReleaseByteArrayElements mode=0",
                R.drawable.demo_05_reverse,
                () -> {
                    byte[] data = {1, 2, 3, 4, 5};
                    appendResult("before: " + Arrays.toString(data));
                    NativeBridge.reverseBytesInPlace(data);
                    appendResult("after:  " + Arrays.toString(data));
                }
        ));

        demos.add(new DemoItem(
                "数组",
                "6. XOR 加密 byte[]",
                "Native 分配并返回新的密文字节",
                "用途：学会 NewByteArray / SetByteArrayRegion，从 native 造出 Java 数组。",
                "场景：轻量混淆、固件包异或、配合更强算法前的预处理层（示例非安全加密）。",
                "API: NewByteArray / SetByteArrayRegion",
                R.drawable.demo_06_xor,
                () -> {
                    byte[] plain = "NDK".getBytes(StandardCharsets.UTF_8);
                    byte[] cipher = NativeBridge.xorBytes(plain, (byte) 0x5A);
                    byte[] back = NativeBridge.xorBytes(cipher, (byte) 0x5A);
                    appendResult("plain=" + Arrays.toString(plain)
                            + "\ncipher=" + Arrays.toString(cipher)
                            + "\nroundtrip=" + new String(back, StandardCharsets.UTF_8));
                }
        ));

        demos.add(new DemoItem(
                "对象",
                "7. 读取 Java 对象字段",
                "用 GetFieldID 读 Person.name / age",
                "用途：让 C++ 直接读 Java 对象状态，不必先拆成一堆基本类型参数。",
                "场景：把配置/用户模型传给 native 渲染、物理引擎、业务规则引擎。",
                "API: GetFieldID / GetObjectField / GetIntField",
                R.drawable.demo_07_fields,
                () -> {
                    Person p = new Person("Ada", 36);
                    appendResult(NativeBridge.describePerson(p));
                }
        ));

        demos.add(new DemoItem(
                "对象",
                "8. Native 创建 Java 对象",
                "C++ NewObject 造出一个 Person",
                "用途：学会缓存 jclass + 构造方法，从 native 返回结构化结果。",
                "场景：解析协议后直接吐 Java Bean、模型推理结果封装、减少多次 JNI 往返。",
                "API: NewObject + JNI_OnLoad GlobalRef 缓存",
                R.drawable.demo_08_create,
                () -> {
                    Person created = NativeBridge.createPerson("Linus", 54);
                    appendResult("created: " + created);
                }
        ));

        demos.add(new DemoItem(
                "回调",
                "9. C++ 回调 Java 接口",
                "CallVoidMethod 同步通知 Java",
                "用途：native 算完/遇到事件时主动回调，而不是 Java 轮询。",
                "场景：下载进度、解码完成、传感器事件、播放器状态变化通知 UI。",
                "API: CallVoidMethod + NativeCallback",
                R.drawable.demo_09_callback,
                () -> NativeBridge.invokeCallback(
                        msg -> appendResult(msg),
                        "payload-from-java")
        ));

        demos.add(new DemoItem(
                "异常",
                "10. Native 抛出异常",
                "ThrowNew → Java catch 接住",
                "用途：把 native 错误变成 Java 异常，调用方用熟悉的 try/catch 处理。",
                "场景：参数非法、文件打不开、解码失败——统一错误模型，避免魔法返回码。",
                "API: ThrowNew / IllegalArgumentException",
                R.drawable.demo_10_exception,
                () -> {
                    try {
                        NativeBridge.throwIfNegative(-7);
                        appendResult("unexpected: no exception");
                    } catch (IllegalArgumentException e) {
                        appendResult("caught as expected: " + e.getMessage());
                    }
                    NativeBridge.throwIfNegative(3);
                    appendResult("value=3 accepted (no throw)");
                }
        ));

        demos.add(new DemoItem(
                "进阶",
                "11. 静态 native 方法",
                "不需要实例，签名里是 jclass",
                "用途：分清实例 native 与静态 native，工具函数不必 new 对象。",
                "场景：纯函数工具库、数学内核、无状态编解码入口。",
                "API: static native / jclass",
                R.drawable.demo_11_static,
                () -> appendResult("staticNativeMultiply(6, 7) = "
                        + NativeBridge.staticNativeMultiply(6, 7))
        ));

        demos.add(new DemoItem(
                "进阶",
                "12. ABI / 编译信息",
                "读指针宽度、API level、CPU 架构",
                "用途：运行时识别当前 so 是哪套 ABI，排查包错架构/兼容问题。",
                "场景：崩溃上报附带 ABI、按架构切换算法路径、兼容性诊断页。",
                "API: __ANDROID_API__ / arch macros",
                R.drawable.demo_12_abi,
                () -> appendResult(NativeBridge.getNativeBuildInfo())
        ));

        demos.add(new DemoItem(
                "进阶",
                "13. 字符串 UTF 探测",
                "UTF-16 长度 vs Modified UTF-8 长度",
                "用途：理解 JNI 字符串编码坑，中文/emoji 长度为什么和 Java 看到的不一样。",
                "场景：协议按字节截断、缓冲区预分配、国际化文案进 native 前的长度估算。",
                "API: GetStringLength / GetStringUTFLength",
                R.drawable.demo_13_utf,
                () -> appendResult(NativeBridge.inspectString("你好NDK🚀"))
        ));

        demos.add(new DemoItem(
                "性能",
                "14. Java vs Native 循环",
                "同算法粗略计时对比",
                "用途：用数据感受：热循环可能 native 更快，但 JNI 边界本身也有开销。",
                "场景：决定图像滤波、音频 DSP、压缩是否值得下沉；避免「为了 NDK 而 NDK」。",
                "API: nativeChecksum 计时对比",
                R.drawable.demo_14_bench,
                () -> {
                    final int n = 200_000;
                    long t0 = System.nanoTime();
                    long javaAcc = javaChecksum(n);
                    long javaMs = (System.nanoTime() - t0) / 1_000_000L;

                    t0 = System.nanoTime();
                    long nativeAcc = NativeBridge.nativeChecksum(n);
                    long nativeMs = (System.nanoTime() - t0) / 1_000_000L;

                    appendResult(String.format(Locale.US,
                            "iterations=%d\nJava:   %d ms, checksum=%d\nNative: %d ms, checksum=%d",
                            n, javaMs, javaAcc, nativeMs, nativeAcc));
                }
        ));

        demos.add(new DemoItem(
                "线程",
                "15. Native 线程回调",
                "pthread + AttachCurrentThread",
                "用途：后台 pthread 里安全回调 Java：GlobalRef、Attach、Detach 三件套。",
                "场景：解码线程、网络/采集线程、长时间任务完成后再通知主线程刷新 UI。",
                "API: pthread / AttachCurrentThread / GlobalRef",
                R.drawable.demo_15_thread,
                () -> {
                    appendResult("starting native thread…");
                    NativeBridge.startNativeThread(msg -> runOnUiThread(() -> appendResult(msg)));
                }
        ));

        demos.add(new DemoItem(
                "NIO",
                "16. DirectByteBuffer",
                "零拷贝读 DirectBuffer 原生内存",
                "用途：GetDirectBufferAddress 让 C++ 直接碰 Java 堆外内存，少一次拷贝。",
                "场景：Camera/MediaCodec 缓冲、OpenGL/Vulkan 上传、高性能音视频流水线。",
                "API: GetDirectBufferAddress / Capacity",
                R.drawable.demo_16_buffer,
                () -> {
                    ByteBuffer direct = ByteBuffer.allocateDirect(8);
                    for (int i = 0; i < 8; i++) {
                        direct.put((byte) (i + 1));
                    }
                    direct.clear();
                    int sum = NativeBridge.sumDirectBuffer(direct);
                    ByteBuffer heap = ByteBuffer.allocate(8);
                    int heapSum = NativeBridge.sumDirectBuffer(heap);
                    appendResult("direct sum(1..8)=" + sum + "\nheap buffer returned " + heapSum
                            + " (expected -1 = not direct)");
                }
        ));

        demos.add(new DemoItem(
                "进阶",
                "17. RegisterNatives",
                "动态注册，无需 Java_ 符号名",
                "用途：JNI_OnLoad 里 RegisterNatives，控制导出符号、便于混淆与多库共存。",
                "场景：商业 SDK 隐藏符号、插件式 native 模块、统一在加载时完成绑定。",
                "API: JNI_OnLoad + RegisterNatives",
                R.drawable.demo_17_register,
                () -> appendResult(NativeBridge.dynamicPing())
        ));

        return demos;
    }

    private static long javaChecksum(int iterations) {
        long acc = 0;
        for (int i = 0; i < iterations; i++) {
            acc = (acc * 1315423911L) ^ i;
            acc += (long) (Math.sin(i & 0xFF) * 1000.0);
        }
        return acc;
    }
}
