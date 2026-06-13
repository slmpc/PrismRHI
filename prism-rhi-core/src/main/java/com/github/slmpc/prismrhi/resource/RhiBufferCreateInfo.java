package com.github.slmpc.prismrhi.resource;

import java.util.LinkedHashSet;
import java.util.Set;

public record RhiBufferCreateInfo(long size, Set<RhiBufferUsage> usage, RhiMemoryUsage memoryUsage) {
    public RhiBufferCreateInfo {
        if (size <= 0) {
            throw new IllegalArgumentException("buffer size must be positive");
        }
        usage = Set.copyOf(usage == null || usage.isEmpty() ? Set.of(RhiBufferUsage.TRANSFER_DST) : usage);
        memoryUsage = memoryUsage == null ? RhiMemoryUsage.GPU_ONLY : memoryUsage;
    }

    public static Builder builder(long size) {
        return new Builder(size);
    }

    public static final class Builder {
        private final long size;
        private final Set<RhiBufferUsage> usage = new LinkedHashSet<>();
        private RhiMemoryUsage memoryUsage = RhiMemoryUsage.GPU_ONLY;

        private Builder(long size) {
            this.size = size;
        }

        public Builder usage(RhiBufferUsage usage) {
            this.usage.add(usage);
            return this;
        }

        public Builder memoryUsage(RhiMemoryUsage memoryUsage) {
            this.memoryUsage = memoryUsage;
            return this;
        }

        public RhiBufferCreateInfo build() {
            return new RhiBufferCreateInfo(size, usage, memoryUsage);
        }
    }
}
