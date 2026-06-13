package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.context.RhiContext;
import com.github.slmpc.prismrhi.format.RhiExtent3D;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.swapchain.RhiSwapchain;
import com.github.slmpc.prismrhi.swapchain.RhiSwapchainCreateInfo;
import com.github.slmpc.prismrhi.swapchain.RhiSwapchainImage;
import com.github.slmpc.prismrhi.sync.RhiSemaphore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;

final class VulkanSwapchain implements RhiSwapchain {
    private final VkInstance instance;
    private final VkDevice device;
    private final RhiContext context;
    private final long handle;
    private final int width;
    private final int height;
    private final RhiFormat format;
    private final List<RhiSwapchainImage> images;
    private boolean closed;

    private VulkanSwapchain(
            VkInstance instance,
            VkDevice device,
            RhiContext context,
            long handle,
            int width,
            int height,
            RhiFormat format,
            List<RhiSwapchainImage> images
    ) {
        this.instance = instance;
        this.device = device;
        this.context = context;
        this.handle = handle;
        this.width = width;
        this.height = height;
        this.format = format;
        this.images = List.copyOf(images);
    }

    static VulkanSwapchain create(
            VkInstance instance,
            VkPhysicalDevice physicalDevice,
            VkDevice device,
            RhiSwapchainCreateInfo createInfo,
            int graphicsFamily,
            int presentFamily
    ) {
        RhiContext context = createInfo.context();
        if (context.api() != BackendApi.VULKAN) {
            throw new RhiException("Vulkan swapchain requires a Vulkan context");
        }
        if (context.nativeSurfaceHandle() == NULL) {
            throw new RhiException("Vulkan swapchain requires a native surface");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            VulkanSupport.check(
                    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, context.nativeSurfaceHandle(), capabilities),
                    "vkGetPhysicalDeviceSurfaceCapabilitiesKHR"
            );

            VkSurfaceFormatKHR surfaceFormat = chooseSurfaceFormat(physicalDevice, context.nativeSurfaceHandle(), createInfo, stack);
            int presentMode = choosePresentMode(physicalDevice, context.nativeSurfaceHandle(), createInfo.vsync(), stack);
            int width = clampExtent(createInfo.width(), capabilities.minImageExtent().width(), capabilities.maxImageExtent().width());
            int height = clampExtent(createInfo.height(), capabilities.minImageExtent().height(), capabilities.maxImageExtent().height());
            if (capabilities.currentExtent().width() != 0xFFFFFFFF) {
                width = capabilities.currentExtent().width();
                height = capabilities.currentExtent().height();
            }

            int imageCount = Math.max(createInfo.preferredImageCount(), capabilities.minImageCount());
            if (capabilities.maxImageCount() > 0) {
                imageCount = Math.min(imageCount, capabilities.maxImageCount());
            }

            IntBuffer queueFamilyIndices = null;
            int sharingMode = VK_SHARING_MODE_EXCLUSIVE;
            if (graphicsFamily != presentFamily) {
                queueFamilyIndices = stack.ints(graphicsFamily, presentFamily);
                sharingMode = VK_SHARING_MODE_CONCURRENT;
            }

            VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(context.nativeSurfaceHandle())
                    .minImageCount(imageCount)
                    .imageFormat(surfaceFormat.format())
                    .imageColorSpace(surfaceFormat.colorSpace())
                    .imageExtent(extent(width, height, stack))
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .imageSharingMode(sharingMode)
                    .pQueueFamilyIndices(queueFamilyIndices)
                    .preTransform(capabilities.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(NULL);

            LongBuffer swapchainPointer = stack.longs(0L);
            VulkanSupport.check(vkCreateSwapchainKHR(device, swapchainCreateInfo, null, swapchainPointer), "vkCreateSwapchainKHR");
            long swapchain = swapchainPointer.get(0);
            RhiFormat rhiFormat = VulkanSupport.rhiFormat(surfaceFormat.format());
            List<RhiSwapchainImage> images = createImages(device, swapchain, width, height, rhiFormat, stack);
            return new VulkanSwapchain(instance, device, context, swapchain, width, height, rhiFormat, images);
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
    public RhiContext context() {
        return context;
    }

    @Override
    public RhiFormat format() {
        return format;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public List<RhiSwapchainImage> images() {
        return images;
    }

    @Override
    public int acquireNextImage(RhiSemaphore imageAvailableSemaphore) {
        if (!(imageAvailableSemaphore instanceof VulkanDevice.VulkanSemaphore semaphore)) {
            throw new RhiException("Vulkan acquireNextImage requires a Vulkan semaphore");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer imageIndex = stack.ints(0);
            int result = vkAcquireNextImageKHR(device, handle, Long.MAX_VALUE, semaphore.nativeHandle(), NULL, imageIndex);
            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                throw new RhiException("Vulkan swapchain is out of date");
            }
            if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                VulkanSupport.check(result, "vkAcquireNextImageKHR");
            }
            return imageIndex.get(0);
        }
    }

    @Override
    public void present(RhiQueue queue, int imageIndex, RhiSemaphore waitSemaphore) {
        if (!(waitSemaphore instanceof VulkanDevice.VulkanSemaphore semaphore)) {
            throw new RhiException("Vulkan present requires a Vulkan semaphore");
        }
        org.lwjgl.vulkan.VkQueue vkQueue = new org.lwjgl.vulkan.VkQueue(queue.nativeHandle(), device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(stack.longs(semaphore.nativeHandle()))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(handle))
                    .pImageIndices(stack.ints(imageIndex));
            int result = vkQueuePresentKHR(vkQueue, presentInfo);
            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                throw new RhiException("Vulkan swapchain is out of date");
            }
            if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                VulkanSupport.check(result, "vkQueuePresentKHR");
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        for (RhiSwapchainImage image : images) {
            image.view().close();
        }
        vkDestroySwapchainKHR(device, handle, null);
        closed = true;
    }

