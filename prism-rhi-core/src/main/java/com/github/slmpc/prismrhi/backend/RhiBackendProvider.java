package com.github.slmpc.prismrhi.backend;

import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;

public interface RhiBackendProvider {
    BackendInfo info();

    boolean isSupported();

    RhiInstance createInstance(RhiInstanceCreateInfo createInfo);
}
