package com.github.slmpc.prismrhi.instance;

import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.context.RhiContext;
import com.github.slmpc.prismrhi.context.RhiContextCreateInfo;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;
import com.github.slmpc.prismrhi.RhiException;

import java.util.List;
import java.util.Optional;

public interface RhiInstance extends AutoCloseable {
    BackendInfo backendInfo();

    default long nativeHandle() {
        return 0L;
    }

    default Optional<RhiContext> context() {
        return Optional.empty();
    }

    default RhiContext createContext(RhiContextCreateInfo createInfo) {
        throw new RhiException(backendInfo().displayName() + " does not support RHI-managed contexts");
    }

    List<RhiPhysicalDevice> enumeratePhysicalDevices();

    RhiDevice createDevice(RhiPhysicalDevice physicalDevice, RhiDeviceCreateInfo createInfo);

    @Override
    void close();
}