    private static VkSurfaceFormatKHR chooseSurfaceFormat(
            VkPhysicalDevice physicalDevice,
            long surface,
            RhiSwapchainCreateInfo createInfo,
            MemoryStack stack
    ) {
        IntBuffer count = stack.ints(0);
        VulkanSupport.check(
                vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, null),
                "vkGetPhysicalDeviceSurfaceFormatsKHR(count)"
        );
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.calloc(count.get(0), stack);
        VulkanSupport.check(
                vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, count, formats),
                "vkGetPhysicalDeviceSurfaceFormatsKHR"
        );
        int preferredFormat = VulkanSupport.format(createInfo.preferredFormat());
        for (int i = 0; i < formats.capacity(); i++) {
            VkSurfaceFormatKHR candidate = formats.get(i);
            if (candidate.format() == preferredFormat && candidate.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return candidate;
            }
        }
        for (int i = 0; i < formats.capacity(); i++) {
            VkSurfaceFormatKHR candidate = formats.get(i);
            if (candidate.format() == preferredFormat) {
                return candidate;
            }
        }
        return formats.get(0);
    }

    private static int choosePresentMode(VkPhysicalDevice physicalDevice, long surface, boolean vsync, MemoryStack stack) {
        if (vsync) {
            return VK_PRESENT_MODE_FIFO_KHR;
        }
        IntBuffer count = stack.ints(0);
        VulkanSupport.check(
                vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, null),
                "vkGetPhysicalDeviceSurfacePresentModesKHR(count)"
        );
        IntBuffer modes = stack.mallocInt(count.get(0));
        VulkanSupport.check(
                vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, count, modes),
                "vkGetPhysicalDeviceSurfacePresentModesKHR"
        );
        for (int i = 0; i < modes.capacity(); i++) {
            if (modes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return VK_PRESENT_MODE_MAILBOX_KHR;
            }
        }
        return VK_PRESENT_MODE_FIFO_KHR;
    }

    private static List<RhiSwapchainImage> createImages(
            VkDevice device,
            long swapchain,
            int width,
            int height,
            RhiFormat format,
            MemoryStack stack
    ) {
        IntBuffer count = stack.ints(0);
        VulkanSupport.check(vkGetSwapchainImagesKHR(device, swapchain, count, null), "vkGetSwapchainImagesKHR(count)");
        LongBuffer handles = stack.mallocLong(count.get(0));
        VulkanSupport.check(vkGetSwapchainImagesKHR(device, swapchain, count, handles), "vkGetSwapchainImagesKHR");

        List<RhiSwapchainImage> images = new ArrayList<>(count.get(0));
        for (int i = 0; i < count.get(0); i++) {
            long imageHandle = handles.get(i);
            var image = new VulkanDevice.VulkanImage(
                    0L,
                    imageHandle,
                    0L,
                    RhiExtent3D.of2D(width, height),
                    format
            );
            RhiImageView view = createImageView(device, imageHandle, image, format, stack);
            images.add(new RhiSwapchainImage(i, image, view));
        }
        return images;
    }

    private static RhiImageView createImageView(
            VkDevice device,
            long imageHandle,
            VulkanDevice.VulkanImage image,
            RhiFormat format,
            MemoryStack stack
    ) {
        VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc(stack)
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);
        VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(imageHandle)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(VulkanSupport.format(format))
                .subresourceRange(subresourceRange);
        LongBuffer viewPointer = stack.longs(0L);
        VulkanSupport.check(vkCreateImageView(device, createInfo, null, viewPointer), "vkCreateImageView");
        return new VulkanSwapchainImageView(device, viewPointer.get(0), image, format);
    }

    private static VkExtent2D extent(int width, int height, MemoryStack stack) {
        return VkExtent2D.calloc(stack).width(width).height(height);
    }

    private static int clampExtent(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private record VulkanSwapchainImageView(
            VkDevice device,
            long nativeHandle,
            VulkanDevice.VulkanImage image,
            RhiFormat format
    ) implements RhiImageView {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public java.util.Set<RhiImageAspect> aspects() {
            return EnumSet.of(RhiImageAspect.COLOR);
        }

        @Override
        public void close() {
            vkDestroyImageView(device, nativeHandle, null);
        }
    }
}
