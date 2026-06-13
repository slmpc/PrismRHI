package com.github.slmpc.prismrhi.shaderc;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.shader.RhiShaderCodeType;
import com.github.slmpc.prismrhi.shader.RhiShaderCompileOptions;
import com.github.slmpc.prismrhi.shader.RhiShaderCompiler;
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderOptimizationLevel;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;
import org.lwjgl.util.shaderc.Shaderc;

import java.nio.ByteBuffer;
import java.util.Map;

public final class ShadercCompiler implements RhiShaderCompiler {
    @Override
    public boolean supports(RhiShaderCodeType sourceType, RhiShaderCodeType targetType) {
        return sourceType == RhiShaderCodeType.GLSL && targetType == RhiShaderCodeType.SPIRV;
    }

    @Override
    public RhiShaderModuleCreateInfo compile(
            RhiShaderModuleCreateInfo source,
            RhiShaderCodeType targetType,
            RhiShaderCompileOptions options
    ) {
        if (!supports(source.codeType(), targetType)) {
            throw new RhiException("shaderc compiler supports GLSL to SPIR-V only");
        }
        long compiler = Shaderc.shaderc_compiler_initialize();
        if (compiler == 0L) {
            throw new RhiException("shaderc_compiler_initialize failed");
        }
        long compileOptions = Shaderc.shaderc_compile_options_initialize();
        if (compileOptions == 0L) {
            Shaderc.shaderc_compiler_release(compiler);
            throw new RhiException("shaderc_compile_options_initialize failed");
        }
        try {
            configureOptions(compileOptions, options);
            long result = Shaderc.shaderc_compile_into_spv(
                    compiler,
                    new String(source.code(), java.nio.charset.StandardCharsets.UTF_8),
                    shaderKind(source.stage()),
                    options.sourceName(),
                    source.entryPoint(),
                    compileOptions
            );
            if (result == 0L) {
                throw new RhiException("shaderc_compile_into_spv returned null");
            }
            try {
                int status = Shaderc.shaderc_result_get_compilation_status(result);
                if (status != Shaderc.shaderc_compilation_status_success) {
                    throw new RhiException(Shaderc.shaderc_result_get_error_message(result));
                }
                ByteBuffer bytes = Shaderc.shaderc_result_get_bytes(result);
                byte[] spirv = new byte[bytes.remaining()];
                bytes.get(spirv);
                return RhiShaderModuleCreateInfo.spirv(source.stage(), spirv, source.entryPoint());
            } finally {
                Shaderc.shaderc_result_release(result);
            }
        } finally {
            Shaderc.shaderc_compile_options_release(compileOptions);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private static void configureOptions(long optionsHandle, RhiShaderCompileOptions options) {
        Shaderc.shaderc_compile_options_set_source_language(optionsHandle, Shaderc.shaderc_source_language_glsl);
        Shaderc.shaderc_compile_options_set_target_env(
                optionsHandle,
                Shaderc.shaderc_target_env_vulkan,
                Shaderc.shaderc_env_version_vulkan_1_3
        );
        Shaderc.shaderc_compile_options_set_target_spirv(optionsHandle, Shaderc.shaderc_spirv_version_1_6);
        Shaderc.shaderc_compile_options_set_optimization_level(optionsHandle, optimization(options.optimizationLevel()));
        if (options.generateDebugInfo()) {
            Shaderc.shaderc_compile_options_set_generate_debug_info(optionsHandle);
        }
        if (options.warningsAsErrors()) {
            Shaderc.shaderc_compile_options_set_warnings_as_errors(optionsHandle);
        }
        for (Map.Entry<String, String> macro : options.macros().entrySet()) {
            Shaderc.shaderc_compile_options_add_macro_definition(optionsHandle, macro.getKey(), macro.getValue());
        }
    }

    private static int shaderKind(RhiShaderStage stage) {
        return switch (stage) {
            case VERTEX -> Shaderc.shaderc_vertex_shader;
            case FRAGMENT -> Shaderc.shaderc_fragment_shader;
            case COMPUTE -> Shaderc.shaderc_compute_shader;
        };
    }

    private static int optimization(RhiShaderOptimizationLevel level) {
        return switch (level == null ? RhiShaderOptimizationLevel.NONE : level) {
            case NONE -> Shaderc.shaderc_optimization_level_zero;
            case SIZE -> Shaderc.shaderc_optimization_level_size;
            case PERFORMANCE -> Shaderc.shaderc_optimization_level_performance;
        };
    }
}
