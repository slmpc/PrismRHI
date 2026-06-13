package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;
import com.github.slmpc.prismrhi.device.RhiPhysicalDeviceInfo;

final class GlDsaPhysicalDevice implements RhiPhysicalDevice {
    private final BackendInfo backendInfo;

    GlDsaPhysicalDevice(BackendInfo backendInfo) {
        this.backendInfo = backendInfo;
    }

    @Override
    public RhiPhysicalDeviceInfo info() {
        String vendor = GlDsaSupport.vendorName();
        return new RhiPhysicalDeviceInfo(
                GlDsaSupport.rendererName(),
                GlDsaSupport.inferDeviceType(vendor),
                0,
                0,
                backendInfo.features()
        );
    }
}
