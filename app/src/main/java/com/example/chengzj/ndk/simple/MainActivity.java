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

        appendResult("Ready. Tap a demo to run native code.\nLibrary: libndk_demo.so");
    }

    private void runDemo(DemoItem item) {
        appendResult("\n── " + item.title + " ──");
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
        // keep last ~4KB so the panel stays readable
        if (log.length() > 4096) {
            log.delete(0, log.length() - 4096);
        }
        resultView.setText(log.toString());
        // scroll parent if needed — TextView inside ScrollView
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
                "从 C++ 返回字符串",
                "NewStringUTF / loadLibrary",
                () -> appendResult(NativeBridge.stringFromJNI())
        ));

        demos.add(new DemoItem(
                "基础",
                "2. 基本类型加法",
                "传递 jint 并返回结果",
                "JNI 基本类型映射",
                () -> appendResult("add(40, 2) = " + NativeBridge.add(40, 2))
        ));

        demos.add(new DemoItem(
                "基础",
                "3. 字符串往返",
                "Java String → native → 新 String",
                "GetStringUTFChars / NewStringUTF",
                () -> appendResult(NativeBridge.greet("NDK 学员"))
        ));

        demos.add(new DemoItem(
                "数组",
                "4. int[] 求和",
                "GetIntArrayElements + Release",
                "数组临界区 / JNI_ABORT",
                () -> {
                    int[] values = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
                    appendResult("sum" + Arrays.toString(values) + " = " + NativeBridge.sumArray(values));
                }
        ));

        demos.add(new DemoItem(
                "数组",
                "5. 原地反转 byte[]",
                "修改同一块 Java 数组内存",
                "ReleaseByteArrayElements mode=0",
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
                "Native 分配并返回新数组",
                "NewByteArray / SetByteArrayRegion",
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
                "GetFieldID + GetObject/IntField",
                "访问 Person 的 name / age",
                () -> {
                    Person p = new Person("Ada", 36);
                    appendResult(NativeBridge.describePerson(p));
                }
        ));

        demos.add(new DemoItem(
                "对象",
                "8. Native 创建 Java 对象",
                "缓存 jclass + NewObject",
                "JNI_OnLoad GlobalRef 缓存",
                () -> {
                    Person created = NativeBridge.createPerson("Linus", 54);
                    appendResult("created: " + created);
                }
        ));

        demos.add(new DemoItem(
                "回调",
                "9. C++ 回调 Java 接口",
                "CallVoidMethod 同步回调",
                "NativeCallback.onResult",
                () -> NativeBridge.invokeCallback(
                        msg -> appendResult(msg),
                        "payload-from-java")
        ));

        demos.add(new DemoItem(
                "异常",
                "10. Native 抛出异常",
                "ThrowNew → Java catch",
                "IllegalArgumentException",
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
                "签名里是 jclass 不是 jobject",
                "static native",
                () -> appendResult("staticNativeMultiply(6, 7) = "
                        + NativeBridge.staticNativeMultiply(6, 7))
        ));

        demos.add(new DemoItem(
                "进阶",
                "12. ABI / 编译信息",
                "指针宽度、API level、架构",
                "__ANDROID_API__ / arch macros",
                () -> appendResult(NativeBridge.getNativeBuildInfo())
        ));

        demos.add(new DemoItem(
                "进阶",
                "13. 字符串 UTF 探测",
                "UTF-16 长度 vs Modified UTF-8 长度",
                "GetStringLength / GetStringUTFLength",
                () -> appendResult(NativeBridge.inspectString("你好NDK🚀"))
        ));

        demos.add(new DemoItem(
                "性能",
                "14. Java vs Native 循环",
                "同算法粗略计时对比",
                "别过度迷信 JNI 开销外的收益",
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
                "GlobalRef 跨线程持有 callback",
                () -> {
                    appendResult("starting native thread…");
                    NativeBridge.startNativeThread(msg -> runOnUiThread(() -> appendResult(msg)));
                }
        ));

        demos.add(new DemoItem(
                "NIO",
                "16. DirectByteBuffer",
                "零拷贝读原生内存",
                "GetDirectBufferAddress",
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
                "JNI_OnLoad + RegisterNatives",
                () -> appendResult(NativeBridge.dynamicPing())
        ));

        return demos;
    }

    /** Same rough algorithm as nativeChecksum for a fair-ish comparison. */
    private static long javaChecksum(int iterations) {
        long acc = 0;
        for (int i = 0; i < iterations; i++) {
            acc = (acc * 1315423911L) ^ i;
            acc += (long) (Math.sin(i & 0xFF) * 1000.0);
        }
        return acc;
    }
}
