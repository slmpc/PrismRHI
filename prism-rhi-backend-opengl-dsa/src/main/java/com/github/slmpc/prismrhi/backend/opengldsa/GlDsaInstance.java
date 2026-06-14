package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;

import java.util.List;

final class GlDsaInstance implements RhiInstance {
    private final BackendInfo backendInfo;
    private boolean closed;

    GlDsaInstance(BackendInfo backendInfo) {
        GlDsaSupport.requireCapabilities();
        this.backendInfo = backendInfo;
    }

    @Override
    public BackendInfo backendInfo() {
        return backendInfo;
    }

    @Override
    public List<RhiPhysicalDevice> enumeratePhysicalDevices() {
        ensureOpen();
        return List.of(new GlDsaPhysicalDevice(backendInfo));
    }

    @Override
    public RhiDevice createDevice(RhiPhysicalDevice physicalDevice, RhiDeviceCreateInfo createInfo) {
        ensureOpen();
        if (!(physicalDevice instanceof GlDsaPhysicalDevice)) {
            throw new RhiException("Physical device was not created by this OpenGL DSA instance");
        }
        return new GlDsaDevice(createInfo == null ? null : createInfo.glStateBridge());
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new RhiException("OpenGL DSA instance is closed");
        }
    }
}
