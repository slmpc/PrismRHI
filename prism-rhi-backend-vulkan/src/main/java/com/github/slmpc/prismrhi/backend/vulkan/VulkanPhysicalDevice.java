package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;
import com.github.slmpc.prismrhi.device.RhiPhysicalDeviceInfo;
import com.github.slmpc.prismrhi.device.RhiPhysicalDeviceType;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.util.Set;

final class VulkanPhysicalDevice implements RhiPhysicalDevice {
    private final VkPhysicalDevice handle;
    private final RhiPhysicalDeviceInfo info;

    VulkanPhysicalDevice(
            VkPhysicalDevice handle,
            String name,
            RhiPhysicalDeviceType type,
            int vendorId,
            int deviceId,
            Set<BackendFeature> features
    ) {
        this.handle = handle;
        this.info = new RhiPhysicalDeviceInfo(name, type, vendorId, deviceId, features);
    }

    @Override
    public RhiPhysicalDeviceInfo info() {
        return info;
    }

    @Override
    public long nativeHandle() {
        return handle.address();
    }

    VkPhysicalDevice handle() {
        return handle;
    }
}
