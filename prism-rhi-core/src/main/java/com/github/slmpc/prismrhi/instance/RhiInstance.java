package com.github.slmpc.prismrhi.instance;

import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;

import java.util.List;

public interface RhiInstance extends AutoCloseable {
    BackendInfo backendInfo();

    List<RhiPhysicalDevice> enumeratePhysicalDevices();

    RhiDevice createDevice(RhiPhysicalDevice physicalDevice, RhiDeviceCreateInfo createInfo);

    @Override
    void close();
}
