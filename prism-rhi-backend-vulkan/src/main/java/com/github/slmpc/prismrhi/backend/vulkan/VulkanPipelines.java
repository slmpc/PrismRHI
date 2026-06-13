package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.command.RhiPrimitiveTopology;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.pipeline.RhiBlendFactor;
import com.github.slmpc.prismrhi.pipeline.RhiBlendOp;
import com.github.slmpc.prismrhi.pipeline.RhiColorBlendAttachmentState;
import com.github.slmpc.prismrhi.pipeline.RhiColorWriteMask;
import com.github.slmpc.prismrhi.pipeline.RhiCompareOp;
import com.github.slmpc.prismrhi.pipeline.RhiCullMode;
import com.github.slmpc.prismrhi.pipeline.RhiFrontFace;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiPipelineShaderStage;
import com.github.slmpc.prismrhi.pipeline.RhiPolygonMode;
import com.github.slmpc.prismrhi.pipeline.RhiVertexAttribute;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputBinding;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputRate;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_DST_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_DST_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ZERO;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_MAX;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_MIN;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_REVERSE_SUBTRACT;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_SUBTRACT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_EQUAL;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_GREATER;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_NEVER;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_NOT_EQUAL;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_BACK_BIT;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_FRONT_AND_BACK;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_FRONT_BIT;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE;
import static org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_SCISSOR;
import static org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_VIEWPORT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_LINE;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_POINT;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_INSTANCE;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;
import static org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO;

final class VulkanPipelines {
    private VulkanPipelines() {
    }

    static RhiGraphicsPipeline createGraphicsPipeline(VkDevice device, RhiGraphicsPipelineCreateInfo createInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pipelineLayout = createPipelineLayout(device, createInfo, stack);

            VkPipelineRenderingCreateInfo renderingCreateInfo = VkPipelineRenderingCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO)
                    .viewMask(createInfo.rendering().viewMask())
                    .depthAttachmentFormat(vkFormatOrUndefined(createInfo.rendering().depthAttachmentFormat()))
                    .stencilAttachmentFormat(vkFormatOrUndefined(createInfo.rendering().stencilAttachmentFormat()));
            if (!createInfo.rendering().colorAttachmentFormats().isEmpty()) {
                IntBuffer colorFormats = stack.mallocInt(createInfo.rendering().colorAttachmentFormats().size());
                for (var format : createInfo.rendering().colorAttachmentFormats()) {
                    colorFormats.put(VulkanSupport.format(format));
                }
                colorFormats.flip();
                renderingCreateInfo.pColorAttachmentFormats(colorFormats);
            } else {
                renderingCreateInfo.colorAttachmentCount(0);
            }

            VkPipelineShaderStageCreateInfo.Buffer shaderStages =
                    VkPipelineShaderStageCreateInfo.calloc(createInfo.shaderStages().size(), stack);
            for (int i = 0; i < createInfo.shaderStages().size(); i++) {
                RhiPipelineShaderStage stage = createInfo.shaderStages().get(i);
                if (stage.module().api() != BackendApi.VULKAN) {
                    throw new RhiException("Vulkan graphics pipeline requires Vulkan shader modules");
                }
                shaderStages.get(i)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                        .stage(shaderStage(stage.stage()))
                        .module(stage.module().nativeHandle())
                        .pName(stack.UTF8(stage.entryPoint()));
            }

            VkPipelineVertexInputStateCreateInfo vertexInputState = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            if (!createInfo.vertexInputBindings().isEmpty()) {
                vertexInputState.pVertexBindingDescriptions(vertexInputBindings(createInfo.vertexInputBindings(), stack));
            }
            if (!createInfo.vertexAttributes().isEmpty()) {
                vertexInputState.pVertexAttributeDescriptions(vertexAttributes(createInfo.vertexAttributes(), stack));
            }
            VkPipelineInputAssemblyStateCreateInfo inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(topology(createInfo.topology()))
                    .primitiveRestartEnable(false);
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .viewportCount(1)
                    .scissorCount(1);
            VkPipelineRasterizationStateCreateInfo rasterizationState = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(polygonMode(createInfo.rasterization().polygonMode()))
                    .cullMode(cullMode(createInfo.rasterization().cullMode()))
                    .frontFace(frontFace(createInfo.rasterization().frontFace()))
                    .depthBiasEnable(false)
                    .lineWidth(createInfo.rasterization().lineWidth());
            VkPipelineMultisampleStateCreateInfo multisampleState = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                    .sampleShadingEnable(false);
            VkPipelineDepthStencilStateCreateInfo depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(createInfo.depthStencil().depthTestEnable())
                    .depthWriteEnable(createInfo.depthStencil().depthWriteEnable())
                    .depthCompareOp(compareOp(createInfo.depthStencil().depthCompareOp()))
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(createInfo.depthStencil().stencilTestEnable());

