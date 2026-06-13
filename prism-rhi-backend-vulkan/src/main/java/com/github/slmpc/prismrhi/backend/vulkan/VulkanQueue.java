package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.queue.RhiSubmitInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

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

            VkSubmitInfo submit = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(commandBuffers);
            VulkanSupport.check(vkQueueSubmit(queue, submit, 0L), "vkQueueSubmit");
        }
    }

    @Override
    public void waitIdle() {
        VulkanSupport.check(vkQueueWaitIdle(queue), "vkQueueWaitIdle");
    }
}
