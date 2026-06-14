package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.resource.RhiBufferUsage;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.rendering.RhiAttachmentLoadOp;
import com.github.slmpc.prismrhi.rendering.RhiAttachmentStoreOp;
import com.github.slmpc.prismrhi.rendering.RhiImageLayout;
import com.github.slmpc.prismrhi.resource.RhiFilter;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;
import com.github.slmpc.prismrhi.resource.RhiImageUsage;
import com.github.slmpc.prismrhi.resource.RhiMemoryUsage;
import com.github.slmpc.prismrhi.device.RhiPhysicalDeviceType;
import com.github.slmpc.prismrhi.resource.RhiSamplerAddressMode;

import java.util.Set;

import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_AUTO;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_AUTO_PREFER_HOST;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D24_UNORM_S8_UINT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R16G16B16A16_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_INDEX_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_INDIRECT_COMMAND_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_INPUT_ATTACHMENT_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_UNIFORM_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_STENCIL_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_GENERAL;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_STORAGE_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_LOAD;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_CPU;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_OTHER;
import static org.lwjgl.vulkan.VK10.VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;

final class VulkanSupport {
    private VulkanSupport() {
    }

    static int format(RhiFormat format) {
        return switch (format) {
            case UNDEFINED -> VK_FORMAT_UNDEFINED;
            case R8_UNORM -> VK_FORMAT_R8_UNORM;
            case RG8_UNORM -> VK_FORMAT_R8G8_UNORM;
            case RGB8_UNORM -> VK_FORMAT_R8G8B8_UNORM;
            case RGBA8_UNORM -> VK_FORMAT_R8G8B8A8_UNORM;
            case BGRA8_UNORM -> VK_FORMAT_B8G8R8A8_UNORM;
            case RGBA8_SRGB -> VK_FORMAT_R8G8B8A8_SRGB;
            case BGRA8_SRGB -> VK_FORMAT_B8G8R8A8_SRGB;
            case R32_FLOAT -> VK_FORMAT_R32_SFLOAT;
            case RG32_FLOAT -> VK_FORMAT_R32G32_SFLOAT;
            case RGB32_FLOAT -> VK_FORMAT_R32G32B32_SFLOAT;
            case RGBA16_FLOAT -> VK_FORMAT_R16G16B16A16_SFLOAT;
            case RGBA32_FLOAT -> VK_FORMAT_R32G32B32A32_SFLOAT;
            case D24_UNORM_S8_UINT -> VK_FORMAT_D24_UNORM_S8_UINT;
            case D32_FLOAT -> VK_FORMAT_D32_SFLOAT;
        };
    }

    static RhiFormat rhiFormat(int format) {
        return switch (format) {
            case VK_FORMAT_R8_UNORM -> RhiFormat.R8_UNORM;
            case VK_FORMAT_R8G8_UNORM -> RhiFormat.RG8_UNORM;
            case VK_FORMAT_R8G8B8_UNORM -> RhiFormat.RGB8_UNORM;
            case VK_FORMAT_R8G8B8A8_UNORM -> RhiFormat.RGBA8_UNORM;
            case VK_FORMAT_B8G8R8A8_UNORM -> RhiFormat.BGRA8_UNORM;
            case VK_FORMAT_R8G8B8A8_SRGB -> RhiFormat.RGBA8_SRGB;
            case VK_FORMAT_B8G8R8A8_SRGB -> RhiFormat.BGRA8_SRGB;
            case VK_FORMAT_R32_SFLOAT -> RhiFormat.R32_FLOAT;
            case VK_FORMAT_R32G32_SFLOAT -> RhiFormat.RG32_FLOAT;
            case VK_FORMAT_R32G32B32_SFLOAT -> RhiFormat.RGB32_FLOAT;
            case VK_FORMAT_R16G16B16A16_SFLOAT -> RhiFormat.RGBA16_FLOAT;
            case VK_FORMAT_R32G32B32A32_SFLOAT -> RhiFormat.RGBA32_FLOAT;
            case VK_FORMAT_D24_UNORM_S8_UINT -> RhiFormat.D24_UNORM_S8_UINT;
            case VK_FORMAT_D32_SFLOAT -> RhiFormat.D32_FLOAT;
            default -> throw new RhiException("Unsupported Vulkan format " + format);
        };
    }

