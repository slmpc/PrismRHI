package com.github.slmpc.prismrhi.shader;

import com.github.slmpc.prismrhi.RhiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class RhiShaderCompilers {
    private RhiShaderCompilers() {
    }

    public static List<RhiShaderCompiler> available() {
        List<RhiShaderCompiler> compilers = new ArrayList<>();
        ServiceLoader.load(RhiShaderCompiler.class).forEach(compilers::add);
        return List.copyOf(compilers);
    }

    public static Optional<RhiShaderCompiler> find(RhiShaderCodeType sourceType, RhiShaderCodeType targetType) {
        return available().stream()
                .filter(compiler -> compiler.supports(sourceType, targetType))
                .findFirst();
    }

    public static RhiShaderModuleCreateInfo compile(
            RhiShaderModuleCreateInfo source,
            RhiShaderCodeType targetType,
            RhiShaderCompileOptions options
    ) {
        if (source.codeType() == targetType) {
            return source;
        }
        RhiShaderCompiler compiler = find(source.codeType(), targetType)
                .orElseThrow(() -> new RhiException(
                        "No shader compiler for " + source.codeType() + " to " + targetType
                ));
        return compiler.compile(source, targetType, options == null ? RhiShaderCompileOptions.defaults() : options);
    }
}
