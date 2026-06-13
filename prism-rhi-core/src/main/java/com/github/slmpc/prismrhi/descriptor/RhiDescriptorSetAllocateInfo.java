package com.github.slmpc.prismrhi.descriptor;

import java.util.Objects;

public record RhiDescriptorSetAllocateInfo(RhiDescriptorSetLayout layout, int variableDescriptorCount) {
    public RhiDescriptorSetAllocateInfo {
        layout = Objects.requireNonNull(layout, "layout");
        if (variableDescriptorCount < 0) {
            throw new IllegalArgumentException("variableDescriptorCount must not be negative");
        }
    }

    public static RhiDescriptorSetAllocateInfo of(RhiDescriptorSetLayout layout) {
        return new RhiDescriptorSetAllocateInfo(layout, 0);
    }

    public static RhiDescriptorSetAllocateInfo bindless(RhiDescriptorSetLayout layout, int variableDescriptorCount) {
        return new RhiDescriptorSetAllocateInfo(layout, variableDescriptorCount);
    }
}
