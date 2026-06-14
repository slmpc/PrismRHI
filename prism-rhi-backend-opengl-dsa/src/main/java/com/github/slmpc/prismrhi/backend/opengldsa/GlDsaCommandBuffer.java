package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.backend.RhiGlStateBridge;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiDrawCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndexedCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndexedIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndirectCommand;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.resource.RhiIndexType;
import com.github.slmpc.prismrhi.command.RhiMultiDrawCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedIndirectCountCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndirectCountCommand;
import com.github.slmpc.prismrhi.command.RhiPrimitiveTopology;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorType;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorWrite;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiColorBlendAttachmentState;
import com.github.slmpc.prismrhi.pipeline.RhiVertexAttribute;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputBinding;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputRate;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.rendering.RhiAttachmentLoadOp;
import com.github.slmpc.prismrhi.rendering.RhiClearValue;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiVertexBufferBinding;
import com.github.slmpc.prismrhi.rendering.RhiViewport;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageUploadInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.opengl.ARBIndirectParameters.GL_PARAMETER_BUFFER_ARB;
import static org.lwjgl.opengl.ARBIndirectParameters.glMultiDrawArraysIndirectCountARB;
import static org.lwjgl.opengl.ARBIndirectParameters.glMultiDrawElementsIndirectCountARB;
import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_DEPTH_FUNC;
import static org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_FRONT_FACE;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE_MODE;
import static org.lwjgl.opengl.GL11.GL_LINE_WIDTH;
import static org.lwjgl.opengl.GL11.GL_POLYGON_MODE;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ROW_LENGTH;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glGetBoolean;
import static org.lwjgl.opengl.GL11.glGetFloat;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA;
import static org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB;
import static org.lwjgl.opengl.GL14.glMultiDrawArrays;
import static org.lwjgl.opengl.GL14.glMultiDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA;
import static org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB;
import static org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.GL_VERTEX_ARRAY_BINDING;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glClearBufferfi;
import static org.lwjgl.opengl.GL30.glClearBufferfv;
import static org.lwjgl.opengl.GL30.glDrawBuffers;
import static org.lwjgl.opengl.GL30.glGetIntegeri;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER_BINDING;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER_SIZE;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER_START;
import static org.lwjgl.opengl.GL33.GL_SAMPLER_BINDING;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL42.glDrawArraysInstancedBaseInstance;
import static org.lwjgl.opengl.GL42.glDrawElementsInstancedBaseVertexBaseInstance;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER_BINDING;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER_SIZE;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER_START;
import static org.lwjgl.opengl.GL43.glMultiDrawArraysIndirect;
import static org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect;
import static org.lwjgl.opengl.GL45.glTextureSubImage2D;
import static org.lwjgl.opengl.GL46.GL_PARAMETER_BUFFER;
import static org.lwjgl.opengl.GL46.glMultiDrawArraysIndirectCount;
import static org.lwjgl.opengl.GL46.glMultiDrawElementsIndirectCount;

final class GlDsaCommandBuffer implements RhiCommandBuffer {
    private enum State {
        INITIAL,
        RECORDING,
        EXECUTABLE,
        CLOSED
    }

    private static final AtomicLong NEXT_HANDLE = new AtomicLong(1);

    private final long handle = NEXT_HANDLE.getAndIncrement();
    private final RhiCommandBufferLevel level;
    private final RhiQueueType queueType;
    private final RhiGlStateBridge gl;
    private final int vertexArray;
    private final List<Runnable> recordedCommands = new ArrayList<>();
    private RhiPrimitiveTopology primitiveTopology = RhiPrimitiveTopology.TRIANGLE_LIST;
    private RhiIndexType indexType = RhiIndexType.UINT32;
    private RhiGraphicsPipelineCreateInfo currentPipelineInfo;
    private int activeFramebuffer;
    private State state = State.INITIAL;

    GlDsaCommandBuffer(RhiCommandBufferLevel level, RhiQueueType queueType, RhiGlStateBridge glStateBridge) {
        this.level = level == null ? RhiCommandBufferLevel.PRIMARY : level;
        this.queueType = queueType;
        this.gl = glStateBridge == null ? GlDsaRawStateBridge.INSTANCE : glStateBridge;
        this.vertexArray = this.gl.genVertexArray();
    }

    @Override
    public BackendApi api() {
        return BackendApi.OPENGL_DSA;
    }

    @Override
    public long nativeHandle() {
        return handle;
    }

    @Override
    public RhiCommandBufferLevel level() {
        return level;
    }

    @Override
    public void begin() {
        if (state != State.INITIAL && state != State.EXECUTABLE) {
            throw new RhiException("OpenGL DSA command buffer cannot begin from state " + state);
        }
        recordedCommands.clear();
        state = State.RECORDING;
    }

    @Override
    public void end() {
        if (state != State.RECORDING) {
            throw new RhiException("OpenGL DSA command buffer is not recording");
        }
        state = State.EXECUTABLE;
    }

