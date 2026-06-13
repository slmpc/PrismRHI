package com.github.slmpc.prismrhi.shader;

public interface RhiShaderCompiler {
    boolean supports(RhiShaderCodeType sourceType, RhiShaderCodeType targetType);

    RhiShaderModuleCreateInfo compile(
            RhiShaderModuleCreateInfo source,
            RhiShaderCodeType targetType,
            RhiShaderCompileOptions options
    );
}
