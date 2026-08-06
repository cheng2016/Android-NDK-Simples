package com.example.chengzj.ndk.simple.model;

import androidx.annotation.DrawableRes;

/**
 * One list row in the learning demo UI.
 */
public class DemoItem {
    public final String category;
    public final String title;
    /** Short one-liner: what this API does. */
    public final String summary;
    /** 用途：为什么要学这个. */
    public final String purpose;
    /** 实际场景：项目里能干啥. */
    public final String scenario;
    public final String topic;
    @DrawableRes
    public final int iconRes;
    public final Runnable action;

    public DemoItem(String category,
                    String title,
                    String summary,
                    String purpose,
                    String scenario,
                    String topic,
                    @DrawableRes int iconRes,
                    Runnable action) {
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.purpose = purpose;
        this.scenario = scenario;
        this.topic = topic;
        this.iconRes = iconRes;
        this.action = action;
    }
}
