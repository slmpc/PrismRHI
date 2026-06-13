package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiBufferCreateInfo;
import com.github.slmpc.prismrhi.command.RhiCommandPool;
import com.github.slmpc.prismrhi.command.RhiCommandPoolCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetAllocateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.resource.RhiImageViewCreateInfo;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.resource.RhiSampler;
import com.github.slmpc.prismrhi.resource.RhiSamplerCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderModule;
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL33.glDeleteSamplers;
import static org.lwjgl.opengl.GL33.glGenSamplers;
import static org.lwjgl.opengl.GL33.glSamplerParameteri;
import static org.lwjgl.opengl.GL45.glCreateBuffers;
import static org.lwjgl.opengl.GL45.glCreateTextures;
import static org.lwjgl.opengl.GL45.glNamedBufferData;
import static org.lwjgl.opengl.GL45.glTextureStorage2D;

final class GlDsaDevice implements RhiDevice {
    private final Map<RhiQueueType, RhiQueue> queues = new EnumMap<>(RhiQueueType.class);
    private boolean closed;

    GlDsaDevice() {
        queues.put(RhiQueueType.GRAPHICS, new GlDsaQueue(RhiQueueType.GRAPHICS));
        queues.put(RhiQueueType.COMPUTE, new GlDsaQueue(RhiQueueType.COMPUTE));
        queues.put(RhiQueueType.TRANSFER, new GlDsaQueue(RhiQueueType.TRANSFER));
    }

    @Override
    public BackendApi api() {
        return BackendApi.OPENGL_DSA;
    }

    @Override
    public RhiQueue queue(RhiQueueType type) {
        ensureOpen();
        RhiQueue queue = queues.get(type);
        if (queue == null) {
            throw new RhiException("OpenGL DSA backend does not expose a " + type + " queue");
        }
        return queue;
    }

    @Override
    public RhiBuffer createBuffer(RhiBufferCreateInfo createInfo) {
        ensureOpen();
        int handle = glCreateBuffers();
        glNamedBufferData(handle, createInfo.size(), GlDsaSupport.bufferUsage(createInfo.memoryUsage()));
        return new GlDsaBuffer(handle, createInfo.size());
    }

    @Override
    public RhiImage createImage(RhiImageCreateInfo createInfo) {
        ensureOpen();
        if (createInfo.extent().depth() != 1) {
            throw new RhiException("OpenGL DSA backend currently supports 2D images only");
        }
        GlDsaSupport.TextureFormat format = GlDsaSupport.textureFormat(createInfo.format());
        int handle = glCreateTextures(GlDsaSupport.textureTarget());
        glTextureStorage2D(
                handle,
                1,
                format.internalFormat(),
                createInfo.extent().width(),
                createInfo.extent().height()
        );
        GlDsaSupport.initializeTextureParameters(handle);
        return new GlDsaImage(handle, createInfo.extent(), createInfo.format());
    }

    @Override
    public RhiImageView createImageView(RhiImageViewCreateInfo createInfo) {
        ensureOpen();
        if (createInfo.image().api() != BackendApi.OPENGL_DSA) {
            throw new RhiException("OpenGL DSA image view requires an OpenGL DSA image");
        }
        return new GlDsaImageView(createInfo.image(), createInfo.format(), createInfo.aspects());
    }

    @Override
    public RhiSampler createSampler(RhiSamplerCreateInfo createInfo) {
        ensureOpen();
        int handle = glGenSamplers();
        glSamplerParameteri(handle, GL_TEXTURE_MIN_FILTER, GlDsaSupport.filter(createInfo.minFilter()));
        glSamplerParameteri(handle, GL_TEXTURE_MAG_FILTER, GlDsaSupport.filter(createInfo.magFilter()));
        glSamplerParameteri(handle, GL_TEXTURE_WRAP_S, GlDsaSupport.addressMode(createInfo.addressModeU()));
        glSamplerParameteri(handle, GL_TEXTURE_WRAP_T, GlDsaSupport.addressMode(createInfo.addressModeV()));
        return new GlDsaSampler(handle);
    }

    @Override
    public RhiShaderModule createShaderModule(RhiShaderModuleCreateInfo createInfo) {
        ensureOpen();
        int shader = glCreateShader(GlDsaSupport.shaderType(createInfo.stage(), createInfo.codeType()));
        glShaderSource(shader, new String(createInfo.code(), StandardCharsets.UTF_8));
        glCompileShader(shader);
        GlDsaSupport.checkShaderCompile(shader);
        return new GlDsaShaderModule(shader, createInfo.stage(), createInfo.codeType());
    }

    @Override
    public RhiCommandPool createCommandPool(RhiCommandPoolCreateInfo createInfo) {
        ensureOpen();
        return new GlDsaCommandPool(createInfo.queueType());
    }

    @Override
    public RhiDescriptorSetLayout createDescriptorSetLayout(RhiDescriptorSetLayoutCreateInfo createInfo) {
        ensureOpen();
        return GlDsaDescriptors.layout(createInfo.bindings());
    }

    @Override
    public RhiDescriptorSet allocateDescriptorSet(RhiDescriptorSetAllocateInfo allocateInfo) {
        ensureOpen();
        return GlDsaDescriptors.set(allocateInfo.layout());
    }

    @Override
    public RhiGraphicsPipeline createGraphicsPipeline(RhiGraphicsPipelineCreateInfo createInfo) {
        ensureOpen();
        return GlDsaPipelines.create(createInfo);
    }

    @Override
    public void waitIdle() {
        ensureOpen();
        glFinish();
    }

    @Override
    public void close() {
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new RhiException("OpenGL DSA device is closed");
        }
    }

    private record GlDsaBuffer(int handle, long size) implements RhiBuffer {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public void close() {
            glDeleteBuffers(handle);
        }
    }

    private record GlDsaImage(int handle, com.github.slmpc.prismrhi.format.RhiExtent3D extent,
                              com.github.slmpc.prismrhi.format.RhiFormat format) implements RhiImage {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public void close() {
            glDeleteTextures(handle);
        }
    }

    private record GlDsaImageView(RhiImage image, com.github.slmpc.prismrhi.format.RhiFormat format,
                                  Set<RhiImageAspect> aspects) implements RhiImageView {
        GlDsaImageView {
            aspects = Set.copyOf(aspects);
        }

        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
        }

        @Override
        public long nativeHandle() {
            return image.nativeHandle();
        }

        @Override
        public void close() {
        }
    }

    private record GlDsaSampler(int handle) implements RhiSampler {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public void close() {
            glDeleteSamplers(handle);
        }
    }

    private record GlDsaShaderModule(int handle, com.github.slmpc.prismrhi.shader.RhiShaderStage stage,
                                     com.github.slmpc.prismrhi.shader.RhiShaderCodeType codeType) implements RhiShaderModule {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_DSA;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public void close() {
            glDeleteShader(handle);
        }
    }
}
