package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.VK_QUEUE_COMPUTE_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

final class VulkanQueueFamily {
    private VulkanQueueFamily() {
    }

    static Map<RhiQueueType, Integer> select(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null);
            VkQueueFamilyProperties.Buffer properties = VkQueueFamilyProperties.calloc(count.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, properties);

            Map<RhiQueueType, Integer> families = new EnumMap<>(RhiQueueType.class);
            for (int i = 0; i < properties.capacity(); i++) {
                int flags = properties.get(i).queueFlags();
                if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    families.putIfAbsent(RhiQueueType.GRAPHICS, i);
                }
                if ((flags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    families.putIfAbsent(RhiQueueType.COMPUTE, i);
                }
                if ((flags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    families.putIfAbsent(RhiQueueType.TRANSFER, i);
                }
            }
            if (!families.containsKey(RhiQueueType.GRAPHICS)) {
                throw new RhiException("No Vulkan graphics queue family found");
            }
            families.putIfAbsent(RhiQueueType.COMPUTE, families.get(RhiQueueType.GRAPHICS));
            families.putIfAbsent(RhiQueueType.TRANSFER, families.get(RhiQueueType.GRAPHICS));
            return families;
        }
    }
}
