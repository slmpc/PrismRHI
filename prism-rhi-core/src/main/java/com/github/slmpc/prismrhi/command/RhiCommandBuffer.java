package com.github.slmpc.prismrhi.command;

import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiIndexType;
import com.github.slmpc.prismrhi.resource.RhiResource;
import com.github.slmpc.prismrhi.barrier.RhiPipelineBarrier;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiVertexBufferBinding;
import com.github.slmpc.prismrhi.rendering.RhiViewport;

import java.util.List;

public interface RhiCommandBuffer extends RhiResource {
    RhiCommandBufferLevel level();

    void begin();

    void end();

    default void setPrimitiveTopology(RhiPrimitiveTopology topology) {
    }

    default void setIndexType(RhiIndexType indexType) {
    }

    default void pipelineBarrier(RhiPipelineBarrier barrier) {
    }

    default void beginRendering(RhiRenderingInfo renderingInfo) {
    }

    default void endRendering() {
    }

    default void bindGraphicsPipeline(RhiGraphicsPipeline pipeline) {
    }

    default void bindDescriptorSet(RhiGraphicsPipeline pipeline, int setIndex, RhiDescriptorSet descriptorSet) {
        bindDescriptorSets(pipeline, setIndex, List.of(descriptorSet));
    }

    default void bindDescriptorSets(RhiGraphicsPipeline pipeline, int firstSet, List<RhiDescriptorSet> descriptorSets) {
    }

    default void setViewport(RhiViewport viewport) {
    }

    default void setScissor(RhiRect2D scissor) {
    }

    default void bindVertexBuffer(int binding, RhiBuffer buffer, long offset) {
        bindVertexBuffers(List.of(new RhiVertexBufferBinding(binding, buffer, offset)));
    }

    default void bindVertexBuffers(List<RhiVertexBufferBinding> bindings) {
    }

    default void bindIndexBuffer(RhiBuffer buffer, long offset, RhiIndexType indexType) {
        setIndexType(indexType);
    }

    void draw(RhiDrawCommand command);

    default void draw(int vertexCount) {
        draw(new RhiDrawCommand(vertexCount, 1, 0, 0));
    }

    default void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        draw(new RhiDrawCommand(vertexCount, instanceCount, firstVertex, firstInstance));
    }

    void drawIndexed(RhiDrawIndexedCommand command);

    default void drawIndexed(int indexCount) {
        drawIndexed(new RhiDrawIndexedCommand(indexCount, 1, 0, 0, 0));
    }

    default void drawIndexed(int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
        drawIndexed(new RhiDrawIndexedCommand(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance));
    }

    default void drawInstanced(int vertexCount, int instanceCount) {
        draw(new RhiDrawCommand(vertexCount, instanceCount, 0, 0));
    }

    default void drawInstanced(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        draw(new RhiDrawCommand(vertexCount, instanceCount, firstVertex, firstInstance));
    }

    default void drawIndexedInstanced(int indexCount, int instanceCount) {
        drawIndexed(new RhiDrawIndexedCommand(indexCount, instanceCount, 0, 0, 0));
    }

    default void drawIndexedInstanced(
            int indexCount,
            int instanceCount,
            int firstIndex,
            int vertexOffset,
            int firstInstance
    ) {
        drawIndexed(new RhiDrawIndexedCommand(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance));
    }

    void multiDraw(RhiMultiDrawCommand command);

    default void multiDraw(Iterable<RhiDrawCommand> commands) {
        multiDraw(new RhiMultiDrawCommand(commands));
    }

    void multiDrawIndexed(RhiMultiDrawIndexedCommand command);

    default void multiDrawIndexed(Iterable<RhiDrawIndexedCommand> commands) {
        multiDrawIndexed(new RhiMultiDrawIndexedCommand(commands));
    }

    void drawIndirect(RhiDrawIndirectCommand command);

    default void drawIndirect(RhiBuffer buffer, long offset) {
        drawIndirect(new RhiDrawIndirectCommand(buffer, offset));
    }

    void drawIndexedIndirect(RhiDrawIndexedIndirectCommand command);

    default void drawIndexedIndirect(RhiBuffer buffer, long offset) {
        drawIndexedIndirect(new RhiDrawIndexedIndirectCommand(buffer, offset));
    }

    void multiDrawIndirect(RhiMultiDrawIndirectCommand command);

    default void multiDrawIndirect(RhiBuffer buffer, long offset, int drawCount, int stride) {
        multiDrawIndirect(new RhiMultiDrawIndirectCommand(buffer, offset, drawCount, stride));
    }

    void multiDrawIndexedIndirect(RhiMultiDrawIndexedIndirectCommand command);

    default void multiDrawIndexedIndirect(RhiBuffer buffer, long offset, int drawCount, int stride) {
        multiDrawIndexedIndirect(new RhiMultiDrawIndexedIndirectCommand(buffer, offset, drawCount, stride));
    }

    void multiDrawIndirectCount(RhiMultiDrawIndirectCountCommand command);

    default void multiDrawIndirectCount(
            RhiBuffer buffer,
            long offset,
            RhiBuffer countBuffer,
            long countOffset,
            int maxDrawCount,
            int stride
    ) {
        multiDrawIndirectCount(new RhiMultiDrawIndirectCountCommand(
                buffer,
                offset,
                countBuffer,
                countOffset,
                maxDrawCount,
                stride
        ));
    }

    default void drawIndirectCount(RhiBuffer buffer, long offset, RhiBuffer countBuffer, long countOffset, int stride) {
        multiDrawIndirectCount(buffer, offset, countBuffer, countOffset, 1, stride);
    }

    void multiDrawIndexedIndirectCount(RhiMultiDrawIndexedIndirectCountCommand command);

    default void multiDrawIndexedIndirectCount(
            RhiBuffer buffer,
            long offset,
            RhiBuffer countBuffer,
            long countOffset,
            int maxDrawCount,
            int stride
    ) {
        multiDrawIndexedIndirectCount(new RhiMultiDrawIndexedIndirectCountCommand(
                buffer,
                offset,
                countBuffer,
                countOffset,
                maxDrawCount,
                stride
        ));
    }

    default void drawIndexedIndirectCount(RhiBuffer buffer, long offset, RhiBuffer countBuffer, long countOffset, int stride) {
        multiDrawIndexedIndirectCount(buffer, offset, countBuffer, countOffset, 1, stride);
    }
}
