package com.example.chengzj.ndk.simple;

/**
 * Created by chengzj on 2017/5/30.
 */

public class Java2CJNI {
    static {
        System.loadLibrary("Java2C");
    }
    public native String java2C();
}
