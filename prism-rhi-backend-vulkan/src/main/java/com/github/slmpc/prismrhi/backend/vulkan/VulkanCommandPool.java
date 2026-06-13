package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiCommandPool;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_SECONDARY;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;

final class VulkanCommandPool implements RhiCommandPool {
    private final VkDevice device;
    private final RhiQueueType queueType;
    private final long handle;
    private final boolean multiDrawSupported;

    private VulkanCommandPool(VkDevice device, RhiQueueType queueType, long handle, boolean multiDrawSupported) {
        this.device = device;
        this.queueType = queueType;
        this.handle = handle;
        this.multiDrawSupported = multiDrawSupported;
    }

    static VulkanCommandPool create(
            VkDevice device,
            RhiQueueType queueType,
            int queueFamilyIndex,
            int flags,
            boolean multiDrawSupported
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(flags)
                    .queueFamilyIndex(queueFamilyIndex);
            LongBuffer commandPoolPointer = stack.longs(0L);
            VulkanSupport.check(vkCreateCommandPool(device, createInfo, null, commandPoolPointer), "vkCreateCommandPool");
            return new VulkanCommandPool(device, queueType, commandPoolPointer.get(0), multiDrawSupported);
        }
    }

    @Override
    public BackendApi api() {
        return BackendApi.VULKAN;
    }

    @Override
    public long nativeHandle() {
        return handle;
    }

    @Override
    public RhiCommandBuffer allocateCommandBuffer(RhiCommandBufferLevel level) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(handle)
                    .level(level == RhiCommandBufferLevel.SECONDARY
                            ? VK_COMMAND_BUFFER_LEVEL_SECONDARY
                            : VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            var commandBufferPointer = stack.mallocPointer(1);
            VulkanSupport.check(vkAllocateCommandBuffers(device, allocateInfo, commandBufferPointer), "vkAllocateCommandBuffers");
            return new VulkanCommandBuffer(device, commandBufferPointer.get(0), level, queueType, multiDrawSupported);
        }
    }

    @Override
    public void close() {
        vkDestroyCommandPool(device, handle, null);
    }
}
