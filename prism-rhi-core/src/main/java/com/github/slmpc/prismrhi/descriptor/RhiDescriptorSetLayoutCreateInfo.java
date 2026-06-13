package com.github.slmpc.prismrhi.descriptor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record RhiDescriptorSetLayoutCreateInfo(
        List<RhiDescriptorBinding> bindings,
        Set<RhiDescriptorLayoutFlag> flags
) {
    public RhiDescriptorSetLayoutCreateInfo {
        bindings = List.copyOf(bindings == null ? List.of() : bindings);
        flags = Set.copyOf(flags == null ? Set.of() : flags);
        if (bindings.stream().filter(RhiDescriptorBinding::variableDescriptorCount).count() > 1) {
            throw new IllegalArgumentException("only one variable descriptor count binding is allowed per set layout");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<RhiDescriptorBinding> bindings = new ArrayList<>();
        private final Set<RhiDescriptorLayoutFlag> flags = EnumSet.noneOf(RhiDescriptorLayoutFlag.class);

        public Builder binding(int binding, RhiDescriptorType type, int count, RhiDescriptorStage... stages) {
            return binding(binding, type, count, Set.of(stages), Set.of());
        }

        public Builder binding(
                int binding,
                RhiDescriptorType type,
                int count,
                Set<RhiDescriptorStage> stages,
                Set<RhiDescriptorBindingFlag> flags
        ) {
            bindings.add(new RhiDescriptorBinding(binding, type, count, stages, flags));
            if (flags != null && flags.contains(RhiDescriptorBindingFlag.UPDATE_AFTER_BIND)) {
                this.flags.add(RhiDescriptorLayoutFlag.UPDATE_AFTER_BIND_POOL);
            }
            return this;
        }

        public Builder bindless(
                int binding,
                RhiDescriptorType type,
                int maxCount,
                RhiDescriptorStage... stages
        ) {
            Set<RhiDescriptorBindingFlag> bindingFlags = new LinkedHashSet<>();
            bindingFlags.add(RhiDescriptorBindingFlag.UPDATE_AFTER_BIND);
            bindingFlags.add(RhiDescriptorBindingFlag.UPDATE_UNUSED_WHILE_PENDING);
            bindingFlags.add(RhiDescriptorBindingFlag.PARTIALLY_BOUND);
            bindingFlags.add(RhiDescriptorBindingFlag.VARIABLE_DESCRIPTOR_COUNT);
            return binding(binding, type, maxCount, Set.of(stages), bindingFlags);
        }

        public Builder flag(RhiDescriptorLayoutFlag flag) {
            flags.add(flag);
            return this;
        }

        public RhiDescriptorSetLayoutCreateInfo build() {
            return new RhiDescriptorSetLayoutCreateInfo(bindings, flags);
        }
    }
}
