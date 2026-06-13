package com.github.slmpc.prismrhi.device;

public interface RhiPhysicalDevice {
    RhiPhysicalDeviceInfo info();

    default long nativeHandle() {
        return 0L;
    }
}
