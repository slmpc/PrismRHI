package com.github.slmpc.prismrhi;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.backend.RhiBackendProvider;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class PrismRHI {
    private PrismRHI() {
    }

    public static List<RhiBackendProvider> providers() {
        return ServiceLoader.load(RhiBackendProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(provider -> provider.info().id()))
                .toList();
    }

    public static Optional<RhiBackendProvider> findProvider(BackendApi api) {
        return providers().stream()
                .filter(provider -> provider.info().api() == api)
                .findFirst();
    }

    public static RhiInstance createInstance(RhiInstanceCreateInfo createInfo) {
        var provider = findProvider(createInfo.backend())
                .orElseThrow(() -> new RhiException("No RHI backend provider found for " + createInfo.backend()));
        if (!provider.isSupported()) {
            throw new RhiException(provider.info().displayName() + " is not supported in the current process");
        }
        return provider.createInstance(createInfo);
    }
}
