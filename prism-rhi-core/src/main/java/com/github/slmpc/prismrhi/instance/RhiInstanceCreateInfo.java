package com.github.slmpc.prismrhi.instance;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.context.RhiContextCreateInfo;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RhiInstanceCreateInfo(
        BackendApi backend,
        String applicationName,
        boolean enableValidation,
        Set<String> enabledExtensions,
        long nativeInstanceHandle,
        boolean ownsNativeInstance,
        RhiContextCreateInfo contextCreateInfo
) {
    public RhiInstanceCreateInfo {
        backend = Objects.requireNonNull(backend, "backend");
        applicationName = applicationName == null || applicationName.isBlank() ? "PrismRHI Application" : applicationName;
        enabledExtensions = Set.copyOf(enabledExtensions == null ? Set.of() : enabledExtensions);
        if (nativeInstanceHandle != 0L && backend != BackendApi.VULKAN) {
            throw new IllegalArgumentException("native instance wrapping is only supported by the Vulkan backend");
        }
    }

    public static Builder builder(BackendApi backend) {
        return new Builder(backend);
    }

    public static final class Builder {
        private final BackendApi backend;
        private String applicationName = "PrismRHI Application";
        private boolean enableValidation;
        private final Set<String> enabledExtensions = new LinkedHashSet<>();
        private long nativeInstanceHandle;
        private boolean ownsNativeInstance;
        private RhiContextCreateInfo contextCreateInfo;

        private Builder(BackendApi backend) {
            this.backend = Objects.requireNonNull(backend, "backend");
        }

        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        public Builder enableValidation(boolean enableValidation) {
            this.enableValidation = enableValidation;
            return this;
        }

        public Builder addExtension(String extension) {
            if (extension != null && !extension.isBlank()) {
                enabledExtensions.add(extension);
            }
            return this;
        }

        public Builder extensions(Iterable<String> extensions) {
            if (extensions != null) {
                for (String extension : extensions) {
                    addExtension(extension);
                }
            }
            return this;
        }

        public Builder nativeInstanceHandle(long nativeInstanceHandle) {
            this.nativeInstanceHandle = nativeInstanceHandle;
            return this;
        }

        public Builder externalVulkanInstance(long instanceHandle) {
            this.nativeInstanceHandle = instanceHandle;
            this.ownsNativeInstance = false;
            return this;
        }

        public Builder ownsNativeInstance(boolean ownsNativeInstance) {
            this.ownsNativeInstance = ownsNativeInstance;
            return this;
        }

        public Builder context(RhiContextCreateInfo contextCreateInfo) {
            this.contextCreateInfo = contextCreateInfo;
            return this;
        }

        public RhiInstanceCreateInfo build() {
            return new RhiInstanceCreateInfo(
                    backend,
                    applicationName,
                    enableValidation,
                    enabledExtensions,
                    nativeInstanceHandle,
                    ownsNativeInstance,
                    contextCreateInfo
            );
        }
    }
}
