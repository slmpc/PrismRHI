package com.github.slmpc.prismrhi.device;

import com.github.slmpc.prismrhi.backend.BackendFeature;

import java.util.Set;

public record RhiPhysicalDeviceInfo(
        String name,
        RhiPhysicalDeviceType type,
        int vendorId,
        int deviceId,
        Set<BackendFeature> features
) {
    public RhiPhysicalDeviceInfo {
        name = name == null || name.isBlank() ? "Unknown GPU" : name;
        type = type == null ? RhiPhysicalDeviceType.OTHER : type;
        features = Set.copyOf(features == null ? Set.of() : features);
    }
}