    static int bufferUsage(Set<RhiBufferUsage> usages) {
        int flags = 0;
        for (RhiBufferUsage usage : usages) {
            flags |= switch (usage) {
                case TRANSFER_SRC -> VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
                case TRANSFER_DST -> VK_BUFFER_USAGE_TRANSFER_DST_BIT;
                case UNIFORM_BUFFER -> VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
                case STORAGE_BUFFER -> VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
                case INDEX_BUFFER -> VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
                case VERTEX_BUFFER -> VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
                case INDIRECT_BUFFER -> VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
            };
        }
        return flags == 0 ? VK_BUFFER_USAGE_TRANSFER_DST_BIT : flags;
    }

    static int imageUsage(Set<RhiImageUsage> usages) {
        int flags = 0;
        for (RhiImageUsage usage : usages) {
            flags |= switch (usage) {
                case TRANSFER_SRC -> VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
                case TRANSFER_DST -> VK_IMAGE_USAGE_TRANSFER_DST_BIT;
                case SAMPLED, PRESENT -> VK_IMAGE_USAGE_SAMPLED_BIT;
                case STORAGE -> VK_IMAGE_USAGE_STORAGE_BIT;
                case COLOR_ATTACHMENT -> VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                case DEPTH_STENCIL_ATTACHMENT -> VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
            };
        }
        return flags == 0 ? VK_IMAGE_USAGE_SAMPLED_BIT : flags;
    }

    static int imageAspect(Set<RhiImageAspect> aspects) {
        int flags = 0;
        for (RhiImageAspect aspect : aspects) {
            flags |= switch (aspect) {
                case COLOR -> VK_IMAGE_ASPECT_COLOR_BIT;
                case DEPTH -> VK_IMAGE_ASPECT_DEPTH_BIT;
                case STENCIL -> VK_IMAGE_ASPECT_STENCIL_BIT;
            };
        }
        return flags;
    }

    static int imageLayout(RhiImageLayout layout) {
        return switch (layout == null ? RhiImageLayout.GENERAL : layout) {
            case UNDEFINED -> VK_IMAGE_LAYOUT_UNDEFINED;
            case GENERAL -> VK_IMAGE_LAYOUT_GENERAL;
            case COLOR_ATTACHMENT -> VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
            case DEPTH_STENCIL_ATTACHMENT -> VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
            case SHADER_READ_ONLY -> VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            case PRESENT -> VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
        };
    }

    static int imageLayout(RhiResourceState state) {
        return switch (state == null ? RhiResourceState.UNDEFINED : state) {
            case UNDEFINED -> VK_IMAGE_LAYOUT_UNDEFINED;
            case PRESENT -> VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
            case TRANSFER_SRC -> VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
            case TRANSFER_DST -> VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
            case SAMPLED_IMAGE, DEPTH_STENCIL_READ -> VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            case STORAGE_IMAGE_READ, STORAGE_IMAGE_WRITE -> VK_IMAGE_LAYOUT_GENERAL;
            case COLOR_ATTACHMENT -> VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
            case DEPTH_STENCIL_ATTACHMENT -> VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
            case UNIFORM_BUFFER,
                    STORAGE_BUFFER_READ,
                    STORAGE_BUFFER_WRITE,
                    VERTEX_BUFFER,
                    INDEX_BUFFER,
                    INDIRECT_BUFFER -> VK_IMAGE_LAYOUT_UNDEFINED;
        };
    }

    static int accessMask(RhiResourceState state) {
        return switch (state == null ? RhiResourceState.UNDEFINED : state) {
            case UNDEFINED, PRESENT -> 0;
            case TRANSFER_SRC -> VK_ACCESS_TRANSFER_READ_BIT;
            case TRANSFER_DST -> VK_ACCESS_TRANSFER_WRITE_BIT;
            case SAMPLED_IMAGE, STORAGE_IMAGE_READ -> VK_ACCESS_SHADER_READ_BIT;
            case STORAGE_IMAGE_WRITE -> VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT;
            case COLOR_ATTACHMENT -> VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
            case DEPTH_STENCIL_ATTACHMENT ->
                    VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT;
            case DEPTH_STENCIL_READ -> VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_INPUT_ATTACHMENT_READ_BIT;
            case UNIFORM_BUFFER -> VK_ACCESS_UNIFORM_READ_BIT;
            case STORAGE_BUFFER_READ -> VK_ACCESS_SHADER_READ_BIT;
            case STORAGE_BUFFER_WRITE -> VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT;
            case VERTEX_BUFFER -> VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT;
            case INDEX_BUFFER -> VK_ACCESS_INDEX_READ_BIT;
            case INDIRECT_BUFFER -> VK_ACCESS_INDIRECT_COMMAND_READ_BIT;
        };
    }

