package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.queue.RhiSubmitInfo;
import com.github.slmpc.prismrhi.sync.RhiPipelineStage;
import com.github.slmpc.prismrhi.sync.RhiSemaphore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_VERTEX_INPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;

final class VulkanQueue implements RhiQueue {
    private final RhiQueueType type;
    private final VkQueue queue;

    VulkanQueue(RhiQueueType type, VkQueue queue) {
        this.type = type;
        this.queue = queue;
    }

    @Override
    public RhiQueueType type() {
        return type;
    }

    @Override
    public long nativeHandle() {
        return queue.address();
    }

    @Override
    public void submit(RhiSubmitInfo submitInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var commandBuffers = stack.mallocPointer(submitInfo.commandBuffers().size());
            for (RhiCommandBuffer commandBuffer : submitInfo.commandBuffers()) {
                if (!(commandBuffer instanceof VulkanCommandBuffer vulkanCommandBuffer)) {
                    throw new RhiException("Cannot submit command buffer from another backend to Vulkan queue");
                }
                commandBuffers.put(vulkanCommandBuffer.handle());
            }
            commandBuffers.flip();

            LongBuffer waitSemaphores = null;
            IntBuffer waitStages = null;
            if (!submitInfo.waitSemaphores().isEmpty()) {
                waitSemaphores = stack.mallocLong(submitInfo.waitSemaphores().size());
                waitStages = stack.mallocInt(submitInfo.waitSemaphores().size());
                for (int i = 0; i < submitInfo.waitSemaphores().size(); i++) {
                    RhiSemaphore semaphore = submitInfo.waitSemaphores().get(i);
                    if (!(semaphore instanceof VulkanDevice.VulkanSemaphore vulkanSemaphore)) {
                        throw new RhiException("Vulkan queue wait requires Vulkan semaphores");
                    }
                    waitSemaphores.put(vulkanSemaphore.nativeHandle());
                    RhiPipelineStage stage = submitInfo.waitStages().isEmpty()
                            ? RhiPipelineStage.ALL_COMMANDS
                            : submitInfo.waitStages().get(i);
                    waitStages.put(pipelineStage(stage));
                }
                waitSemaphores.flip();
                waitStages.flip();
            }

            LongBuffer signalSemaphores = null;
            if (!submitInfo.signalSemaphores().isEmpty()) {
                signalSemaphores = stack.mallocLong(submitInfo.signalSemaphores().size());
                for (RhiSemaphore semaphore : submitInfo.signalSemaphores()) {
                    if (!(semaphore instanceof VulkanDevice.VulkanSemaphore vulkanSemaphore)) {
                        throw new RhiException("Vulkan queue signal requires Vulkan semaphores");
                    }
                    signalSemaphores.put(vulkanSemaphore.nativeHandle());
                }
                signalSemaphores.flip();
            }

            VkSubmitInfo submit = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(waitSemaphores == null ? 0 : waitSemaphores.remaining())
                    .pWaitSemaphores(waitSemaphores)
                    .pWaitDstStageMask(waitStages)
                    .pCommandBuffers(commandBuffers)
                    .pSignalSemaphores(signalSemaphores);
            VulkanSupport.check(vkQueueSubmit(queue, submit, 0L), "vkQueueSubmit");
        }
    }

    @Override
    public void waitIdle() {
        VulkanSupport.check(vkQueueWaitIdle(queue), "vkQueueWaitIdle");
    }

    private static int pipelineStage(RhiPipelineStage stage) {
        return switch (stage == null ? RhiPipelineStage.ALL_COMMANDS : stage) {
            case TOP_OF_PIPE -> VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            case DRAW_INDIRECT -> VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT;
            case VERTEX_INPUT -> VK_PIPELINE_STAGE_VERTEX_INPUT_BIT;
            case VERTEX_SHADER -> VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
            case FRAGMENT_SHADER -> VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            case COLOR_ATTACHMENT_OUTPUT -> VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
            case TRANSFER -> VK_PIPELINE_STAGE_TRANSFER_BIT;
            case BOTTOM_OF_PIPE -> VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
            case ALL_COMMANDS -> VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
        };
    }
}
