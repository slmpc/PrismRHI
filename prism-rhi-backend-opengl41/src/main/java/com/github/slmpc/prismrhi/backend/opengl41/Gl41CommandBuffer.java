package com.github.slmpc.prismrhi.backend.opengl41;

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
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_SCISSOR_TEST;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL14.glMultiDrawArrays;
import static org.lwjgl.opengl.GL14.glMultiDrawElements;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_DEPTH_STENCIL;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glClearBufferfi;
import static org.lwjgl.opengl.GL30.glClearBufferfv;
import static org.lwjgl.opengl.GL30.glDrawBuffers;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER;
import static org.lwjgl.opengl.GL40.glDrawArraysIndirect;
import static org.lwjgl.opengl.GL40.glDrawElementsIndirect;

final class Gl41CommandBuffer implements RhiCommandBuffer {
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
    private final List<Runnable> recordedCommands = new ArrayList<>();
    private RhiPrimitiveTopology primitiveTopology = RhiPrimitiveTopology.TRIANGLE_LIST;
    private RhiIndexType indexType = RhiIndexType.UINT32;
    private RhiGraphicsPipelineCreateInfo currentPipelineInfo;
    private int activeFramebuffer;
    private State state = State.INITIAL;

    Gl41CommandBuffer(RhiCommandBufferLevel level, RhiQueueType queueType, RhiGlStateBridge glStateBridge) {
        this.level = level == null ? RhiCommandBufferLevel.PRIMARY : level;
        this.queueType = queueType;
        this.gl = glStateBridge == null ? Gl41RawStateBridge.INSTANCE : glStateBridge;
    }

