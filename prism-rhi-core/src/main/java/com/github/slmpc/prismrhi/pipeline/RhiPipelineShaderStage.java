package com.github.slmpc.prismrhi.pipeline;

import com.github.slmpc.prismrhi.shader.RhiShaderModule;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;

import java.util.Objects;

public record RhiPipelineShaderStage(RhiShaderStage stage, RhiShaderModule module, String entryPoint) {
    public RhiPipelineShaderStage {
        stage = Objects.requireNonNull(stage, "stage");
        module = Objects.requireNonNull(module, "module");
        entryPoint = entryPoint == null || entryPoint.isBlank() ? "main" : entryPoint;
    }
}
