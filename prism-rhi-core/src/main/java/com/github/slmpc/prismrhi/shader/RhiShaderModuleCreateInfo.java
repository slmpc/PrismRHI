package com.github.slmpc.prismrhi.shader;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record RhiShaderModuleCreateInfo(
        RhiShaderStage stage,
        RhiShaderCodeType codeType,
        byte[] code,
        String entryPoint
) {
    public RhiShaderModuleCreateInfo {
        stage = stage == null ? RhiShaderStage.VERTEX : stage;
        codeType = codeType == null ? RhiShaderCodeType.GLSL : codeType;
        code = code == null ? new byte[0] : Arrays.copyOf(code, code.length);
        entryPoint = entryPoint == null || entryPoint.isBlank() ? "main" : entryPoint;
        if (code.length == 0) {
            throw new IllegalArgumentException("shader code must not be empty");
        }
    }

    @Override
    public byte[] code() {
        return Arrays.copyOf(code, code.length);
    }

    public static RhiShaderModuleCreateInfo glsl(RhiShaderStage stage, String source) {
        return glsl(stage, source, "main");
    }

    public static RhiShaderModuleCreateInfo glsl(RhiShaderStage stage, String source, String entryPoint) {
        return new RhiShaderModuleCreateInfo(
                stage,
                RhiShaderCodeType.GLSL,
                (source == null ? "" : source).getBytes(StandardCharsets.UTF_8),
                entryPoint
        );
    }

    public static RhiShaderModuleCreateInfo spirv(RhiShaderStage stage, byte[] code) {
        return spirv(stage, code, "main");
    }

    public static RhiShaderModuleCreateInfo spirv(RhiShaderStage stage, byte[] code, String entryPoint) {
        return new RhiShaderModuleCreateInfo(stage, RhiShaderCodeType.SPIRV, code, entryPoint);
    }
}