    @Override
    public BackendApi api() {
        return BackendApi.OPENGL_41;
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
            throw new RhiException("OpenGL 4.1 command buffer cannot begin from state " + state);
        }
        recordedCommands.clear();
        state = State.RECORDING;
    }

    @Override
    public void end() {
        if (state != State.RECORDING) {
            throw new RhiException("OpenGL 4.1 command buffer is not recording");
        }
        state = State.EXECUTABLE;
    }

    void submit() {
        if (state != State.EXECUTABLE) {
            throw new RhiException("OpenGL 4.1 command buffer must be ended before submit");
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
                throw new RhiException("OpenGL 4.1 command buffer is already inside rendering");
            }
            int framebuffer = gl.genFramebuffer();
            gl.bindFramebuffer(GL_FRAMEBUFFER, framebuffer);
            activeFramebuffer = framebuffer;
            attachRenderingTargets(renderingInfo);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new RhiException("OpenGL 4.1 framebuffer is not complete for dynamic rendering");
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
        if (!(pipeline instanceof Gl41Pipelines.Gl41GraphicsPipeline glPipeline)) {
            throw new RhiException("OpenGL 4.1 bindGraphicsPipeline requires an OpenGL 4.1 pipeline");
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
            throw new RhiException("OpenGL 4.1 bindVertexBuffers requires a bound graphics pipeline");
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
        requireBackendBuffer(buffer, "OpenGL 4.1 bindIndexBuffer");
        this.indexType = indexType == null ? RhiIndexType.UINT32 : indexType;
        recordedCommands.add(() -> gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, (int) buffer.nativeHandle()));
    }

    @Override
    public void draw(RhiDrawCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        recordedCommands.add(() -> {
            int mode = Gl41Support.primitiveTopology(topology);
            if (command.instanceCount() <= 1) {
                glDrawArrays(mode, command.firstVertex(), command.vertexCount());
            } else {
                requireZeroFirstInstance(command.firstInstance(), "OpenGL 4.1 draw instanced");
                glDrawArraysInstanced(mode, command.firstVertex(), command.vertexCount(), command.instanceCount());
            }
        });
    }

    @Override
    public void drawIndexed(RhiDrawIndexedCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> {
            int mode = Gl41Support.primitiveTopology(topology);
            int glIndexType = Gl41Support.indexType(capturedIndexType);
            long indexOffset = (long) command.firstIndex() * capturedIndexType.bytes();
            if (command.instanceCount() <= 1 && command.vertexOffset() == 0) {
                requireZeroFirstInstance(command.firstInstance(), "OpenGL 4.1 indexed draw");
                glDrawElements(mode, command.indexCount(), glIndexType, indexOffset);
            } else if (command.vertexOffset() == 0) {
                requireZeroFirstInstance(command.firstInstance(), "OpenGL 4.1 indexed instanced draw");
                glDrawElementsInstanced(mode, command.indexCount(), glIndexType, indexOffset, command.instanceCount());
            } else {
                throw new RhiException("OpenGL 4.1 backend does not support vertexOffset in indexed draw");
            }
        });
    }

    @Override
    public void multiDraw(RhiMultiDrawCommand command) {
        ensureRecording();
        if (command.drawCount() == 0) {
            return;
        }
        requireZeroFirstInstance(command.firstInstance(), "OpenGL 4.1 multiDraw");
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
                glMultiDrawArrays(Gl41Support.primitiveTopology(topology), first, count);
            }
        });
    }

    @Override
    public void multiDrawIndexed(RhiMultiDrawIndexedCommand command) {
        ensureRecording();
        if (command.drawCount() == 0) {
            return;
        }
        requireZeroFirstInstance(command.firstInstance(), "OpenGL 4.1 multiDrawIndexed");
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer count = stack.mallocInt(command.drawCount());
                PointerBuffer indices = stack.mallocPointer(command.drawCount());
                for (RhiDrawIndexedCommand drawCommand : command.commands()) {
                    if (drawCommand.vertexOffset() != 0) {
                        throw new RhiException("OpenGL 4.1 multiDrawIndexed does not support vertexOffset");
                    }
                    count.put(drawCommand.indexCount());
                    indices.put((long) drawCommand.firstIndex() * capturedIndexType.bytes());
                }
                count.flip();
                indices.flip();
                glMultiDrawElements(
                        Gl41Support.primitiveTopology(topology),
                        count,
                        Gl41Support.indexType(capturedIndexType),
                        indices
                );
            }
        });
    }

    @Override
    public void drawIndirect(RhiDrawIndirectCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        recordedCommands.add(() -> {
            requireBackendBuffer(command.buffer(), "OpenGL 4.1 drawIndirect");
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            glDrawArraysIndirect(Gl41Support.primitiveTopology(topology), command.offset());
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        });
    }

    @Override
    public void drawIndexedIndirect(RhiDrawIndexedIndirectCommand command) {
        ensureRecording();
        RhiPrimitiveTopology topology = primitiveTopology;
        RhiIndexType capturedIndexType = indexType;
        recordedCommands.add(() -> {
            requireBackendBuffer(command.buffer(), "OpenGL 4.1 drawIndexedIndirect");
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, (int) command.buffer().nativeHandle());
            glDrawElementsIndirect(
                    Gl41Support.primitiveTopology(topology),
                    Gl41Support.indexType(capturedIndexType),
                    command.offset()
            );
            gl.bindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        });
    }

    @Override
    public void multiDrawIndirect(RhiMultiDrawIndirectCommand command) {
        ensureRecording();
        if (command.drawCount() <= 1) {
            drawIndirect(new RhiDrawIndirectCommand(command.buffer(), command.offset()));
            return;
        }
        throw new RhiException("OpenGL 4.1 backend does not support multiDrawIndirect");
    }

    @Override
    public void multiDrawIndexedIndirect(RhiMultiDrawIndexedIndirectCommand command) {
        ensureRecording();
        if (command.drawCount() <= 1) {
            drawIndexedIndirect(new RhiDrawIndexedIndirectCommand(command.buffer(), command.offset()));
            return;
        }
        throw new RhiException("OpenGL 4.1 backend does not support multiDrawIndexedIndirect");
    }

    @Override
    public void multiDrawIndirectCount(RhiMultiDrawIndirectCountCommand command) {
        ensureRecording();
        throw new RhiException("OpenGL 4.1 backend does not support multiDrawIndirectCount");
    }

    @Override
    public void multiDrawIndexedIndirectCount(RhiMultiDrawIndexedIndirectCountCommand command) {
        ensureRecording();
        throw new RhiException("OpenGL 4.1 backend does not support multiDrawIndexedIndirectCount");
    }

    RhiQueueType queueType() {
        return queueType;
    }

    private void ensureRecording() {
        if (state != State.RECORDING) {
            throw new RhiException("OpenGL 4.1 command buffer must be recording");
        }
    }

    private void requireZeroFirstInstance(int firstInstance, String operation) {
        if (firstInstance != 0) {
            throw new RhiException(operation + " does not support firstInstance");
        }
    }

    private void requireBackendBuffer(com.github.slmpc.prismrhi.resource.RhiBuffer buffer, String operation) {
        if (buffer.api() != BackendApi.OPENGL_41) {
            throw new RhiException(operation + " requires an OpenGL 4.1 buffer");
        }
    }

    private void applyPipelineState(RhiGraphicsPipelineCreateInfo pipelineInfo) {
        var raster = pipelineInfo.rasterization();
        gl.polygonMode(GL_FRONT_AND_BACK, Gl41Support.polygonMode(raster.polygonMode()));
        gl.frontFace(Gl41Support.frontFace(raster.frontFace()));
        gl.lineWidth(raster.lineWidth());

        int cullMode = Gl41Support.cullMode(raster.cullMode());
        if (cullMode == 0) {
            gl.disable(GL_CULL_FACE);
        } else {
            gl.enable(GL_CULL_FACE);
            gl.cullFace(cullMode);
        }

        var depthStencil = pipelineInfo.depthStencil();
        if (depthStencil.depthTestEnable()) {
            gl.enable(GL_DEPTH_TEST);
            gl.depthFunc(Gl41Support.compareOp(depthStencil.depthCompareOp()));
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
                    Gl41Support.blendFactor(blend.srcColorBlendFactor()),
                    Gl41Support.blendFactor(blend.dstColorBlendFactor()),
                    Gl41Support.blendFactor(blend.srcAlphaBlendFactor()),
                    Gl41Support.blendFactor(blend.dstAlphaBlendFactor())
            );
            gl.blendEquationSeparate(
                    Gl41Support.blendOp(blend.colorBlendOp()),
                    Gl41Support.blendOp(blend.alphaBlendOp())
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
                requireImageView(attachment, "OpenGL 4.1 color attachment");
                int attachmentPoint = Gl41Support.colorAttachment(i);
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
            requireImageView(attachment, "OpenGL 4.1 depth attachment");
            gl.framebufferTexture2D(
                    GL_FRAMEBUFFER,
                    Gl41Support.depthStencilAttachment(attachment.view().format()),
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
            if (Gl41Support.stencilFormat(attachment.view().format())) {
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
            requireBackendBuffer(bufferBinding.buffer(), "OpenGL 4.1 bindVertexBuffers");
            gl.bindBuffer(GL_ARRAY_BUFFER, (int) bufferBinding.buffer().nativeHandle());
            RhiVertexInputBinding inputBinding = pipelineInfo.vertexInputBindings().stream()
                    .filter(binding -> binding.binding() == bufferBinding.binding())
                    .findFirst()
                    .orElse(new RhiVertexInputBinding(bufferBinding.binding(), 0, RhiVertexInputRate.VERTEX));
            for (RhiVertexAttribute attribute : pipelineInfo.vertexAttributes()) {
                if (attribute.binding() == bufferBinding.binding()) {
                    Gl41Support.VertexAttributeFormat format = Gl41Support.vertexAttributeFormat(attribute.format());
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
        if (!(descriptorSet instanceof Gl41Descriptors.Gl41DescriptorSet glSet)) {
            throw new RhiException("OpenGL 4.1 bindDescriptorSets requires OpenGL 4.1 descriptor sets");
        }
        for (RhiDescriptorWrite write : glSet.writes()) {
            if (write.type() == RhiDescriptorType.UNIFORM_BUFFER || write.type() == RhiDescriptorType.UNIFORM_BUFFER_DYNAMIC) {
                requireBackendBuffer(write.buffer(), "OpenGL 4.1 uniform buffer descriptor");
                long range = write.range() == 0 ? write.buffer().size() : write.range();
                gl.bindBufferRange(GL_UNIFORM_BUFFER, write.binding(), (int) write.buffer().nativeHandle(), write.offset(), range);
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
                throw new RhiException("OpenGL 4.1 backend does not support " + write.type() + " descriptors");
            }
        }
    }

    private void requireImageView(RhiRenderingAttachment attachment, String operation) {
        if (attachment.view().api() != BackendApi.OPENGL_41) {
            throw new RhiException(operation + " requires an OpenGL 4.1 image view");
        }
    }

    private void requireImageDescriptor(RhiDescriptorWrite write) {
        if (write.imageView() == null || write.imageView().api() != BackendApi.OPENGL_41) {
            throw new RhiException("OpenGL 4.1 image descriptor requires an OpenGL 4.1 image view");
        }
    }

    private void requireSampler(RhiDescriptorWrite write) {
        if (write.sampler() == null || write.sampler().api() != BackendApi.OPENGL_41) {
            throw new RhiException("OpenGL 4.1 sampler descriptor requires an OpenGL 4.1 sampler");
        }
    }
}
