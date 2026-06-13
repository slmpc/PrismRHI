package com.github.slmpc.prismrhi.backend;

import java.util.Set;

public record BackendInfo(
        String id,
        BackendApi api,
        String displayName,
        String description,
        Set<BackendFeature> features
) {
    public BackendInfo {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        api = java.util.Objects.requireNonNull(api, "api");
        description = description == null ? "" : description;
        features = Set.copyOf(features == null ? Set.of() : features);
    }
}
