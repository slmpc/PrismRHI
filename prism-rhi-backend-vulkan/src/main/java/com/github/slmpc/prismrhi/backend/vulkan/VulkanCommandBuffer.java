package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.barrier.RhiBufferBarrier;
import com.github.slmpc.prismrhi.barrier.RhiImageBarrier;
import com.github.slmpc.prismrhi.barrier.RhiPipelineBarrier;
import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiDrawCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndexedCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndexedIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndirectCommand;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.command.RhiMultiDrawCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedIndirectCountCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndirectCountCommand;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.rendering.RhiClearValue;
import com.github.slmpc.prismrhi.rendering.RhiImageLayout;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiVertexBufferBinding;
import com.github.slmpc.prismrhi.rendering.RhiViewport;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageUploadInfo;
import com.github.slmpc.prismrhi.resource.RhiIndexType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkViewport;
import org.lwjgl.vulkan.VkMultiDrawIndexedInfoEXT;
import org.lwjgl.vulkan.VkMultiDrawInfoEXT;

import java.nio.LongBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT16;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexedIndirect;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndirect;
import static org.lwjgl.vulkan.VK10.vkCmdSetScissor;
import static org.lwjgl.vulkan.VK10.vkCmdSetViewport;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkResetCommandBuffer;
import static org.lwjgl.vulkan.VK12.vkCmdDrawIndexedIndirectCount;
import static org.lwjgl.vulkan.VK12.vkCmdDrawIndirectCount;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_RENDERING_INFO;
import static org.lwjgl.vulkan.VK13.vkCmdBeginRendering;
import static org.lwjgl.vulkan.VK13.vkCmdEndRendering;
import static org.lwjgl.vulkan.EXTMultiDraw.vkCmdDrawMultiEXT;
import static org.lwjgl.vulkan.EXTMultiDraw.vkCmdDrawMultiIndexedEXT;

final class VulkanCommandBuffer implements RhiCommandBuffer {
    private final VkCommandBuffer commandBuffer;
    private final RhiCommandBufferLevel level;
    private final RhiQueueType queueType;
    private final boolean multiDrawSupported;

    VulkanCommandBuffer(
            VkDevice device,
            long handle,
            RhiCommandBufferLevel level,
            RhiQueueType queueType,
            boolean multiDrawSupported
    ) {
        this.commandBuffer = new VkCommandBuffer(handle, device);
        this.level = level == null ? RhiCommandBufferLevel.PRIMARY : level;
        this.queueType = queueType;
        this.multiDrawSupported = multiDrawSupported;
    }

    @Override
    public BackendApi api() {
        return BackendApi.VULKAN;
    }

    @Override
    public long nativeHandle() {
        return commandBuffer.address();
    }

    @Override
    public RhiCommandBufferLevel level() {
        return level;
    }