    static int pipelineStage(RhiResourceState state) {
        return switch (state == null ? RhiResourceState.UNDEFINED : state) {
            case UNDEFINED -> VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            case PRESENT -> VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
            case TRANSFER_SRC, TRANSFER_DST -> VK_PIPELINE_STAGE_TRANSFER_BIT;
            case SAMPLED_IMAGE,
                    STORAGE_IMAGE_READ,
                    STORAGE_IMAGE_WRITE,
                    UNIFORM_BUFFER,
                    STORAGE_BUFFER_READ,
                    STORAGE_BUFFER_WRITE -> VK_PIPELINE_STAGE_VERTEX_SHADER_BIT | VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            case COLOR_ATTACHMENT -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            case DEPTH_STENCIL_ATTACHMENT, DEPTH_STENCIL_READ -> VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            case VERTEX_BUFFER, INDEX_BUFFER -> VK_PIPELINE_STAGE_VERTEX_INPUT_BIT;
            case INDIRECT_BUFFER -> VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT;
        };
    }

    static int loadOp(RhiAttachmentLoadOp loadOp) {
        return switch (loadOp == null ? RhiAttachmentLoadOp.LOAD : loadOp) {
            case LOAD -> VK_ATTACHMENT_LOAD_OP_LOAD;
            case CLEAR -> VK_ATTACHMENT_LOAD_OP_CLEAR;
            case DONT_CARE -> VK_ATTACHMENT_LOAD_OP_DONT_CARE;
        };
    }

    static int storeOp(RhiAttachmentStoreOp storeOp) {
        return switch (storeOp == null ? RhiAttachmentStoreOp.STORE : storeOp) {
            case STORE -> VK_ATTACHMENT_STORE_OP_STORE;
            case DONT_CARE -> VK_ATTACHMENT_STORE_OP_DONT_CARE;
        };
    }

    static int filter(RhiFilter filter) {
        return switch (filter == null ? RhiFilter.LINEAR : filter) {
            case NEAREST -> VK_FILTER_NEAREST;
            case LINEAR -> VK_FILTER_LINEAR;
        };
    }

    static int samplerAddressMode(RhiSamplerAddressMode mode) {
        return switch (mode == null ? RhiSamplerAddressMode.REPEAT : mode) {
            case REPEAT -> VK_SAMPLER_ADDRESS_MODE_REPEAT;
            case MIRRORED_REPEAT -> VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
            case CLAMP_TO_EDGE -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
            case CLAMP_TO_BORDER -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
        };
    }

    static int memoryUsage(RhiMemoryUsage usage) {
        return switch (usage) {
            case GPU_ONLY -> VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE;
            case CPU_TO_GPU -> VMA_MEMORY_USAGE_AUTO_PREFER_HOST;
            case GPU_TO_CPU -> VMA_MEMORY_USAGE_AUTO;
        };
    }

    static RhiPhysicalDeviceType physicalDeviceType(int vulkanType) {
        return switch (vulkanType) {
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> RhiPhysicalDeviceType.INTEGRATED_GPU;
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> RhiPhysicalDeviceType.DISCRETE_GPU;
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> RhiPhysicalDeviceType.VIRTUAL_GPU;
            case VK_PHYSICAL_DEVICE_TYPE_CPU -> RhiPhysicalDeviceType.CPU;
            case VK_PHYSICAL_DEVICE_TYPE_OTHER -> RhiPhysicalDeviceType.OTHER;
            default -> RhiPhysicalDeviceType.OTHER;
        };
    }

    static void check(int result, String operation) {
        if (result != org.lwjgl.vulkan.VK10.VK_SUCCESS) {
            throw new RhiException(operation + " failed with VkResult " + result);
        }
    }
}
