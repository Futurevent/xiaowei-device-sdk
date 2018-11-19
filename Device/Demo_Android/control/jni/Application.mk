APP_STL := stlport_shared
APP_ABI := armeabi,x86
APP_PLATFORM := android-17
NDK_TOOLCHAIN_VERSION := 4.9
APP_CPPFLAGS += -Wno-error=format-security
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -frtti