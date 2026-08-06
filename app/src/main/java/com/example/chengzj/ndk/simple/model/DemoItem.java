package com.example.chengzj.ndk.simple.model;

/**
 * One list row in the learning demo UI.
 */
public class DemoItem {
    public final String category;
    public final String title;
    public final String summary;
    public final String topic;
    public final Runnable action;

    public DemoItem(String category, String title, String summary, String topic, Runnable action) {
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.topic = topic;
        this.action = action;
    }
}