    void submit() {
        if (state != State.EXECUTABLE) {
            throw new RhiException("OpenGL DSA command buffer must be ended before submit");
        }
        GlStateSnapshot previousState = GlStateSnapshot.capture(gl);
        gl.bindVertexArray(vertexArray);
        try {
            recordedCommands.forEach(Runnable::run);
        } finally {
            previousState.restore(gl);
        }
    }

    @Override
    public void close() {
        if (state != State.CLOSED) {
            gl.deleteVertexArray(vertexArray);
        }
        state = State.CLOSED;
    }

    @Override
    public void setPrimitiveTopology(RhiPrimitiveTopology topology) {
        primitiveTopology = topology == null ? RhiPrimitiveTopology.TRIANGLE_LIST : topology;
    }

    @Override
    public void setIndexType(RhiIndexType indexType) {
        this.indexType = indexType == null ? RhiIndexType.UINT32 : indexType;
    }

    @Override
    public void beginRendering(RhiRenderingInfo renderingInfo) {
        ensureRecording();
        recordedCommands.add(() -> {
            if (activeFramebuffer != 0) {
                throw new RhiException("OpenGL DSA command buffer is already inside rendering");
            }
            int framebuffer = gl.genFramebuffer();
            gl.bindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            activeFramebuffer = framebuffer;
            attachRenderingTargets(renderingInfo);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new RhiException("OpenGL DSA framebuffer is not complete for dynamic rendering");
            }
            clearRenderingTargets(renderingInfo);
        });
    }

    @Override
    public void endRendering() {
        ensureRecording();
        recordedCommands.add(() -> {
            if (activeFramebuffer != 0) {
                gl.disable(GL_SCISSOR_TEST);
                gl.bindFramebuffer(GL_FRAMEBUFFER, 0);
                gl.deleteFramebuffer(activeFramebuffer);
                activeFramebuffer = 0;
            }
        });
    }

    @Override
    public void bindGraphicsPipeline(RhiGraphicsPipeline pipeline) {
        ensureRecording();
        if (!(pipeline instanceof GlDsaPipelines.GlDsaGraphicsPipeline glPipeline)) {
            throw new RhiException("OpenGL DSA bindGraphicsPipeline requires an OpenGL DSA pipeline");
        }
        currentPipelineInfo = glPipeline.createInfo();
        RhiGraphicsPipelineCreateInfo pipelineInfo = currentPipelineInfo;
        recordedCommands.add(() -> {
            applyPipelineState(pipelineInfo);
            gl.useProgram((int) glPipeline.nativeHandle());
        });
    }

    @Override
    public void bindDescriptorSets(RhiGraphicsPipeline pipeline, int firstSet, List<RhiDescriptorSet> descriptorSets) {
        ensureRecording();
        if (firstSet < 0) {
            throw new IllegalArgumentException("firstSet must not be negative");
        }
        List<RhiDescriptorSet> capturedSets = List.copyOf(descriptorSets);
        for (RhiDescriptorSet descriptorSet : capturedSets) {
            if (descriptorSet instanceof GlDsaDescriptors.GlDsaDescriptorSet glSet) {
                GlStateSnapshot.track(glSet);
            }
        }
        recordedCommands.add(() -> {
            for (RhiDescriptorSet descriptorSet : capturedSets) {
                applyDescriptorSet(descriptorSet);
            }
        });
    }

    @Override
    public void setViewport(RhiViewport viewport) {
        ensureRecording();
        recordedCommands.add(() -> gl.viewport(
                Math.round(viewport.x()),
                Math.round(viewport.y()),
                Math.round(viewport.width()),
                Math.round(viewport.height())
        ));
    }

    @Override
    public void setScissor(RhiRect2D scissor) {
        ensureRecording();
        recordedCommands.add(() -> {
            gl.enable(GL_SCISSOR_TEST);
            gl.scissor(
                    scissor.offset().x(),
                    scissor.offset().y(),
                    scissor.extent().width(),
                    scissor.extent().height()
            );
        });
    }

    @Override
    public void bindVertexBuffers(List<RhiVertexBufferBinding> bindings) {
        ensureRecording();
        if (currentPipelineInfo == null) {
            throw new RhiException("OpenGL DSA bindVertexBuffers requires a bound graphics pipeline");
        }
        RhiGraphicsPipelineCreateInfo pipelineInfo = currentPipelineInfo;
        List<RhiVertexBufferBinding> capturedBindings = bindings.stream()
                .sorted(Comparator.comparingInt(RhiVertexBufferBinding::binding))
                .toList();
        recordedCommands.add(() -> configureVertexBuffers(pipelineInfo, capturedBindings));
    }

    @Override
    public void bindIndexBuffer(RhiBuffer buffer, long offset, RhiIndexType indexType) {
        ensureRecording();
        requireBackendBuffer(buffer, "OpenGL DSA bindIndexBuffer");
        this.indexType = indexType == null ? RhiIndexType.UINT32 : indexType;
        recordedCommands.add(() -> gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, (int) buffer.nativeHandle()));
    }

    @Override
    public void copyBufferToImage(RhiBuffer source, RhiImage destination, RhiImageUploadInfo uploadInfo) {
        ensureRecording();
        requireBackendBuffer(source, "OpenGL DSA copyBufferToImage");
        requireBackendImage(destination, "OpenGL DSA copyBufferToImage");
        validateImageUpload(source, destination, uploadInfo, "OpenGL DSA copyBufferToImage");
        recordedCommands.add(() -> {
            GlDsaSupport.TextureFormat format = GlDsaSupport.textureFormat(destination.format());
            gl.bindBuffer(GL_PIXEL_UNPACK_BUFFER, (int) source.nativeHandle());
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, uploadInfo.bufferRowLength(destination.format()));
            glTextureSubImage2D(
                    (int) destination.nativeHandle(),
                    uploadInfo.mipLevel(),
                    uploadInfo.x(),
                    uploadInfo.y(),
                    uploadInfo.width(),
                    uploadInfo.height(),
                    format.externalFormat(),
                    format.type(),
                    uploadInfo.bufferOffset()
            );
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            gl.bindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
        });
    }

    @Override
    public void draw(RhiDrawCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        recordedCommands.add(() -> glDrawArraysInstancedBaseInstance(
                GlDsaSupport.primitiveTopology(topology),
                command.firstVertex(),
                command.vertexCount(),
                command.instanceCount(),
                command.firstInstance()
        ));
    }

    @Override
    public void drawIndexed(RhiDrawIndexedCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> glDrawElementsInstancedBaseVertexBaseInstance(
                GlDsaSupport.primitiveTopology(topology),
                command.indexCount(),
                GlDsaSupport.indexType(capturedIndexType),
                (long) command.firstIndex() * capturedIndexType.bytes(),
                command.instanceCount(),
                command.vertexOffset(),
                command.firstInstance()
        ));
    }

    @Override
    public void multiDraw(RhiMultiDrawCommand command) {
        ensureRecording();
        if (command.drawCount() == 0) {
            return;
        }
        requireZeroFirstInstance(command.firstInstance(), "OpenGL DSA multiDraw");
        RhiPrimitiveTopology topology = primitiveTopology;
        recordedCommands.add(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer first = stack.mallocInt(command.drawCount());
                IntBuffer count = stack.mallocInt(command.drawCount());
                for (RhiDrawCommand drawCommand : command.commands()) {
                    first.put(drawCommand.firstVertex());
                    count.put(drawCommand.vertexCount());
                }
                first.flip();
                count.flip();
                glMultiDrawArrays(GlDsaSupport.primitiveTopology(topology), first, count);
            }
        });
    }

    @Override
    public void multiDrawIndexed(RhiMultiDrawIndexedCommand command) {
        ensureRecording();
        if (command.drawCount() == 0) {
            return;
        }
        requireZeroFirstInstance(command.firstInstance(), "OpenGL DSA multiDrawIndexed");
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer count = stack.mallocInt(command.drawCount());
                PointerBuffer indices = stack.mallocPointer(command.drawCount());
                for (RhiDrawIndexedCommand drawCommand : command.commands()) {
                    if (drawCommand.vertexOffset() != 0) {
                        throw new RhiException("OpenGL DSA multiDrawIndexed does not support vertexOffset");
                    }
                    count.put(drawCommand.indexCount());
                    indices.put((long) drawCommand.firstIndex() * capturedIndexType.bytes());
                }
                count.flip();
                indices.flip();
                glMultiDrawElements(
                        GlDsaSupport.primitiveTopology(topology),
                        count,
                        GlDsaSupport.indexType(capturedIndexType),
                        indices
                );
            }
        });
    }

    @Override
    public void drawIndirect(RhiDrawIndirectCommand command) {
        multiDrawIndirect(new RhiMultiDrawIndirectCommand(command.buffer(), command.offset(), 1, RhiMultiDrawIndirectCommand.PACKED_STRIDE));
    }

    @Override
    public void drawIndexedIndirect(RhiDrawIndexedIndirectCommand command) {
        multiDrawIndexedIndirect(new RhiMultiDrawIndexedIndirectCommand(
                command.buffer(),
                command.offset(),
                1,
                RhiMultiDrawIndexedIndirectCommand.PACKED_STRIDE
        ));
    }

    @Override
    public void multiDrawIndirect(RhiMultiDrawIndirectCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        recordedCommands.add(() -> {
            requireBackendBuffer(command.buffer(), "OpenGL DSA multiDrawIndirect");
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            glMultiDrawArraysIndirect(
                    GlDsaSupport.primitiveTopology(topology),
                    command.offset(),
                    command.drawCount(),
                    command.stride()
            );
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        });
    }

    @Override
    public void multiDrawIndexedIndirect(RhiMultiDrawIndexedIndirectCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> {
            requireBackendBuffer(command.buffer(), "OpenGL DSA multiDrawIndexedIndirect");
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            glMultiDrawElementsIndirect(
                    GlDsaSupport.primitiveTopology(topology),
                    GlDsaSupport.indexType(capturedIndexType),
                    command.offset(),
                    command.drawCount(),
                    command.stride()
            );
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        });
    }

    @Override
    public void multiDrawIndirectCount(RhiMultiDrawIndirectCountCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        recordedCommands.add(() -> {
            requireIndirectCountSupport();
            requireBackendBuffer(command.buffer(), "OpenGL DSA multiDrawIndirectCount");
            requireBackendBuffer(command.countBuffer(), "OpenGL DSA multiDrawIndirectCount");
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            if (GL.getCapabilities().OpenGL46) {
                gl.bindBuffer(GL_PARAMETER_BUFFER, (int) command.countBuffer().nativeHandle());
                glMultiDrawArraysIndirectCount(
                        GlDsaSupport.primitiveTopology(topology),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                gl.bindBuffer(GL_PARAMETER_BUFFER, 0);
            } else {
                gl.bindBuffer(GL_PARAMETER_BUFFER_ARB, (int) command.countBuffer().nativeHandle());
                glMultiDrawArraysIndirectCountARB(
                        GlDsaSupport.primitiveTopology(topology),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                gl.bindBuffer(GL_PARAMETER_BUFFER_ARB, 0);
            }
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        });
    }

    @Override
    public void multiDrawIndexedIndirectCount(RhiMultiDrawIndexedIndirectCountCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> {
            requireIndirectCountSupport();
            requireBackendBuffer(command.buffer(), "OpenGL DSA multiDrawIndexedIndirectCount");
            requireBackendBuffer(command.countBuffer(), "OpenGL DSA multiDrawIndexedIndirectCount");
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            if (GL.getCapabilities().OpenGL46) {
                gl.bindBuffer(GL_PARAMETER_BUFFER, (int) command.countBuffer().nativeHandle());
                glMultiDrawElementsIndirectCount(
                        GlDsaSupport.primitiveTopology(topology),
                        GlDsaSupport.indexType(capturedIndexType),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                gl.bindBuffer(GL_PARAMETER_BUFFER, 0);
            } else {
                gl.bindBuffer(GL_PARAMETER_BUFFER_ARB, (int) command.countBuffer().nativeHandle());
                glMultiDrawElementsIndirectCountARB(
                        GlDsaSupport.primitiveTopology(topology),
                        GlDsaSupport.indexType(capturedIndexType),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                gl.bindBuffer(GL_PARAMETER_BUFFER_ARB, 0);
            }
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        });
    }

    RhiQueueType queueType() {
        return queueType;
    }

    private void ensureRecording() {
        if (state != State.RECORDING) {
            throw new RhiException("OpenGL DSA command buffer must be recording");
        }
    }

    private void requireBackendBuffer(com.github.slmpc.prismrhi.resource.RhiBuffer buffer, String operation) {
        if (buffer.api() != BackendApi.OPENGL_DSA) {
            throw new RhiException(operation + " requires an OpenGL DSA buffer");
        }
    }

    private void requireBackendImage(RhiImage image, String operation) {
        if (image.api() != BackendApi.OPENGL_DSA) {
            throw new RhiException(operation + " requires an OpenGL DSA image");
        }
    }

    private static void validateImageUpload(
            RhiBuffer source,
            RhiImage destination,
            RhiImageUploadInfo uploadInfo,
            String operation
    ) {
        uploadInfo.validateFor(destination);
        if (uploadInfo.z() != 0 || uploadInfo.depth() != 1) {
            throw new RhiException(operation + " currently supports 2D image regions only");
        }
        if (uploadInfo.mipLevel() != 0 || uploadInfo.arrayLayer() != 0 || uploadInfo.layerCount() != 1) {
            throw new RhiException(operation + " currently supports mip level 0 and array layer 0 only");
        }
        if (source.size() < uploadInfo.requiredBytes(destination.format())) {
            throw new IllegalArgumentException("source buffer is too small for image upload");
        }
    }

    private void requireZeroFirstInstance(int firstInstance, String operation) {
        if (firstInstance != 0) {
            throw new RhiException(operation + " does not support firstInstance");
        }
    }

    private void requireIndirectCountSupport() {
        if (!GlDsaSupport.supportsIndirectCount()) {
            throw new RhiException("OpenGL DSA indirect count draws require OpenGL 4.6 or GL_ARB_indirect_parameters");
        }
    }

    private void applyPipelineState(RhiGraphicsPipelineCreateInfo pipelineInfo) {
        var raster = pipelineInfo.rasterization();
        gl.polygonMode(GL_FRONT_AND_BACK, GlDsaSupport.polygonMode(raster.polygonMode()));
        gl.frontFace(GlDsaSupport.frontFace(raster.frontFace()));
        gl.lineWidth(raster.lineWidth());

        int cullMode = GlDsaSupport.cullMode(raster.cullMode());
        if (cullMode == 0) {
            gl.disable(GL_CULL_FACE);
        } else {
            gl.enable(GL_CULL_FACE);
            gl.cullFace(cullMode);
        }

        var depthStencil = pipelineInfo.depthStencil();
        if (depthStencil.depthTestEnable()) {
            gl.enable(GL_DEPTH_TEST);
            gl.depthFunc(GlDsaSupport.compareOp(depthStencil.depthCompareOp()));
        } else {
            gl.disable(GL_DEPTH_TEST);
        }
        gl.depthMask(depthStencil.depthWriteEnable());

        if (depthStencil.stencilTestEnable()) {
            gl.enable(GL_STENCIL_TEST);
        } else {
            gl.disable(GL_STENCIL_TEST);
        }

        if (pipelineInfo.colorBlendAttachments().stream().anyMatch(RhiColorBlendAttachmentState::blendEnable)) {
            RhiColorBlendAttachmentState blend = pipelineInfo.colorBlendAttachments().get(0);
            gl.enable(GL_BLEND);
            gl.blendFuncSeparate(
                    GlDsaSupport.blendFactor(blend.srcColorBlendFactor()),
                    GlDsaSupport.blendFactor(blend.dstColorBlendFactor()),
                    GlDsaSupport.blendFactor(blend.srcAlphaBlendFactor()),
                    GlDsaSupport.blendFactor(blend.dstAlphaBlendFactor())
            );
            gl.blendEquationSeparate(
                    GlDsaSupport.blendOp(blend.colorBlendOp()),
                    GlDsaSupport.blendOp(blend.alphaBlendOp())
            );
        } else {
            gl.disable(GL_BLEND);
        }
    }

    private void attachRenderingTargets(RhiRenderingInfo renderingInfo) {
        int colorCount = renderingInfo.colorAttachments().size();
        if (colorCount > 0) {
            int[] drawBuffers = new int[colorCount];
            for (int i = 0; i < colorCount; i++) {
                RhiRenderingAttachment attachment = renderingInfo.colorAttachments().get(i);
                requireImageView(attachment, "OpenGL DSA color attachment");
                int attachmentPoint = GlDsaSupport.colorAttachment(i);
                gl.framebufferTexture2D(
                        GL_FRAMEBUFFER,
                        attachmentPoint,
                        GL_TEXTURE_2D,
                        (int) attachment.view().nativeHandle(),
                        0
                );
                drawBuffers[i] = attachmentPoint;
            }
            glDrawBuffers(drawBuffers);
        }
        if (renderingInfo.depthAttachment() != null) {
            RhiRenderingAttachment attachment = renderingInfo.depthAttachment();
            requireImageView(attachment, "OpenGL DSA depth attachment");
            gl.framebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GlDsaSupport.depthStencilAttachment(attachment.view().format()),
                    GL_TEXTURE_2D,
                    (int) attachment.view().nativeHandle(),
                    0
            );
        }
    }

    private void clearRenderingTargets(RhiRenderingInfo renderingInfo) {
        for (int i = 0; i < renderingInfo.colorAttachments().size(); i++) {
            RhiRenderingAttachment attachment = renderingInfo.colorAttachments().get(i);
            if (attachment.loadOp() == RhiAttachmentLoadOp.CLEAR) {
                RhiClearValue clear = attachment.clearValue();
                glClearBufferfv(GL_COLOR, i, new float[]{clear.r(), clear.g(), clear.b(), clear.a()});
            }
        }
        if (renderingInfo.depthAttachment() != null && renderingInfo.depthAttachment().loadOp() == RhiAttachmentLoadOp.CLEAR) {
            RhiRenderingAttachment attachment = renderingInfo.depthAttachment();
            RhiClearValue clear = attachment.clearValue();
            if (GlDsaSupport.stencilFormat(attachment.view().format())) {
                glClearBufferfi(GL_DEPTH_STENCIL, 0, clear.depth(), clear.stencil());
            } else {
                glClearBufferfv(GL_DEPTH, 0, new float[]{clear.depth()});
            }
        }
    }

    private void configureVertexBuffers(
            RhiGraphicsPipelineCreateInfo pipelineInfo,
            List<RhiVertexBufferBinding> vertexBufferBindings
    ) {
        for (RhiVertexBufferBinding bufferBinding : vertexBufferBindings) {
            requireBackendBuffer(bufferBinding.buffer(), "OpenGL DSA bindVertexBuffers");
            gl.bindBuffer(GL_ARRAY_BUFFER, (int) bufferBinding.buffer().nativeHandle());
            RhiVertexInputBinding inputBinding = pipelineInfo.vertexInputBindings().stream()
                    .filter(binding -> binding.binding() == bufferBinding.binding())
                    .findFirst()
                    .orElse(new RhiVertexInputBinding(bufferBinding.binding(), 0, RhiVertexInputRate.VERTEX));
            for (RhiVertexAttribute attribute : pipelineInfo.vertexAttributes()) {
                if (attribute.binding() == bufferBinding.binding()) {
                    GlDsaSupport.VertexAttributeFormat format = GlDsaSupport.vertexAttributeFormat(attribute.format());
                    gl.enableVertexAttribArray(attribute.location());
                    gl.vertexAttribPointer(
                            attribute.location(),
                            format.components(),
                            format.type(),
                            format.normalized(),
                            inputBinding.stride(),
                            bufferBinding.offset() + attribute.offset()
                    );
                    gl.vertexAttribDivisor(attribute.location(), inputBinding.inputRate() == RhiVertexInputRate.INSTANCE ? 1 : 0);
                }
            }
        }
    }

    private void applyDescriptorSet(RhiDescriptorSet descriptorSet) {
        if (!(descriptorSet instanceof GlDsaDescriptors.GlDsaDescriptorSet glSet)) {
            throw new RhiException("OpenGL DSA bindDescriptorSets requires OpenGL DSA descriptor sets");
        }
        for (RhiDescriptorWrite write : glSet.writes()) {
            if (write.type() == RhiDescriptorType.UNIFORM_BUFFER || write.type() == RhiDescriptorType.UNIFORM_BUFFER_DYNAMIC) {
                requireBackendBuffer(write.buffer(), "OpenGL DSA uniform buffer descriptor");
                long range = write.range() == 0 ? write.buffer().size() : write.range();
                gl.bindBufferRange(GL_UNIFORM_BUFFER, write.binding(), (int) write.buffer().nativeHandle(), write.offset(), range);
            } else if (write.type() == RhiDescriptorType.STORAGE_BUFFER || write.type() == RhiDescriptorType.STORAGE_BUFFER_DYNAMIC) {
                requireBackendBuffer(write.buffer(), "OpenGL DSA storage buffer descriptor");
                long range = write.range() == 0 ? write.buffer().size() : write.range();
                gl.bindBufferRange(GL_SHADER_STORAGE_BUFFER, write.binding(), (int) write.buffer().nativeHandle(), write.offset(), range);
            } else if (write.type() == RhiDescriptorType.SAMPLER) {
                requireSampler(write);
                gl.bindSampler(write.binding(), (int) write.sampler().nativeHandle());
            } else if (write.type() == RhiDescriptorType.SAMPLED_IMAGE
                    || write.type() == RhiDescriptorType.COMBINED_IMAGE_SAMPLER
                    || write.type() == RhiDescriptorType.INPUT_ATTACHMENT) {
                requireImageDescriptor(write);
                gl.activeTexture(GL_TEXTURE0 + write.binding());
                gl.bindTexture(GL_TEXTURE_2D, (int) write.imageView().nativeHandle());
                if (write.sampler() != null) {
                    requireSampler(write);
                    gl.bindSampler(write.binding(), (int) write.sampler().nativeHandle());
                }
            } else {
                throw new RhiException("OpenGL DSA backend does not support " + write.type() + " descriptors");
            }
        }
    }

    private void requireImageView(RhiRenderingAttachment attachment, String operation) {
        if (attachment.view().api() != BackendApi.OPENGL_DSA) {
            throw new RhiException(operation + " requires an OpenGL DSA image view");
        }
    }

    private void requireImageDescriptor(RhiDescriptorWrite write) {
        if (write.imageView() == null || write.imageView().api() != BackendApi.OPENGL_DSA) {
            throw new RhiException("OpenGL DSA image descriptor requires an OpenGL DSA image view");
        }
    }

    private void requireSampler(RhiDescriptorWrite write) {
        if (write.sampler() == null || write.sampler().api() != BackendApi.OPENGL_DSA) {
            throw new RhiException("OpenGL DSA sampler descriptor requires an OpenGL DSA sampler");
        }
    }

    private record TextureUnitState(int texture, int sampler) {
    }

    private record BufferBindingState(int buffer, long offset, long size) {
    }

    private static final class GlStateSnapshot {
        private static final Map<Integer, Integer> TOUCHED_TEXTURE_UNITS = new LinkedHashMap<>();
        private static final Map<Integer, Integer> TOUCHED_UNIFORM_BINDINGS = new LinkedHashMap<>();
        private static final Map<Integer, Integer> TOUCHED_STORAGE_BINDINGS = new LinkedHashMap<>();

        private final int program;
        private final int activeTexture;
        private final int arrayBuffer;
        private final int elementArrayBuffer;
        private final int pixelUnpackBuffer;
        private final int drawIndirectBuffer;
        private final int parameterBuffer;
        private final int parameterBufferArb;
        private final int uniformBuffer;
        private final int storageBuffer;
        private final int readFramebuffer;
        private final int drawFramebuffer;
        private final int vertexArray;
        private final boolean blend;
        private final int blendSrcRgb;
        private final int blendDstRgb;
        private final int blendSrcAlpha;
        private final int blendDstAlpha;
        private final int blendEquationRgb;
        private final int blendEquationAlpha;
        private final boolean depthTest;
        private final int depthFunc;
        private final boolean depthMask;
        private final boolean cull;
        private final int cullFace;
        private final int frontFace;
        private final boolean scissor;
        private final boolean stencil;
        private final int polygonMode;
        private final float lineWidth;
        private final Map<Integer, TextureUnitState> textureUnits;
        private final Map<Integer, BufferBindingState> uniformBindings;
        private final Map<Integer, BufferBindingState> storageBindings;

        private GlStateSnapshot(
                int program,
                int activeTexture,
                int arrayBuffer,
                int elementArrayBuffer,
                int pixelUnpackBuffer,
                int drawIndirectBuffer,
                int parameterBuffer,
                int parameterBufferArb,
                int uniformBuffer,
                int storageBuffer,
                int readFramebuffer,
                int drawFramebuffer,
                int vertexArray,
                boolean blend,
                int blendSrcRgb,
                int blendDstRgb,
                int blendSrcAlpha,
                int blendDstAlpha,
                int blendEquationRgb,
                int blendEquationAlpha,
                boolean depthTest,
                int depthFunc,
                boolean depthMask,
                boolean cull,
                int cullFace,
                int frontFace,
                boolean scissor,
                boolean stencil,
                int polygonMode,
                float lineWidth,
                Map<Integer, TextureUnitState> textureUnits,
                Map<Integer, BufferBindingState> uniformBindings,
                Map<Integer, BufferBindingState> storageBindings
        ) {
            this.program = program;
            this.activeTexture = activeTexture;
            this.arrayBuffer = arrayBuffer;
            this.elementArrayBuffer = elementArrayBuffer;
            this.pixelUnpackBuffer = pixelUnpackBuffer;
            this.drawIndirectBuffer = drawIndirectBuffer;
            this.parameterBuffer = parameterBuffer;
            this.parameterBufferArb = parameterBufferArb;
            this.uniformBuffer = uniformBuffer;
            this.storageBuffer = storageBuffer;
            this.readFramebuffer = readFramebuffer;
            this.drawFramebuffer = drawFramebuffer;
            this.vertexArray = vertexArray;
            this.blend = blend;
            this.blendSrcRgb = blendSrcRgb;
            this.blendDstRgb = blendDstRgb;
            this.blendSrcAlpha = blendSrcAlpha;
            this.blendDstAlpha = blendDstAlpha;
            this.blendEquationRgb = blendEquationRgb;
            this.blendEquationAlpha = blendEquationAlpha;
            this.depthTest = depthTest;
            this.depthFunc = depthFunc;
            this.depthMask = depthMask;
            this.cull = cull;
            this.cullFace = cullFace;
            this.frontFace = frontFace;
            this.scissor = scissor;
            this.stencil = stencil;
            this.polygonMode = polygonMode;
            this.lineWidth = lineWidth;
            this.textureUnits = textureUnits;
            this.uniformBindings = uniformBindings;
            this.storageBindings = storageBindings;
        }

        private static void track(GlDsaDescriptors.GlDsaDescriptorSet descriptorSet) {
            for (RhiDescriptorWrite write : descriptorSet.writes()) {
                if (write.type() == RhiDescriptorType.UNIFORM_BUFFER || write.type() == RhiDescriptorType.UNIFORM_BUFFER_DYNAMIC) {
                    TOUCHED_UNIFORM_BINDINGS.putIfAbsent(write.binding(), 0);
                } else if (write.type() == RhiDescriptorType.STORAGE_BUFFER || write.type() == RhiDescriptorType.STORAGE_BUFFER_DYNAMIC) {
                    TOUCHED_STORAGE_BINDINGS.putIfAbsent(write.binding(), 0);
                } else if (write.type() == RhiDescriptorType.SAMPLER
                        || write.type() == RhiDescriptorType.SAMPLED_IMAGE
                        || write.type() == RhiDescriptorType.COMBINED_IMAGE_SAMPLER
                        || write.type() == RhiDescriptorType.INPUT_ATTACHMENT) {
                    TOUCHED_TEXTURE_UNITS.putIfAbsent(write.binding(), 0);
                }
            }
        }

        private static GlStateSnapshot capture(RhiGlStateBridge gl) {
            Map<Integer, TextureUnitState> textures = new LinkedHashMap<>();
            int activeTexture = glGetInteger(GL_ACTIVE_TEXTURE);
            for (int unit : TOUCHED_TEXTURE_UNITS.keySet()) {
                gl.activeTexture(GL_TEXTURE0 + unit);
                textures.put(unit, new TextureUnitState(glGetInteger(GL_TEXTURE_BINDING_2D), glGetInteger(GL_SAMPLER_BINDING)));
            }
            gl.activeTexture(activeTexture);

            Map<Integer, BufferBindingState> uniformBindings = new LinkedHashMap<>();
            for (int binding : TOUCHED_UNIFORM_BINDINGS.keySet()) {
                uniformBindings.put(binding, new BufferBindingState(
                        glGetIntegeri(GL_UNIFORM_BUFFER_BINDING, binding),
                        glGetIntegeri(GL_UNIFORM_BUFFER_START, binding),
                        glGetIntegeri(GL_UNIFORM_BUFFER_SIZE, binding)
                ));
            }

            Map<Integer, BufferBindingState> storageBindings = new LinkedHashMap<>();
            for (int binding : TOUCHED_STORAGE_BINDINGS.keySet()) {
                storageBindings.put(binding, new BufferBindingState(
                        glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING, binding),
                        glGetIntegeri(GL_SHADER_STORAGE_BUFFER_START, binding),
                        glGetIntegeri(GL_SHADER_STORAGE_BUFFER_SIZE, binding)
                ));
            }

            return new GlStateSnapshot(
                    glGetInteger(GL_CURRENT_PROGRAM),
                    activeTexture,
                    glGetInteger(GL_ARRAY_BUFFER),
                    glGetInteger(GL_ELEMENT_ARRAY_BUFFER),
                    glGetInteger(GL_PIXEL_UNPACK_BUFFER),
                    glGetInteger(GL_DRAW_INDIRECT_BUFFER),
                    GL.getCapabilities().OpenGL46 ? glGetInteger(GL_PARAMETER_BUFFER) : 0,
                    GL.getCapabilities().GL_ARB_indirect_parameters ? glGetInteger(GL_PARAMETER_BUFFER_ARB) : 0,
                    glGetInteger(GL_UNIFORM_BUFFER_BINDING),
                    glGetInteger(GL_SHADER_STORAGE_BUFFER_BINDING),
                    glGetInteger(GL_READ_FRAMEBUFFER_BINDING),
                    glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING),
                    glGetInteger(GL_VERTEX_ARRAY_BINDING),
                    glIsEnabled(GL_BLEND),
                    glGetInteger(GL_BLEND_SRC_RGB),
                    glGetInteger(GL_BLEND_DST_RGB),
                    glGetInteger(GL_BLEND_SRC_ALPHA),
                    glGetInteger(GL_BLEND_DST_ALPHA),
                    glGetInteger(GL_BLEND_EQUATION_RGB),
                    glGetInteger(GL_BLEND_EQUATION_ALPHA),
                    glIsEnabled(GL_DEPTH_TEST),
                    glGetInteger(GL_DEPTH_FUNC),
                    glGetBoolean(GL_DEPTH_WRITEMASK),
                    glIsEnabled(GL_CULL_FACE),
                    glGetInteger(GL_CULL_FACE_MODE),
                    glGetInteger(GL_FRONT_FACE),
                    glIsEnabled(GL_SCISSOR_TEST),
                    glIsEnabled(GL_STENCIL_TEST),
                    glGetInteger(GL_POLYGON_MODE),
                    glGetFloat(GL_LINE_WIDTH),
                    textures,
                    uniformBindings,
                    storageBindings
            );
        }

        private void restore(RhiGlStateBridge gl) {
            restoreCapability(gl, GL_BLEND, blend);
            gl.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
            gl.blendEquationSeparate(blendEquationRgb, blendEquationAlpha);
            restoreCapability(gl, GL_DEPTH_TEST, depthTest);
            gl.depthFunc(depthFunc);
            gl.depthMask(depthMask);
            restoreCapability(gl, GL_CULL_FACE, cull);
            gl.cullFace(cullFace);
            gl.frontFace(frontFace);
            restoreCapability(gl, GL_SCISSOR_TEST, scissor);
            restoreCapability(gl, GL_STENCIL_TEST, stencil);
            gl.polygonMode(GL_FRONT_AND_BACK, polygonMode);
            gl.lineWidth(lineWidth);

            gl.useProgram(program);
            gl.bindFramebuffer(GL_FRAMEBUFFER, drawFramebuffer);
            gl.bindFramebuffer(36008, readFramebuffer);

            restoreBufferBindings(gl, GL_UNIFORM_BUFFER, uniformBindings);
            restoreBufferBindings(gl, GL_SHADER_STORAGE_BUFFER, storageBindings);

            for (Map.Entry<Integer, TextureUnitState> entry : textureUnits.entrySet()) {
                gl.activeTexture(GL_TEXTURE0 + entry.getKey());
                gl.bindTexture(GL_TEXTURE_2D, entry.getValue().texture());
                gl.bindSampler(entry.getKey(), entry.getValue().sampler());
            }
            gl.activeTexture(activeTexture);

            gl.bindBuffer(GL_PIXEL_UNPACK_BUFFER, pixelUnpackBuffer);
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, drawIndirectBuffer);
            if (GL.getCapabilities().OpenGL46) {
                gl.bindBuffer(GL_PARAMETER_BUFFER, parameterBuffer);
            }
            if (GL.getCapabilities().GL_ARB_indirect_parameters) {
                gl.bindBuffer(GL_PARAMETER_BUFFER_ARB, parameterBufferArb);
            }
            gl.bindBuffer(GL_UNIFORM_BUFFER, uniformBuffer);
            gl.bindBuffer(GL_SHADER_STORAGE_BUFFER, storageBuffer);
            gl.bindBuffer(GL_ARRAY_BUFFER, arrayBuffer);
            gl.bindVertexArray(vertexArray);
            gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, elementArrayBuffer);
        }

        private static void restoreBufferBindings(RhiGlStateBridge gl, int target, Map<Integer, BufferBindingState> bindings) {
            for (Map.Entry<Integer, BufferBindingState> entry : bindings.entrySet()) {
                BufferBindingState binding = entry.getValue();
                if (binding.buffer() == 0) {
                    gl.bindBufferRange(target, entry.getKey(), 0, 0, 0);
                } else {
                    gl.bindBufferRange(target, entry.getKey(), binding.buffer(), binding.offset(), binding.size());
                }
            }
        }

        private static void restoreCapability(RhiGlStateBridge gl, int cap, boolean enabled) {
            if (enabled) {
                gl.enable(cap);
            } else {
                gl.disable(cap);
            }
        }
    }
}
