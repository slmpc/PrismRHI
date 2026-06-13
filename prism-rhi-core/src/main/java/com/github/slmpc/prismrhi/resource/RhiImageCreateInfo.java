package com.github.slmpc.prismrhi.resource;

import com.github.slmpc.prismrhi.format.RhiExtent3D;
import com.github.slmpc.prismrhi.format.RhiFormat;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RhiImageCreateInfo(
        RhiExtent3D extent,
        RhiFormat format,
        Set<RhiImageUsage> usage,
        RhiMemoryUsage memoryUsage
) {
    public RhiImageCreateInfo {
        extent = Objects.requireNonNull(extent, "extent");
        format = format == null ? RhiFormat.RGBA8_UNORM : format;
        usage = Set.copyOf(usage == null || usage.isEmpty() ? Set.of(RhiImageUsage.SAMPLED) : usage);
        memoryUsage = memoryUsage == null ? RhiMemoryUsage.GPU_ONLY : memoryUsage;
    }

    public static Builder builder(RhiExtent3D extent) {
        return new Builder(extent);
    }

    public static final class Builder {
        private final RhiExtent3D extent;
        private RhiFormat format = RhiFormat.RGBA8_UNORM;
        private final Set<RhiImageUsage> usage = new LinkedHashSet<>();
        private RhiMemoryUsage memoryUsage = RhiMemoryUsage.GPU_ONLY;

        private Builder(RhiExtent3D extent) {
            this.extent = extent;
        }

        public Builder format(RhiFormat format) {
            this.format = format;
            return this;
        }

        public Builder usage(RhiImageUsage usage) {
            this.usage.add(usage);
            return this;
        }

        public Builder memoryUsage(RhiMemoryUsage memoryUsage) {
            this.memoryUsage = memoryUsage;
            return this;
        }

        public RhiImageCreateInfo build() {
            return new RhiImageCreateInfo(extent, format, usage, memoryUsage);
        }
    }
}
