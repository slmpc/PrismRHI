package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiPipelineShaderStage;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;

import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glLinkProgram;

final class GlDsaPipelines {
    private GlDsaPipelines() {
    }

    static RhiGraphicsPipeline create(RhiGraphicsPipelineCreateInfo createInfo) {
        int program = glCreateProgram();
        boolean hasVertex = false;
        boolean hasFragment = false;
        for (RhiPipelineShaderStage stage : createInfo.shaderStages()) {
            if (stage.module().api() != BackendApi.OPENGL_DSA) {
                throw new RhiException("OpenGL DSA graphics pipeline requires OpenGL DSA shader modules");
            }
            if (stage.stage() == RhiShaderStage.VERTEX) {
                hasVertex = true;
            } else if (stage.stage() == RhiShaderStage.FRAGMENT) {
                hasFragment = true;
            } else {
                throw new RhiException("OpenGL DSA graphics pipeline does not support compute shader stages");
            }
            glAttachShader(program, (int) stage.module().nativeHandle());
        }
        if (!hasVertex || !hasFragment) {
            throw new RhiException("OpenGL DSA graphics pipeline requires vertex and fragment shader stages");
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RhiException(log);
        }
        return new GlDsaGraphicsPipeline(program, createInfo);
    }

    record GlDsaGraphicsPipeline(long nativeHandle, RhiGraphicsPipelineCreateInfo createInfo) implements RhiGraphicsPipeline {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
        }

        @Override
        public long nativeLayoutHandle() {
            return 0;
        }

        @Override
        public void close() {
            glDeleteProgram((int) nativeHandle);
        }
    }
}
