package com.github.slmpc.prismrhi.descriptor;

import java.util.EnumSet;
import java.util.Set;

public record RhiDescriptorBinding(
        int binding,
        RhiDescriptorType type,
        int count,
        Set<RhiDescriptorStage> stages,
        Set<RhiDescriptorBindingFlag> flags
) {
    public RhiDescriptorBinding {
        if (binding < 0) {
            throw new IllegalArgumentException("binding must not be negative");
        }
        type = type == null ? RhiDescriptorType.UNIFORM_BUFFER : type;
        if (count < 1) {
            throw new IllegalArgumentException("descriptor count must be at least 1");
        }
        stages = Set.copyOf(stages == null || stages.isEmpty() ? EnumSet.of(RhiDescriptorStage.ALL) : stages);
        flags = Set.copyOf(flags == null ? Set.of() : flags);
    }

    public boolean variableDescriptorCount() {
        return flags.contains(RhiDescriptorBindingFlag.VARIABLE_DESCRIPTOR_COUNT);
    }

    public boolean updateAfterBind() {
        return flags.contains(RhiDescriptorBindingFlag.UPDATE_AFTER_BIND);
    }
}
