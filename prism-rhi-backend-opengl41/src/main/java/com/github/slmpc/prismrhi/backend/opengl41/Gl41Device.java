package com.github.slmpc.prismrhi.backend.opengl41;

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

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL33.glDeleteSamplers;
import static org.lwjgl.opengl.GL33.glGenSamplers;
import static org.lwjgl.opengl.GL33.glSamplerParameteri;

final class Gl41Device implements RhiDevice {
    private final Map<RhiQueueType, RhiQueue> queues = new EnumMap<>(RhiQueueType.class);
    private boolean closed;

    Gl41Device() {
        queues.put(RhiQueueType.GRAPHICS, new Gl41Queue(RhiQueueType.GRAPHICS));
        queues.put(RhiQueueType.TRANSFER, new Gl41Queue(RhiQueueType.TRANSFER));
    }

    @Override
    public BackendApi api() {
        return BackendApi.OPENGL_41;
    }

    @Override
    public RhiQueue queue(RhiQueueType type) {
        ensureOpen();
        RhiQueue queue = queues.get(type);
        if (queue == null) {
            throw new RhiException("OpenGL 4.1 backend does not expose a " + type + " queue");
        }
        return queue;
    }

    @Override
    public RhiBuffer createBuffer(RhiBufferCreateInfo createInfo) {
        ensureOpen();
        int handle = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, handle);
        glBufferData(GL_ARRAY_BUFFER, createInfo.size(), Gl41Support.bufferUsage(createInfo.memoryUsage()));
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return new Gl41Buffer(handle, createInfo.size());
    }

    @Override
    public RhiImage createImage(RhiImageCreateInfo createInfo) {
        ensureOpen();
        if (createInfo.extent().depth() != 1) {
            throw new RhiException("OpenGL 4.1 backend currently supports 2D images only");
        }
        Gl41Support.TextureFormat format = Gl41Support.textureFormat(createInfo.format());
        int handle = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, handle);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                format.internalFormat(),
                createInfo.extent().width(),
                createInfo.extent().height(),
                0,
                format.externalFormat(),
                format.type(),
                0L
        );
        Gl41Support.initializeTextureParameters();
        glBindTexture(GL_TEXTURE_2D, 0);
        return new Gl41Image(handle, createInfo.extent(), createInfo.format());
    }

    @Override
    public RhiImageView createImageView(RhiImageViewCreateInfo createInfo) {
        ensureOpen();
        if (createInfo.image().api() != BackendApi.OPENGL_41) {
            throw new RhiException("OpenGL 4.1 image view requires an OpenGL 4.1 image");
        }
        return new Gl41ImageView(createInfo.image(), createInfo.format(), createInfo.aspects());
    }

    @Override
    public RhiSampler createSampler(RhiSamplerCreateInfo createInfo) {
        ensureOpen();
        int handle = glGenSamplers();
        glSamplerParameteri(handle, GL_TEXTURE_MIN_FILTER, Gl41Support.filter(createInfo.minFilter()));
        glSamplerParameteri(handle, GL_TEXTURE_MAG_FILTER, Gl41Support.filter(createInfo.magFilter()));
        glSamplerParameteri(handle, GL_TEXTURE_WRAP_S, Gl41Support.addressMode(createInfo.addressModeU()));
        glSamplerParameteri(handle, GL_TEXTURE_WRAP_T, Gl41Support.addressMode(createInfo.addressModeV()));
        return new Gl41Sampler(handle);
    }

    @Override
    public RhiShaderModule createShaderModule(RhiShaderModuleCreateInfo createInfo) {
        ensureOpen();
        int shader = glCreateShader(Gl41Support.shaderType(createInfo.stage(), createInfo.codeType()));
        glShaderSource(shader, new String(createInfo.code(), StandardCharsets.UTF_8));
        glCompileShader(shader);
        Gl41Support.checkShaderCompile(shader);
        return new Gl41ShaderModule(shader, createInfo.stage(), createInfo.codeType());
    }

    @Override
    public RhiCommandPool createCommandPool(RhiCommandPoolCreateInfo createInfo) {
        ensureOpen();
        return new Gl41CommandPool(createInfo.queueType());
    }

    @Override
    public RhiDescriptorSetLayout createDescriptorSetLayout(RhiDescriptorSetLayoutCreateInfo createInfo) {
        ensureOpen();
        return Gl41Descriptors.layout(createInfo.bindings());
    }

    @Override
    public RhiDescriptorSet allocateDescriptorSet(RhiDescriptorSetAllocateInfo allocateInfo) {
        ensureOpen();
        return Gl41Descriptors.set(allocateInfo.layout());
    }

    @Override
    public RhiGraphicsPipeline createGraphicsPipeline(RhiGraphicsPipelineCreateInfo createInfo) {
        ensureOpen();
        return Gl41Pipelines.create(createInfo);
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
            throw new RhiException("OpenGL 4.1 device is closed");
        }
    }

    private record Gl41Buffer(int handle, long size) implements RhiBuffer {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
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

    private record Gl41Image(int handle, com.github.slmpc.prismrhi.format.RhiExtent3D extent,
                             com.github.slmpc.prismrhi.format.RhiFormat format) implements RhiImage {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
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

    private record Gl41ImageView(RhiImage image, com.github.slmpc.prismrhi.format.RhiFormat format,
                                 Set<RhiImageAspect> aspects) implements RhiImageView {
        Gl41ImageView {
            aspects = Set.copyOf(aspects);
        }

        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
        }

        @Override
        public long nativeHandle() {
            return image.nativeHandle();
        }

        @Override
        public void close() {
        }
    }

    private record Gl41Sampler(int handle) implements RhiSampler {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
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

    private record Gl41ShaderModule(int handle, com.github.slmpc.prismrhi.shader.RhiShaderStage stage,
                                    com.github.slmpc.prismrhi.shader.RhiShaderCodeType codeType) implements RhiShaderModule {
        @Override
        public BackendApi api() {
            return BackendApi.OPENGL_41;
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
