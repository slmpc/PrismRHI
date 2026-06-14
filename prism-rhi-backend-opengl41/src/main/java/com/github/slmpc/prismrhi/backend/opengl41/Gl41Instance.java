package com.github.slmpc.prismrhi.backend.opengl41;

import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;

import java.util.List;

final class Gl41Instance implements RhiInstance {
    private final BackendInfo backendInfo;
    private boolean closed;

    Gl41Instance(BackendInfo backendInfo) {
        Gl41Support.requireCapabilities();
        this.backendInfo = backendInfo;
    }

    @Override
    public BackendInfo backendInfo() {
        return backendInfo;
    }

    @Override
    public List<RhiPhysicalDevice> enumeratePhysicalDevices() {
        ensureOpen();
        return List.of(new Gl41PhysicalDevice(backendInfo));
    }

    @Override
    public RhiDevice createDevice(RhiPhysicalDevice physicalDevice, RhiDeviceCreateInfo createInfo) {
        ensureOpen();
        if (!(physicalDevice instanceof Gl41PhysicalDevice)) {
            throw new RhiException("Physical device was not created by this OpenGL 4.1 instance");
        }
        return new Gl41Device(createInfo == null ? null : createInfo.glStateBridge());
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new RhiException("OpenGL 4.1 instance is closed");
        }
    }
}
