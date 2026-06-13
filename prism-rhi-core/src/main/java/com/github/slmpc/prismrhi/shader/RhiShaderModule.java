package com.github.slmpc.prismrhi.shader;

import com.github.slmpc.prismrhi.resource.RhiResource;

public interface RhiShaderModule extends RhiResource {
    RhiShaderStage stage();

    RhiShaderCodeType codeType();
}
