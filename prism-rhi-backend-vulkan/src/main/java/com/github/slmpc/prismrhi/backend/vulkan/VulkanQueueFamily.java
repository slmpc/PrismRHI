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
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;

final class VulkanQueueFamily {
    private VulkanQueueFamily() {
    }

    static Map<RhiQueueType, Integer> select(VkPhysicalDevice physicalDevice) {
        return select(physicalDevice, 0L);
    }

    static Map<RhiQueueType, Integer> select(VkPhysicalDevice physicalDevice, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null);
            VkQueueFamilyProperties.Buffer properties = VkQueueFamilyProperties.calloc(count.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, properties);

            Map<RhiQueueType, Integer> families = new EnumMap<>(RhiQueueType.class);
            int firstGraphics = -1;
            for (int i = 0; i < properties.capacity(); i++) {
                int flags = properties.get(i).queueFlags();
                if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    if (firstGraphics < 0) {
                        firstGraphics = i;
                    }
                    if (surface == 0L || supportsPresent(physicalDevice, i, surface, stack)) {
                        families.putIfAbsent(RhiQueueType.GRAPHICS, i);
                    }
                }
                if ((flags & VK_QUEUE_COMPUTE_BIT) != 0) {
                    families.putIfAbsent(RhiQueueType.COMPUTE, i);
                }
                if ((flags & VK_QUEUE_TRANSFER_BIT) != 0) {
                    families.putIfAbsent(RhiQueueType.TRANSFER, i);
                }
            }
            if (!families.containsKey(RhiQueueType.GRAPHICS)) {
                if (firstGraphics >= 0) {
                    families.put(RhiQueueType.GRAPHICS, firstGraphics);
                } else {
                    throw new RhiException("No Vulkan graphics queue family found");
                }
            }
            families.putIfAbsent(RhiQueueType.COMPUTE, families.get(RhiQueueType.GRAPHICS));
            families.putIfAbsent(RhiQueueType.TRANSFER, families.get(RhiQueueType.GRAPHICS));
            return families;
        }
    }

    static int presentFamily(VkPhysicalDevice physicalDevice, long surface, int preferredFamily) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (surface == 0L || supportsPresent(physicalDevice, preferredFamily, surface, stack)) {
                return preferredFamily;
            }
            IntBuffer count = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, null);
            for (int i = 0; i < count.get(0); i++) {
                if (supportsPresent(physicalDevice, i, surface, stack)) {
                    return i;
                }
            }
            throw new RhiException("No Vulkan present queue family found");
        }
    }

    private static boolean supportsPresent(
            VkPhysicalDevice physicalDevice,
            int queueFamilyIndex,
            long surface,
            MemoryStack stack
    ) {
        IntBuffer supported = stack.ints(0);
        VulkanSupport.check(
                vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, queueFamilyIndex, surface, supported),
                "vkGetPhysicalDeviceSurfaceSupportKHR"
        );
        return supported.get(0) != 0;
    }
}
