package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.backend.RhiBackendProvider;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;

import java.util.Set;

public final class GlDsaBackendProvider implements RhiBackendProvider {
    private static final BackendInfo INFO = new BackendInfo(
            BackendApi.OPENGL_DSA.id(),
            BackendApi.OPENGL_DSA,
            "Modern OpenGL DSA",
            "OpenGL backend using 4.5 direct state access calls for lower binding churn.",
            Set.of(
                    BackendFeature.DIRECT_STATE_ACCESS,
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
        return GlDsaSupport.hasLwjglOpenGl();
    }

    @Override
    public RhiInstance createInstance(RhiInstanceCreateInfo createInfo) {
        return new GlDsaInstance(INFO);
    }
}
