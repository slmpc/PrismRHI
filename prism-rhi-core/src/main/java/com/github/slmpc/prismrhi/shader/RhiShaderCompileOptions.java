package com.github.slmpc.prismrhi.shader;

import java.util.LinkedHashMap;
import java.util.Map;

public record RhiShaderCompileOptions(
        String sourceName,
        RhiShaderOptimizationLevel optimizationLevel,
        boolean generateDebugInfo,
        boolean warningsAsErrors,
        Map<String, String> macros
) {
    public RhiShaderCompileOptions {
        sourceName = sourceName == null || sourceName.isBlank() ? "shader.glsl" : sourceName;
        optimizationLevel = optimizationLevel == null ? RhiShaderOptimizationLevel.NONE : optimizationLevel;
        macros = Map.copyOf(macros == null ? Map.of() : macros);
    }

    public static RhiShaderCompileOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String sourceName = "shader.glsl";
        private RhiShaderOptimizationLevel optimizationLevel = RhiShaderOptimizationLevel.NONE;
        private boolean generateDebugInfo;
        private boolean warningsAsErrors;
        private final Map<String, String> macros = new LinkedHashMap<>();

        public Builder sourceName(String sourceName) {
            this.sourceName = sourceName;
            return this;
        }

        public Builder optimizationLevel(RhiShaderOptimizationLevel optimizationLevel) {
            this.optimizationLevel = optimizationLevel;
            return this;
        }

        public Builder generateDebugInfo(boolean generateDebugInfo) {
            this.generateDebugInfo = generateDebugInfo;
            return this;
        }

        public Builder warningsAsErrors(boolean warningsAsErrors) {
            this.warningsAsErrors = warningsAsErrors;
            return this;
        }

        public Builder macro(String name) {
            return macro(name, "");
        }

        public Builder macro(String name, String value) {
            if (name != null && !name.isBlank()) {
                macros.put(name, value == null ? "" : value);
            }
            return this;
        }

        public RhiShaderCompileOptions build() {
            return new RhiShaderCompileOptions(
                    sourceName,
                    optimizationLevel,
                    generateDebugInfo,
                    warningsAsErrors,
                    macros
            );
        }
    }
}