            VkPipelineColorBlendAttachmentState.Buffer blendAttachments =
                    VkPipelineColorBlendAttachmentState.calloc(createInfo.colorBlendAttachments().size(), stack);
            for (int i = 0; i < createInfo.colorBlendAttachments().size(); i++) {
                RhiColorBlendAttachmentState blend = createInfo.colorBlendAttachments().get(i);
                blendAttachments.get(i)
                        .blendEnable(blend.blendEnable())
                        .srcColorBlendFactor(blendFactor(blend.srcColorBlendFactor()))
                        .dstColorBlendFactor(blendFactor(blend.dstColorBlendFactor()))
                        .colorBlendOp(blendOp(blend.colorBlendOp()))
                        .srcAlphaBlendFactor(blendFactor(blend.srcAlphaBlendFactor()))
                        .dstAlphaBlendFactor(blendFactor(blend.dstAlphaBlendFactor()))
                        .alphaBlendOp(blendOp(blend.alphaBlendOp()))
                        .colorWriteMask(colorWriteMask(blend.colorWriteMask()));
            }
            VkPipelineColorBlendStateCreateInfo colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .pAttachments(blendAttachments);
            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));

            VkGraphicsPipelineCreateInfo.Buffer pipelineCreateInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineCreateInfo.get(0)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pNext(renderingCreateInfo)
                    .pStages(shaderStages)
                    .pVertexInputState(vertexInputState)
                    .pInputAssemblyState(inputAssemblyState)
                    .pViewportState(viewportState)
                    .pRasterizationState(rasterizationState)
                    .pMultisampleState(multisampleState)
                    .pDepthStencilState(depthStencilState)
                    .pColorBlendState(colorBlendState)
                    .pDynamicState(dynamicState)
                    .layout(pipelineLayout)
                    .renderPass(NULL)
                    .subpass(0);

            LongBuffer pipelinePointer = stack.longs(0L);
            VulkanSupport.check(vkCreateGraphicsPipelines(device, NULL, pipelineCreateInfo, null, pipelinePointer), "vkCreateGraphicsPipelines");
            return new VulkanGraphicsPipeline(device, pipelinePointer.get(0), pipelineLayout);
        }
    }

    private static long createPipelineLayout(VkDevice device, RhiGraphicsPipelineCreateInfo createInfo, MemoryStack stack) {
        LongBuffer setLayouts = null;
        if (!createInfo.descriptorSetLayouts().isEmpty()) {
            setLayouts = stack.mallocLong(createInfo.descriptorSetLayouts().size());
            for (RhiDescriptorSetLayout layout : createInfo.descriptorSetLayouts()) {
                if (layout.api() != BackendApi.VULKAN) {
                    throw new RhiException("Vulkan graphics pipeline requires Vulkan descriptor set layouts");
                }
                setLayouts.put(layout.nativeHandle());
            }
            setLayouts.flip();
        }

        VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(setLayouts);
        LongBuffer layoutPointer = stack.longs(0L);
        VulkanSupport.check(vkCreatePipelineLayout(device, layoutCreateInfo, null, layoutPointer), "vkCreatePipelineLayout");
        return layoutPointer.get(0);
    }

    private static int shaderStage(RhiShaderStage stage) {
        return switch (stage) {
            case VERTEX -> VK_SHADER_STAGE_VERTEX_BIT;
            case FRAGMENT -> VK_SHADER_STAGE_FRAGMENT_BIT;
            case COMPUTE -> throw new RhiException("compute shader stage is not valid in a graphics pipeline");
        };
    }

    private static int topology(RhiPrimitiveTopology topology) {
        return switch (topology) {
            case POINT_LIST -> VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
            case LINE_LIST -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case LINE_STRIP -> VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
            case TRIANGLE_LIST -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
            case TRIANGLE_FAN -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
            case PATCH_LIST -> VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;
        };
    }

    private static VkVertexInputBindingDescription.Buffer vertexInputBindings(
            java.util.List<RhiVertexInputBinding> bindings,
            MemoryStack stack
    ) {
        VkVertexInputBindingDescription.Buffer descriptions = VkVertexInputBindingDescription.calloc(bindings.size(), stack);
        for (int i = 0; i < bindings.size(); i++) {
            RhiVertexInputBinding binding = bindings.get(i);
            descriptions.get(i)
                    .binding(binding.binding())
                    .stride(binding.stride())
                    .inputRate(vertexInputRate(binding.inputRate()));
        }
        return descriptions;
    }

    private static VkVertexInputAttributeDescription.Buffer vertexAttributes(
            java.util.List<RhiVertexAttribute> attributes,
            MemoryStack stack
    ) {
        VkVertexInputAttributeDescription.Buffer descriptions = VkVertexInputAttributeDescription.calloc(attributes.size(), stack);
        for (int i = 0; i < attributes.size(); i++) {
            RhiVertexAttribute attribute = attributes.get(i);
            descriptions.get(i)
                    .location(attribute.location())
                    .binding(attribute.binding())
                    .format(VulkanSupport.format(attribute.format()))
                    .offset(attribute.offset());
        }
        return descriptions;
    }

    private static int vertexInputRate(RhiVertexInputRate inputRate) {
        return switch (inputRate == null ? RhiVertexInputRate.VERTEX : inputRate) {
            case VERTEX -> VK_VERTEX_INPUT_RATE_VERTEX;
            case INSTANCE -> VK_VERTEX_INPUT_RATE_INSTANCE;
        };
    }

    private static int polygonMode(RhiPolygonMode mode) {
        return switch (mode) {
            case FILL -> VK_POLYGON_MODE_FILL;
            case LINE -> VK_POLYGON_MODE_LINE;
            case POINT -> VK_POLYGON_MODE_POINT;
        };
    }

    private static int cullMode(RhiCullMode mode) {
        return switch (mode) {
            case NONE -> VK_CULL_MODE_NONE;
            case FRONT -> VK_CULL_MODE_FRONT_BIT;
            case BACK -> VK_CULL_MODE_BACK_BIT;
            case FRONT_AND_BACK -> VK_CULL_MODE_FRONT_AND_BACK;
        };
    }

    private static int frontFace(RhiFrontFace face) {
        return switch (face) {
            case COUNTER_CLOCKWISE -> VK_FRONT_FACE_COUNTER_CLOCKWISE;
            case CLOCKWISE -> VK_FRONT_FACE_CLOCKWISE;
        };
    }

    private static int compareOp(RhiCompareOp compareOp) {
        return switch (compareOp) {
            case NEVER -> VK_COMPARE_OP_NEVER;
            case LESS -> VK_COMPARE_OP_LESS;
            case EQUAL -> VK_COMPARE_OP_EQUAL;
            case LESS_OR_EQUAL -> VK_COMPARE_OP_LESS_OR_EQUAL;
            case GREATER -> VK_COMPARE_OP_GREATER;
            case NOT_EQUAL -> VK_COMPARE_OP_NOT_EQUAL;
            case GREATER_OR_EQUAL -> VK_COMPARE_OP_GREATER_OR_EQUAL;
            case ALWAYS -> VK_COMPARE_OP_ALWAYS;
        };
    }

    private static int blendFactor(RhiBlendFactor factor) {
        return switch (factor) {
            case ZERO -> VK_BLEND_FACTOR_ZERO;
            case ONE -> VK_BLEND_FACTOR_ONE;
            case SRC_COLOR -> VK_BLEND_FACTOR_SRC_COLOR;
            case ONE_MINUS_SRC_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
            case DST_COLOR -> VK_BLEND_FACTOR_DST_COLOR;
            case ONE_MINUS_DST_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
            case SRC_ALPHA -> VK_BLEND_FACTOR_SRC_ALPHA;
            case ONE_MINUS_SRC_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            case DST_ALPHA -> VK_BLEND_FACTOR_DST_ALPHA;
            case ONE_MINUS_DST_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
        };
    }

    private static int blendOp(RhiBlendOp op) {
        return switch (op) {
            case ADD -> VK_BLEND_OP_ADD;
            case SUBTRACT -> VK_BLEND_OP_SUBTRACT;
            case REVERSE_SUBTRACT -> VK_BLEND_OP_REVERSE_SUBTRACT;
            case MIN -> VK_BLEND_OP_MIN;
            case MAX -> VK_BLEND_OP_MAX;
        };
    }

    private static int colorWriteMask(java.util.Set<RhiColorWriteMask> mask) {
        int flags = 0;
        for (RhiColorWriteMask component : mask) {
            flags |= switch (component) {
                case R -> VK_COLOR_COMPONENT_R_BIT;
                case G -> VK_COLOR_COMPONENT_G_BIT;
                case B -> VK_COLOR_COMPONENT_B_BIT;
                case A -> VK_COLOR_COMPONENT_A_BIT;
            };
        }
        return flags;
    }

    private static int vkFormatOrUndefined(com.github.slmpc.prismrhi.format.RhiFormat format) {
        return format == null ? VK_FORMAT_UNDEFINED : VulkanSupport.format(format);
    }

    private record VulkanGraphicsPipeline(VkDevice device, long nativeHandle, long nativeLayoutHandle)
            implements RhiGraphicsPipeline {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public void close() {
            vkDestroyPipeline(device, nativeHandle, null);
            vkDestroyPipelineLayout(device, nativeLayoutHandle, null);
        }
    }
}
