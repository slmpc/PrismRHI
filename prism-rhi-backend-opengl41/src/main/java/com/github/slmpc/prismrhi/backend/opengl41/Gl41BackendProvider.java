package com.github.slmpc.prismrhi.backend.opengl41;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.backend.RhiBackendProvider;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;

import java.util.Set;

public final class Gl41BackendProvider implements RhiBackendProvider {
    private static final BackendInfo INFO = new BackendInfo(
            BackendApi.OPENGL_41.id(),
            BackendApi.OPENGL_41,
            "OpenGL 4.1 Compatibility",
            "OpenGL backend using 4.1-compatible bind-to-edit calls for broad driver support.",
            Set.of(
                    BackendFeature.COMPATIBILITY_PROFILE,
                    BackendFeature.GLSL_SHADER_MODULES,
                    BackendFeature.COMMAND_BUFFERS,
                    BackendFeature.DESCRIPTORS,
                    BackendFeature.GRAPHICS_PIPELINES,
                    BackendFeature.FRAME_GRAPH,
                    BackendFeature.MULTI_DRAW,
                    BackendFeature.DEBUG_LABELS
            )
    );

    @Override
    public BackendInfo info() {
        return INFO;
    }

    @Override
    public boolean isSupported() {
        return Gl41Support.hasLwjglOpenGl();
    }

    @Override
    public RhiInstance createInstance(RhiInstanceCreateInfo createInfo) {
        return new Gl41Instance(INFO);
    }
}
