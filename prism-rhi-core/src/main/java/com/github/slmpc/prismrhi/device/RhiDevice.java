package com.github.slmpc.prismrhi.device;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.command.RhiCommandPool;
import com.github.slmpc.prismrhi.command.RhiCommandPoolCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetAllocateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiBufferCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.resource.RhiImageViewCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiSampler;
import com.github.slmpc.prismrhi.resource.RhiSamplerCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderModule;
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;

public interface RhiDevice extends AutoCloseable {
    BackendApi api();

    RhiQueue queue(RhiQueueType type);

    RhiBuffer createBuffer(RhiBufferCreateInfo createInfo);

    RhiImage createImage(RhiImageCreateInfo createInfo);

    RhiImageView createImageView(RhiImageViewCreateInfo createInfo);

    RhiSampler createSampler(RhiSamplerCreateInfo createInfo);

    RhiShaderModule createShaderModule(RhiShaderModuleCreateInfo createInfo);

    RhiCommandPool createCommandPool(RhiCommandPoolCreateInfo createInfo);

    RhiDescriptorSetLayout createDescriptorSetLayout(RhiDescriptorSetLayoutCreateInfo createInfo);

    RhiDescriptorSet allocateDescriptorSet(RhiDescriptorSetAllocateInfo allocateInfo);

    RhiGraphicsPipeline createGraphicsPipeline(RhiGraphicsPipelineCreateInfo createInfo);

    void waitIdle();

    @Override
    void close();
}