    @Override
    public void begin() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            VulkanSupport.check(vkBeginCommandBuffer(commandBuffer, beginInfo), "vkBeginCommandBuffer");
        }
    }

    @Override
    public void reset() {
        VulkanSupport.check(vkResetCommandBuffer(commandBuffer, 0), "vkResetCommandBuffer");
    }

    @Override
    public void end() {
        VulkanSupport.check(vkEndCommandBuffer(commandBuffer), "vkEndCommandBuffer");
    }

    @Override
    public void close() {
    }

    @Override
    public void pipelineBarrier(RhiPipelineBarrier barrier) {
        if (barrier == null || barrier.isEmpty()) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer imageBarriers = null;
            if (!barrier.imageBarriers().isEmpty()) {
                imageBarriers = VkImageMemoryBarrier.calloc(barrier.imageBarriers().size(), stack);
                for (int i = 0; i < barrier.imageBarriers().size(); i++) {
                    fillImageBarrier(imageBarriers.get(i), barrier.imageBarriers().get(i));
                }
            }

            VkBufferMemoryBarrier.Buffer bufferBarriers = null;
            if (!barrier.bufferBarriers().isEmpty()) {
                bufferBarriers = VkBufferMemoryBarrier.calloc(barrier.bufferBarriers().size(), stack);
                for (int i = 0; i < barrier.bufferBarriers().size(); i++) {
                    fillBufferBarrier(bufferBarriers.get(i), barrier.bufferBarriers().get(i));
                }
            }

            vkCmdPipelineBarrier(
                    commandBuffer,
                    sourceStages(barrier),
                    destinationStages(barrier),
                    0,
                    null,
                    bufferBarriers,
                    imageBarriers
            );
        }
    }

    @Override
    public void beginRendering(RhiRenderingInfo renderingInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingAttachmentInfo.Buffer colorAttachments = null;
            if (!renderingInfo.colorAttachments().isEmpty()) {
                colorAttachments = VkRenderingAttachmentInfo.calloc(renderingInfo.colorAttachments().size(), stack);
                for (int i = 0; i < renderingInfo.colorAttachments().size(); i++) {
                    fillAttachment(colorAttachments.get(i), renderingInfo.colorAttachments().get(i));
                }
            }
            VkRenderingAttachmentInfo depthAttachment = null;
            if (renderingInfo.depthAttachment() != null) {
                depthAttachment = VkRenderingAttachmentInfo.calloc(stack);
                fillAttachment(depthAttachment, renderingInfo.depthAttachment());
            }
            VkRenderingAttachmentInfo stencilAttachment = null;
            if (renderingInfo.stencilAttachment() != null) {
                stencilAttachment = VkRenderingAttachmentInfo.calloc(stack);
                fillAttachment(stencilAttachment, renderingInfo.stencilAttachment());
            }

            VkRenderingInfo vkRenderingInfo = VkRenderingInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
                    .renderArea(vkRect(renderingInfo.renderArea(), stack))
                    .layerCount(renderingInfo.layerCount())
                    .viewMask(renderingInfo.viewMask())
                    .pColorAttachments(colorAttachments)
                    .pDepthAttachment(depthAttachment)
                    .pStencilAttachment(stencilAttachment);
            vkCmdBeginRendering(commandBuffer, vkRenderingInfo);
        }
    }

    @Override
    public void endRendering() {
        vkCmdEndRendering(commandBuffer);
    }

    @Override
    public void bindGraphicsPipeline(RhiGraphicsPipeline pipeline) {
        if (pipeline.api() != BackendApi.VULKAN) {
            throw new RhiException("Vulkan bindGraphicsPipeline requires a Vulkan pipeline");
        }
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.nativeHandle());
    }

    @Override
    public void bindDescriptorSets(RhiGraphicsPipeline pipeline, int firstSet, List<RhiDescriptorSet> descriptorSets) {
        if (pipeline.api() != BackendApi.VULKAN) {
            throw new RhiException("Vulkan bindDescriptorSets requires a Vulkan pipeline");
        }
        if (firstSet < 0) {
            throw new IllegalArgumentException("firstSet must not be negative");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer sets = stack.mallocLong(descriptorSets.size());
            for (RhiDescriptorSet descriptorSet : descriptorSets) {
                if (descriptorSet.api() != BackendApi.VULKAN) {
                    throw new RhiException("Vulkan bindDescriptorSets requires Vulkan descriptor sets");
                }
                sets.put(descriptorSet.nativeHandle());
            }
            sets.flip();
            vkCmdBindDescriptorSets(
                    commandBuffer,
                    VK_PIPELINE_BIND_POINT_GRAPHICS,
                    pipeline.nativeLayoutHandle(),
                    firstSet,
                    sets,
                    null
            );
        }
    }

    @Override
    public void setViewport(RhiViewport viewport) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkViewport.Buffer viewports = VkViewport.calloc(1, stack);
            viewports.get(0)
                    .x(viewport.x())
                    .y(viewport.y())
                    .width(viewport.width())
                    .height(viewport.height())
                    .minDepth(viewport.minDepth())
                    .maxDepth(viewport.maxDepth());
            vkCmdSetViewport(commandBuffer, 0, viewports);
        }
    }

    @Override
    public void setScissor(RhiRect2D scissor) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRect2D.Buffer scissors = VkRect2D.calloc(1, stack);
            fillRect(scissors.get(0), scissor);
            vkCmdSetScissor(commandBuffer, 0, scissors);
        }
    }

    @Override
    public void bindVertexBuffers(List<RhiVertexBufferBinding> bindings) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffers = stack.mallocLong(bindings.size());
            LongBuffer offsets = stack.mallocLong(bindings.size());
            int firstBinding = bindings.isEmpty() ? 0 : bindings.get(0).binding();
            for (int i = 0; i < bindings.size(); i++) {
                RhiVertexBufferBinding binding = bindings.get(i);
                requireBackendBuffer(binding.buffer(), "Vulkan bindVertexBuffers");
                if (binding.binding() != firstBinding + i) {
                    throw new RhiException("Vulkan bindVertexBuffers requires consecutive binding indices");
                }
                buffers.put(binding.buffer().nativeHandle());
                offsets.put(binding.offset());
            }
            buffers.flip();
            offsets.flip();
            vkCmdBindVertexBuffers(commandBuffer, firstBinding, buffers, offsets);
        }
    }

    @Override
    public void bindIndexBuffer(RhiBuffer buffer, long offset, RhiIndexType indexType) {
        requireBackendBuffer(buffer, "Vulkan bindIndexBuffer");
        vkCmdBindIndexBuffer(commandBuffer, buffer.nativeHandle(), offset, indexType(indexType));
    }

    @Override
    public void copyBufferToImage(RhiBuffer source, RhiImage destination, RhiImageUploadInfo uploadInfo) {
        requireBackendBuffer(source, "Vulkan copyBufferToImage");
        requireBackendImage(destination, "Vulkan copyBufferToImage");
        uploadInfo.validateFor(destination);
        if (uploadInfo.mipLevel() != 0 || uploadInfo.arrayLayer() != 0 || uploadInfo.layerCount() != 1) {
            throw new RhiException("Vulkan copyBufferToImage currently supports mip level 0 and array layer 0 only");
        }
        if (source.size() < uploadInfo.requiredBytes(destination.format())) {
            throw new IllegalArgumentException("source buffer is too small for image upload");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(uploadInfo.bufferOffset())
                    .bufferRowLength(uploadInfo.bufferRowLength(destination.format()))
                    .bufferImageHeight(uploadInfo.bufferImageHeight(destination.format()));
            region.imageSubresource()
                    .aspectMask(VulkanSupport.imageAspect(RhiImageBarrier.defaultAspects(destination)))
                    .mipLevel(uploadInfo.mipLevel())
                    .baseArrayLayer(uploadInfo.arrayLayer())
                    .layerCount(uploadInfo.layerCount());
            region.imageOffset()
                    .x(uploadInfo.x())
                    .y(uploadInfo.y())
                    .z(uploadInfo.z());
            region.imageExtent()
                    .width(uploadInfo.width())
                    .height(uploadInfo.height())
                    .depth(uploadInfo.depth());
            vkCmdCopyBufferToImage(
                    commandBuffer,
                    source.nativeHandle(),
                    destination.nativeHandle(),
                    VulkanSupport.imageLayout(RhiResourceState.TRANSFER_DST),
                    region
            );
        }
    }

    @Override
    public void draw(RhiDrawCommand command) {
        vkCmdDraw(
                commandBuffer,
                command.vertexCount(),
                command.instanceCount(),
                command.firstVertex(),
                command.firstInstance()
        );
    }

    @Override
    public void drawIndexed(RhiDrawIndexedCommand command) {
        vkCmdDrawIndexed(
                commandBuffer,
                command.indexCount(),
                command.instanceCount(),
                command.firstIndex(),
                command.vertexOffset(),
                command.firstInstance()
        );
    }

    @Override
    public void multiDraw(RhiMultiDrawCommand command) {
        if (command.drawCount() == 0) {
            return;
        }
        requireMultiDrawSupport("Vulkan multiDraw");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMultiDrawInfoEXT.Buffer drawInfos = VkMultiDrawInfoEXT.calloc(command.drawCount(), stack);
            for (int i = 0; i < command.commands().size(); i++) {
                RhiDrawCommand drawCommand = command.commands().get(i);
                drawInfos.get(i)
                        .firstVertex(drawCommand.firstVertex())
                        .vertexCount(drawCommand.vertexCount());
            }
            vkCmdDrawMultiEXT(commandBuffer, drawInfos, command.instanceCount(), command.firstInstance(), 0);
        }
    }

    @Override
    public void multiDrawIndexed(RhiMultiDrawIndexedCommand command) {
        if (command.drawCount() == 0) {
            return;
        }
        requireMultiDrawSupport("Vulkan multiDrawIndexed");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMultiDrawIndexedInfoEXT.Buffer drawInfos = VkMultiDrawIndexedInfoEXT.calloc(command.drawCount(), stack);
            for (int i = 0; i < command.commands().size(); i++) {
                RhiDrawIndexedCommand drawCommand = command.commands().get(i);
                drawInfos.get(i)
                        .firstIndex(drawCommand.firstIndex())
                        .indexCount(drawCommand.indexCount())
                        .vertexOffset(drawCommand.vertexOffset());
            }
            vkCmdDrawMultiIndexedEXT(
                    commandBuffer,
                    drawInfos,
                    command.instanceCount(),
                    command.firstInstance(),
                    0,
                    (java.nio.IntBuffer) null
            );
        }
    }

    @Override
    public void drawIndirect(RhiDrawIndirectCommand command) {
        requireBackendBuffer(command.buffer(), "Vulkan drawIndirect");
        vkCmdDrawIndirect(commandBuffer, command.buffer().nativeHandle(), command.offset(), 1, RhiIndirectCommandLayout.DRAW_STRIDE);
    }

    @Override
    public void drawIndexedIndirect(RhiDrawIndexedIndirectCommand command) {
        requireBackendBuffer(command.buffer(), "Vulkan drawIndexedIndirect");
        vkCmdDrawIndexedIndirect(
                commandBuffer,
                command.buffer().nativeHandle(),
                command.offset(),
                1,
                RhiIndirectCommandLayout.DRAW_INDEXED_STRIDE
        );
    }

    @Override
    public void multiDrawIndirect(RhiMultiDrawIndirectCommand command) {
        requireBackendBuffer(command.buffer(), "Vulkan multiDrawIndirect");
        vkCmdDrawIndirect(
                commandBuffer,
                command.buffer().nativeHandle(),
                command.offset(),
                command.drawCount(),
                RhiIndirectCommandLayout.drawStride(command.stride())
        );
    }

    @Override
    public void multiDrawIndexedIndirect(RhiMultiDrawIndexedIndirectCommand command) {
        requireBackendBuffer(command.buffer(), "Vulkan multiDrawIndexedIndirect");
        vkCmdDrawIndexedIndirect(
                commandBuffer,
                command.buffer().nativeHandle(),
                command.offset(),
                command.drawCount(),
                RhiIndirectCommandLayout.drawIndexedStride(command.stride())
        );
    }

    @Override
    public void multiDrawIndirectCount(RhiMultiDrawIndirectCountCommand command) {
        requireBackendBuffer(command.buffer(), "Vulkan multiDrawIndirectCount");
        requireBackendBuffer(command.countBuffer(), "Vulkan multiDrawIndirectCount");
        vkCmdDrawIndirectCount(
                commandBuffer,
                command.buffer().nativeHandle(),
                command.offset(),
                command.countBuffer().nativeHandle(),
                command.countOffset(),
                command.maxDrawCount(),
                RhiIndirectCommandLayout.drawStride(command.stride())
        );
    }

    @Override
    public void multiDrawIndexedIndirectCount(RhiMultiDrawIndexedIndirectCountCommand command) {
        requireBackendBuffer(command.buffer(), "Vulkan multiDrawIndexedIndirectCount");
        requireBackendBuffer(command.countBuffer(), "Vulkan multiDrawIndexedIndirectCount");
        vkCmdDrawIndexedIndirectCount(
                commandBuffer,
                command.buffer().nativeHandle(),
                command.offset(),
                command.countBuffer().nativeHandle(),
                command.countOffset(),
                command.maxDrawCount(),
                RhiIndirectCommandLayout.drawIndexedStride(command.stride())
        );
    }

    long handle() {
        return commandBuffer.address();
    }

    RhiQueueType queueType() {
        return queueType;
    }

    private void requireBackendBuffer(com.github.slmpc.prismrhi.resource.RhiBuffer buffer, String operation) {
        if (buffer.api() != BackendApi.VULKAN) {
            throw new RhiException(operation + " requires a Vulkan buffer");
        }
    }

    private void requireBackendImage(RhiImage image, String operation) {
        if (image.api() != BackendApi.VULKAN) {
            throw new RhiException(operation + " requires a Vulkan image");
        }
    }

    private void requireMultiDrawSupport(String operation) {
        if (!multiDrawSupported) {
            throw new RhiException(operation + " requires VK_EXT_multi_draw");
        }
    }

    private static void fillImageBarrier(VkImageMemoryBarrier target, RhiImageBarrier barrier) {
        if (barrier.image().api() != BackendApi.VULKAN) {
            throw new RhiException("Vulkan image barrier requires a Vulkan image");
        }
        target
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .srcAccessMask(VulkanSupport.accessMask(barrier.oldState()))
                .dstAccessMask(VulkanSupport.accessMask(barrier.newState()))
                .oldLayout(VulkanSupport.imageLayout(barrier.oldState()))
                .newLayout(VulkanSupport.imageLayout(barrier.newState()))
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(barrier.image().nativeHandle());
        target.subresourceRange()
                .aspectMask(VulkanSupport.imageAspect(barrier.aspects()))
                .baseMipLevel(barrier.baseMipLevel())
                .levelCount(barrier.levelCount())
                .baseArrayLayer(barrier.baseArrayLayer())
                .layerCount(barrier.layerCount());
    }

    private static void fillBufferBarrier(VkBufferMemoryBarrier target, RhiBufferBarrier barrier) {
        if (barrier.buffer().api() != BackendApi.VULKAN) {
            throw new RhiException("Vulkan buffer barrier requires a Vulkan buffer");
        }
        target
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
                .srcAccessMask(VulkanSupport.accessMask(barrier.oldState()))
                .dstAccessMask(VulkanSupport.accessMask(barrier.newState()))
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(barrier.buffer().nativeHandle())
                .offset(barrier.offset())
                .size(barrier.size() == 0 ? barrier.buffer().size() : barrier.size());
    }

    private static int sourceStages(RhiPipelineBarrier barrier) {
        int stages = 0;
        for (RhiImageBarrier imageBarrier : barrier.imageBarriers()) {
            stages |= VulkanSupport.pipelineStage(imageBarrier.oldState());
        }
        for (RhiBufferBarrier bufferBarrier : barrier.bufferBarriers()) {
            stages |= VulkanSupport.pipelineStage(bufferBarrier.oldState());
        }
        return stages;
    }

    private static int destinationStages(RhiPipelineBarrier barrier) {
        int stages = 0;
        for (RhiImageBarrier imageBarrier : barrier.imageBarriers()) {
            stages |= VulkanSupport.pipelineStage(imageBarrier.newState());
        }
        for (RhiBufferBarrier bufferBarrier : barrier.bufferBarriers()) {
            stages |= VulkanSupport.pipelineStage(bufferBarrier.newState());
        }
        return stages;
    }

    private static void fillAttachment(VkRenderingAttachmentInfo attachmentInfo, RhiRenderingAttachment attachment) {
        if (attachment.view().api() != BackendApi.VULKAN) {
            throw new RhiException("Vulkan rendering attachment requires a Vulkan image view");
        }
        attachmentInfo
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(attachment.view().nativeHandle())
                .imageLayout(VulkanSupport.imageLayout(attachment.layout()))
                .resolveMode(0)
                .resolveImageView(NULL)
                .loadOp(VulkanSupport.loadOp(attachment.loadOp()))
                .storeOp(VulkanSupport.storeOp(attachment.storeOp()));
        fillClearValue(attachmentInfo.clearValue(), attachment);
    }

    private static void fillClearValue(org.lwjgl.vulkan.VkClearValue clearValue, RhiRenderingAttachment attachment) {
        RhiClearValue value = attachment.clearValue();
        if (attachment.layout() == RhiImageLayout.DEPTH_STENCIL_ATTACHMENT) {
            clearValue.depthStencil().depth(value.depth());
            clearValue.depthStencil().stencil(value.stencil());
            return;
        }
        clearValue.color().float32(0, value.r());
        clearValue.color().float32(1, value.g());
        clearValue.color().float32(2, value.b());
        clearValue.color().float32(3, value.a());
    }

    private static VkRect2D vkRect(RhiRect2D rect, MemoryStack stack) {
        VkRect2D result = VkRect2D.calloc(stack);
        fillRect(result, rect);
        return result;
    }

    private static void fillRect(VkRect2D target, RhiRect2D rect) {
        target.offset()
                .x(rect.offset().x())
                .y(rect.offset().y());
        target.extent()
                .width(rect.extent().width())
                .height(rect.extent().height());
    }

    private static int indexType(RhiIndexType indexType) {
        return switch (indexType == null ? RhiIndexType.UINT32 : indexType) {
            case UINT16 -> VK_INDEX_TYPE_UINT16;
            case UINT32 -> VK_INDEX_TYPE_UINT32;
        };
    }

    private static final class RhiIndirectCommandLayout {
        private static final int DRAW_STRIDE = Integer.BYTES * 4;
        private static final int DRAW_INDEXED_STRIDE = Integer.BYTES * 5;

        private RhiIndirectCommandLayout() {
        }

        static int drawStride(int requestedStride) {
            return requestedStride == 0 ? DRAW_STRIDE : requestedStride;
        }

        static int drawIndexedStride(int requestedStride) {
            return requestedStride == 0 ? DRAW_INDEXED_STRIDE : requestedStride;
        }
    }
}
