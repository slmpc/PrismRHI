package com.github.slmpc.prismrhi.backend.opengl41;

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

final class Gl41Pipelines {
    private Gl41Pipelines() {
    }

    static RhiGraphicsPipeline create(RhiGraphicsPipelineCreateInfo createInfo) {
        int program = glCreateProgram();
        boolean hasVertex = false;
        boolean hasFragment = false;
        for (RhiPipelineShaderStage stage : createInfo.shaderStages()) {
            if (stage.module().api() != BackendApi.OPENGL_41) {
                throw new RhiException("OpenGL 4.1 graphics pipeline requires OpenGL 4.1 shader modules");
            }
            if (stage.stage() == RhiShaderStage.VERTEX) {
                hasVertex = true;
            } else if (stage.stage() == RhiShaderStage.FRAGMENT) {
                hasFragment = true;
            } else {
                throw new RhiException("OpenGL 4.1 graphics pipeline does not support compute shader stages");
            }
            glAttachShader(program, (int) stage.module().nativeHandle());
        }
        if (!hasVertex || !hasFragment) {
            throw new RhiException("OpenGL 4.1 graphics pipeline requires vertex and fragment shader stages");
        }
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RhiException(log);
        }
        return new Gl41GraphicsPipeline(program, createInfo);
    }

    record Gl41GraphicsPipeline(long nativeHandle, RhiGraphicsPipelineCreateInfo createInfo) implements RhiGraphicsPipeline {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
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
