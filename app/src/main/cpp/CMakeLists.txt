cmake_minimum_required(VERSION 3.22.1)
project(ndk_demo)

add_library(ndk_demo SHARED
        native_bridge.cpp
        jni_helpers.cpp)

find_library(log-lib log)

target_link_libraries(ndk_demo
        ${log-lib}
        android)
