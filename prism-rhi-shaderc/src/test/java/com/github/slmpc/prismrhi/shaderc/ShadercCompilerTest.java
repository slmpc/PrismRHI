package com.github.slmpc.prismrhi.shaderc;

import com.github.slmpc.prismrhi.shader.RhiShaderCodeType;
import com.github.slmpc.prismrhi.shader.RhiShaderCompilers;
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShadercCompilerTest {
    @Test
    void compilesGlslToSpirv() {
        var spirv = RhiShaderCompilers.compile(
                RhiShaderModuleCreateInfo.glsl(
                        RhiShaderStage.VERTEX,
                        """
                        #version 450
                        void main() {
                            gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
                        }
                        """
                ),
                RhiShaderCodeType.SPIRV,
                null
        );

        assertEquals(RhiShaderCodeType.SPIRV, spirv.codeType());
        assertFalse(spirv.code().length == 0);
    }
}
