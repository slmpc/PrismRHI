package com.github.slmpc.prismrhi.backend.opengl41;

import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;
import com.github.slmpc.prismrhi.device.RhiPhysicalDeviceInfo;

final class Gl41PhysicalDevice implements RhiPhysicalDevice {
    private final BackendInfo backendInfo;

    Gl41PhysicalDevice(BackendInfo backendInfo) {
        this.backendInfo = backendInfo;
    }

    @Override
    public RhiPhysicalDeviceInfo info() {
        String vendor = Gl41Support.vendorName();
        return new RhiPhysicalDeviceInfo(
                Gl41Support.rendererName(),
                Gl41Support.inferDeviceType(vendor),
                0,
                0,
                backendInfo.features().stream()
                        .filter(feature -> feature != BackendFeature.COMMAND_BUFFERS)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet())
        );
    }
}
