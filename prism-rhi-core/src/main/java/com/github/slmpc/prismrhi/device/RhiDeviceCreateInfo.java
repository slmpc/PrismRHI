package com.github.slmpc.prismrhi.device;

import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.queue.RhiQueueRequest;
import com.github.slmpc.prismrhi.queue.RhiQueueType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RhiDeviceCreateInfo(
        String debugName,
        List<RhiQueueRequest> queues,
        Set<BackendFeature> enabledFeatures,
        Set<String> enabledExtensions
) {
    public RhiDeviceCreateInfo {
        debugName = debugName == null ? "" : debugName;
        queues = List.copyOf(queues == null || queues.isEmpty()
                ? List.of(new RhiQueueRequest(RhiQueueType.GRAPHICS, 1))
                : queues);
        enabledFeatures = Set.copyOf(enabledFeatures == null ? Set.of() : enabledFeatures);
        enabledExtensions = Set.copyOf(enabledExtensions == null ? Set.of() : enabledExtensions);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String debugName = "";
        private final List<RhiQueueRequest> queues = new ArrayList<>();
        private final Set<BackendFeature> enabledFeatures = new LinkedHashSet<>();
        private final Set<String> enabledExtensions = new LinkedHashSet<>();

        public Builder debugName(String debugName) {
            this.debugName = debugName;
            return this;
        }

        public Builder queue(RhiQueueType type, int count) {
            queues.add(new RhiQueueRequest(type, count));
            return this;
        }

        public Builder enableFeature(BackendFeature feature) {
            enabledFeatures.add(Objects.requireNonNull(feature, "feature"));
            return this;
        }

        public Builder addExtension(String extension) {
            if (extension != null && !extension.isBlank()) {
                enabledExtensions.add(extension);
            }
            return this;
        }

        public RhiDeviceCreateInfo build() {
            return new RhiDeviceCreateInfo(debugName, queues, enabledFeatures, enabledExtensions);
        }
    }
}
