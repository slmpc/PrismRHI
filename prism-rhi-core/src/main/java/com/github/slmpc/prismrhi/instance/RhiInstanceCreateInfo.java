package com.github.slmpc.prismrhi.instance;

import com.github.slmpc.prismrhi.backend.BackendApi;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record RhiInstanceCreateInfo(
        BackendApi backend,
        String applicationName,
        boolean enableValidation,
        Set<String> enabledExtensions
) {
    public RhiInstanceCreateInfo {
        backend = Objects.requireNonNull(backend, "backend");
        applicationName = applicationName == null || applicationName.isBlank() ? "PrismRHI Application" : applicationName;
        enabledExtensions = Set.copyOf(enabledExtensions == null ? Set.of() : enabledExtensions);
    }

    public static Builder builder(BackendApi backend) {
        return new Builder(backend);
    }

    public static final class Builder {
        private final BackendApi backend;
        private String applicationName = "PrismRHI Application";
        private boolean enableValidation;
        private final Set<String> enabledExtensions = new LinkedHashSet<>();

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

        public RhiInstanceCreateInfo build() {
            return new RhiInstanceCreateInfo(backend, applicationName, enableValidation, enabledExtensions);
        }
    }
}
