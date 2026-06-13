package com.github.slmpc.prismrhi.pipeline;

import com.github.slmpc.prismrhi.command.RhiPrimitiveTopology;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.shader.RhiShaderModule;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;

import java.util.ArrayList;
import java.util.List;

public record RhiGraphicsPipelineCreateInfo(
        List<RhiPipelineShaderStage> shaderStages,
        List<RhiDescriptorSetLayout> descriptorSetLayouts,
        RhiDynamicRenderingState rendering,
        List<RhiVertexInputBinding> vertexInputBindings,
        List<RhiVertexAttribute> vertexAttributes,
        RhiPrimitiveTopology topology,
        RhiRasterizationState rasterization,
        RhiDepthStencilState depthStencil,
        List<RhiColorBlendAttachmentState> colorBlendAttachments
) {
    public RhiGraphicsPipelineCreateInfo {
        shaderStages = List.copyOf(shaderStages == null ? List.of() : shaderStages);
        if (shaderStages.isEmpty()) {
            throw new IllegalArgumentException("graphics pipeline requires at least one shader stage");
        }
        descriptorSetLayouts = List.copyOf(descriptorSetLayouts == null ? List.of() : descriptorSetLayouts);
        rendering = rendering == null ? RhiDynamicRenderingState.color(RhiFormat.RGBA8_UNORM) : rendering;
        vertexInputBindings = List.copyOf(vertexInputBindings == null ? List.of() : vertexInputBindings);
        vertexAttributes = List.copyOf(vertexAttributes == null ? List.of() : vertexAttributes);
        topology = topology == null ? RhiPrimitiveTopology.TRIANGLE_LIST : topology;
        rasterization = rasterization == null ? RhiRasterizationState.defaults() : rasterization;
        depthStencil = depthStencil == null ? RhiDepthStencilState.disabled() : depthStencil;
        colorBlendAttachments = List.copyOf(colorBlendAttachments == null || colorBlendAttachments.isEmpty()
                ? defaultBlendAttachments(rendering.colorAttachmentFormats().size())
                : colorBlendAttachments);
        if (!rendering.colorAttachmentFormats().isEmpty()
                && colorBlendAttachments.size() != rendering.colorAttachmentFormats().size()) {
            throw new IllegalArgumentException("color blend attachment count must match dynamic color attachment formats");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static List<RhiColorBlendAttachmentState> defaultBlendAttachments(int count) {
        List<RhiColorBlendAttachmentState> states = new ArrayList<>();
        for (int i = 0; i < Math.max(1, count); i++) {
            states.add(RhiColorBlendAttachmentState.opaque());
        }
        return states;
    }

    public static final class Builder {
        private final List<RhiPipelineShaderStage> shaderStages = new ArrayList<>();
        private final List<RhiDescriptorSetLayout> descriptorSetLayouts = new ArrayList<>();
        private RhiDynamicRenderingState rendering;
        private final List<RhiVertexInputBinding> vertexInputBindings = new ArrayList<>();
        private final List<RhiVertexAttribute> vertexAttributes = new ArrayList<>();
        private RhiPrimitiveTopology topology = RhiPrimitiveTopology.TRIANGLE_LIST;
        private RhiRasterizationState rasterization = RhiRasterizationState.defaults();
        private RhiDepthStencilState depthStencil = RhiDepthStencilState.disabled();
        private final List<RhiColorBlendAttachmentState> colorBlendAttachments = new ArrayList<>();

        public Builder shader(RhiShaderStage stage, RhiShaderModule module) {
            shaderStages.add(new RhiPipelineShaderStage(stage, module, "main"));
            return this;
        }

        public Builder shader(RhiShaderStage stage, RhiShaderModule module, String entryPoint) {
            shaderStages.add(new RhiPipelineShaderStage(stage, module, entryPoint));
            return this;
        }

        public Builder descriptorSetLayout(RhiDescriptorSetLayout layout) {
            descriptorSetLayouts.add(layout);
            return this;
        }

        public Builder rendering(RhiDynamicRenderingState rendering) {
            this.rendering = rendering;
            return this;
        }

        public Builder colorFormat(RhiFormat colorFormat) {
            this.rendering = RhiDynamicRenderingState.color(colorFormat);
            return this;
        }

        public Builder vertexBinding(int binding, int stride) {
            vertexInputBindings.add(new RhiVertexInputBinding(binding, stride, RhiVertexInputRate.VERTEX));
            return this;
        }

        public Builder vertexBinding(int binding, int stride, RhiVertexInputRate inputRate) {
            vertexInputBindings.add(new RhiVertexInputBinding(binding, stride, inputRate));
            return this;
        }

        public Builder vertexAttribute(int location, int binding, RhiFormat format, int offset) {
            vertexAttributes.add(new RhiVertexAttribute(location, binding, format, offset));
            return this;
        }

        public Builder topology(RhiPrimitiveTopology topology) {
            this.topology = topology;
            return this;
        }

        public Builder rasterization(RhiRasterizationState rasterization) {
            this.rasterization = rasterization;
            return this;
        }

        public Builder depthStencil(RhiDepthStencilState depthStencil) {
            this.depthStencil = depthStencil;
            return this;
        }

        public Builder blend(RhiColorBlendAttachmentState attachmentState) {
            colorBlendAttachments.add(attachmentState);
            return this;
        }

        public RhiGraphicsPipelineCreateInfo build() {
            return new RhiGraphicsPipelineCreateInfo(
                    shaderStages,
                    descriptorSetLayouts,
                    rendering,
                    vertexInputBindings,
                    vertexAttributes,
                    topology,
                    rasterization,
                    depthStencil,
                    colorBlendAttachments
            );
        }
    }
}
