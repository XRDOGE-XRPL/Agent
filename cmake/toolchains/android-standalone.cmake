# Android NDK standalone toolchain for cross-compiling without the AOSP Soong/Make graph.
# Usage:
#   cmake -S . -B build-android \
#     -DCMAKE_TOOLCHAIN_FILE=cmake/toolchains/android-standalone.cmake \
#     -DANDROID_ABI=arm64-v8a \
#     -DANDROID_PLATFORM=android-30

if(DEFINED CMAKE_TOOLCHAIN_FILE AND NOT DEFINED _AGENT_ANDROID_TOOLCHAIN_LOADED)
    set(_AGENT_ANDROID_TOOLCHAIN_LOADED TRUE)
endif()

if(NOT DEFINED ANDROID_NDK)
    if(DEFINED ENV{ANDROID_NDK})
        set(ANDROID_NDK "$ENV{ANDROID_NDK}")
    elseif(DEFINED ENV{ANDROID_HOME})
        set(ANDROID_NDK "$ENV{ANDROID_HOME}/ndk/27.1.12297006")
    elseif(DEFINED ENV{ANDROID_SDK_ROOT})
        set(ANDROID_NDK "$ENV{ANDROID_SDK_ROOT}/ndk/27.1.12297006")
    endif()
endif()

if(NOT DEFINED ANDROID_NDK OR ANDROID_NDK STREQUAL "")
    message(FATAL_ERROR "Android NDK is required. Set ANDROID_NDK or export ANDROID_HOME/ANDROID_SDK_ROOT.")
endif()

if(NOT EXISTS "${ANDROID_NDK}/build/cmake/android.toolchain.cmake")
    message(FATAL_ERROR "Android NDK toolchain file not found under ${ANDROID_NDK}")
endif()

set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION "${ANDROID_PLATFORM}")
set(CMAKE_ANDROID_ARCH_ABI "${ANDROID_ABI}")
set(CMAKE_ANDROID_NDK "${ANDROID_NDK}")
set(CMAKE_ANDROID_API "${ANDROID_PLATFORM}")
set(CMAKE_ANDROID_STL_TYPE c++_static)

if(NOT DEFINED ANDROID_PLATFORM)
    set(ANDROID_PLATFORM android-30)
endif()

if(NOT DEFINED ANDROID_ABI)
    set(ANDROID_ABI arm64-v8a)
endif()

include("${ANDROID_NDK}/build/cmake/android.toolchain.cmake")
