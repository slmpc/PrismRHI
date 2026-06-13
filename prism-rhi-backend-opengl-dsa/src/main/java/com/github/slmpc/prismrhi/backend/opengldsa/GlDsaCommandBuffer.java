package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.backend.BackendApi;
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
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.opengl.ARBIndirectParameters.GL_PARAMETER_BUFFER_ARB;
import static org.lwjgl.opengl.ARBIndirectParameters.glMultiDrawArraysIndirectCountARB;
import static org.lwjgl.opengl.ARBIndirectParameters.glMultiDrawElementsIndirectCountARB;
import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.glMultiDrawArrays;
import static org.lwjgl.opengl.GL14.glMultiDrawElements;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.glBindBufferRange;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glClearBufferfi;
import static org.lwjgl.opengl.GL30.glClearBufferfv;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDrawBuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL33.glBindSampler;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL42.glDrawArraysInstancedBaseInstance;
import static org.lwjgl.opengl.GL42.glDrawElementsInstancedBaseVertexBaseInstance;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;
import static org.lwjgl.opengl.GL43.glMultiDrawArraysIndirect;
import static org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect;
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
    private final List<Runnable> recordedCommands = new ArrayList<>();
    private RhiPrimitiveTopology primitiveTopology = RhiPrimitiveTopology.TRIANGLE_LIST;
    private RhiIndexType indexType = RhiIndexType.UINT32;
    private RhiGraphicsPipelineCreateInfo currentPipelineInfo;
    private int activeFramebuffer;
    private State state = State.INITIAL;

    GlDsaCommandBuffer(RhiCommandBufferLevel level, RhiQueueType queueType) {
        this.level = level == null ? RhiCommandBufferLevel.PRIMARY : level;
        this.queueType = queueType;
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
        recordedCommands.forEach(Runnable::run);
    }

    @Override
    public void close() {
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
            int framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
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
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glDeleteFramebuffers(activeFramebuffer);
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
        recordedCommands.add(() -> glUseProgram((int) glPipeline.nativeHandle()));
    }

    @Override
    public void bindDescriptorSets(RhiGraphicsPipeline pipeline, int firstSet, List<RhiDescriptorSet> descriptorSets) {
        ensureRecording();
        if (firstSet < 0) {
            throw new IllegalArgumentException("firstSet must not be negative");
        }
        List<RhiDescriptorSet> capturedSets = List.copyOf(descriptorSets);
        recordedCommands.add(() -> {
            for (RhiDescriptorSet descriptorSet : capturedSets) {
                applyDescriptorSet(descriptorSet);
            }
        });
    }

    @Override
    public void setViewport(RhiViewport viewport) {
        ensureRecording();
        recordedCommands.add(() -> glViewport(
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
            glEnable(GL_SCISSOR_TEST);
            glScissor(
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
        recordedCommands.add(() -> glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, (int) buffer.nativeHandle()));
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
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            glMultiDrawArraysIndirect(
                    GlDsaSupport.primitiveTopology(topology),
                    command.offset(),
                    command.drawCount(),
                    command.stride()
            );
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        });
    }

    @Override
    public void multiDrawIndexedIndirect(RhiMultiDrawIndexedIndirectCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> {
            requireBackendBuffer(command.buffer(), "OpenGL DSA multiDrawIndexedIndirect");
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            glMultiDrawElementsIndirect(
                    GlDsaSupport.primitiveTopology(topology),
                    GlDsaSupport.indexType(capturedIndexType),
                    command.offset(),
                    command.drawCount(),
                    command.stride()
            );
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
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
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            if (GL.getCapabilities().OpenGL46) {
                glBindBuffer(GL_PARAMETER_BUFFER, (int) command.countBuffer().nativeHandle());
                glMultiDrawArraysIndirectCount(
                        GlDsaSupport.primitiveTopology(topology),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                glBindBuffer(GL_PARAMETER_BUFFER, 0);
            } else {
                glBindBuffer(GL_PARAMETER_BUFFER_ARB, (int) command.countBuffer().nativeHandle());
                glMultiDrawArraysIndirectCountARB(
                        GlDsaSupport.primitiveTopology(topology),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                glBindBuffer(GL_PARAMETER_BUFFER_ARB, 0);
            }
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
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
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            if (GL.getCapabilities().OpenGL46) {
                glBindBuffer(GL_PARAMETER_BUFFER, (int) command.countBuffer().nativeHandle());
                glMultiDrawElementsIndirectCount(
                        GlDsaSupport.primitiveTopology(topology),
                        GlDsaSupport.indexType(capturedIndexType),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                glBindBuffer(GL_PARAMETER_BUFFER, 0);
            } else {
                glBindBuffer(GL_PARAMETER_BUFFER_ARB, (int) command.countBuffer().nativeHandle());
                glMultiDrawElementsIndirectCountARB(
                        GlDsaSupport.primitiveTopology(topology),
                        GlDsaSupport.indexType(capturedIndexType),
                        command.offset(),
                        command.countOffset(),
                        command.maxDrawCount(),
                        command.stride()
                );
                glBindBuffer(GL_PARAMETER_BUFFER_ARB, 0);
            }
            glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
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

    private void attachRenderingTargets(RhiRenderingInfo renderingInfo) {
        int colorCount = renderingInfo.colorAttachments().size();
        if (colorCount > 0) {
            int[] drawBuffers = new int[colorCount];
            for (int i = 0; i < colorCount; i++) {
                RhiRenderingAttachment attachment = renderingInfo.colorAttachments().get(i);
                requireImageView(attachment, "OpenGL DSA color attachment");
                int attachmentPoint = GlDsaSupport.colorAttachment(i);
                glFramebufferTexture2D(
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
            glFramebufferTexture2D(
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
            glBindBuffer(GL_ARRAY_BUFFER, (int) bufferBinding.buffer().nativeHandle());
            RhiVertexInputBinding inputBinding = pipelineInfo.vertexInputBindings().stream()
                    .filter(binding -> binding.binding() == bufferBinding.binding())
                    .findFirst()
                    .orElse(new RhiVertexInputBinding(bufferBinding.binding(), 0, RhiVertexInputRate.VERTEX));
            for (RhiVertexAttribute attribute : pipelineInfo.vertexAttributes()) {
                if (attribute.binding() == bufferBinding.binding()) {
                    GlDsaSupport.VertexAttributeFormat format = GlDsaSupport.vertexAttributeFormat(attribute.format());
                    glEnableVertexAttribArray(attribute.location());
                    glVertexAttribPointer(
                            attribute.location(),
                            format.components(),
                            format.type(),
                            format.normalized(),
                            inputBinding.stride(),
                            bufferBinding.offset() + attribute.offset()
                    );
                    glVertexAttribDivisor(attribute.location(), inputBinding.inputRate() == RhiVertexInputRate.INSTANCE ? 1 : 0);
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
                glBindBufferRange(GL_UNIFORM_BUFFER, write.binding(), (int) write.buffer().nativeHandle(), write.offset(), range);
            } else if (write.type() == RhiDescriptorType.STORAGE_BUFFER || write.type() == RhiDescriptorType.STORAGE_BUFFER_DYNAMIC) {
                requireBackendBuffer(write.buffer(), "OpenGL DSA storage buffer descriptor");
                long range = write.range() == 0 ? write.buffer().size() : write.range();
                glBindBufferRange(GL_SHADER_STORAGE_BUFFER, write.binding(), (int) write.buffer().nativeHandle(), write.offset(), range);
            } else if (write.type() == RhiDescriptorType.SAMPLER) {
                requireSampler(write);
                glBindSampler(write.binding(), (int) write.sampler().nativeHandle());
            } else if (write.type() == RhiDescriptorType.SAMPLED_IMAGE
                    || write.type() == RhiDescriptorType.COMBINED_IMAGE_SAMPLER
                    || write.type() == RhiDescriptorType.INPUT_ATTACHMENT) {
                requireImageDescriptor(write);
                glActiveTexture(GL_TEXTURE0 + write.binding());
                glBindTexture(GL_TEXTURE_2D, (int) write.imageView().nativeHandle());
                if (write.sampler() != null) {
                    requireSampler(write);
                    glBindSampler(write.binding(), (int) write.sampler().nativeHandle());
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
}
