package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.backend.RhiBackendProvider;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;
import org.lwjgl.vulkan.VK;

import java.util.Set;

public final class VulkanBackendProvider implements RhiBackendProvider {
    private static final BackendInfo INFO = new BackendInfo(
            BackendApi.VULKAN.id(),
            BackendApi.VULKAN,
            "Vulkan",
            "Explicit Vulkan backend using LWJGL Vulkan and VMA for memory allocation.",
            Set.of(
                    BackendFeature.EXPLICIT_MEMORY,
                    BackendFeature.EXPLICIT_SYNCHRONIZATION,
                    BackendFeature.SPIRV_SHADER_MODULES,
                    BackendFeature.COMMAND_BUFFERS,
                    BackendFeature.DESCRIPTORS,
                    BackendFeature.DESCRIPTOR_INDEXING,
                    BackendFeature.GRAPHICS_PIPELINES,
                    BackendFeature.DYNAMIC_RENDERING,
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
        return VK.getFunctionProvider() != null;
    }

    @Override
    public RhiInstance createInstance(RhiInstanceCreateInfo createInfo) {
        return VulkanInstance.create(INFO, createInfo);
    }
}
