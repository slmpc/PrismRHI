package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiBufferCreateInfo;
import com.github.slmpc.prismrhi.command.RhiCommandPool;
import com.github.slmpc.prismrhi.command.RhiCommandPoolCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetAllocateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.format.RhiExtent3D;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.resource.RhiImageViewCreateInfo;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.resource.RhiSampler;
import com.github.slmpc.prismrhi.resource.RhiSamplerCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderCodeType;
import com.github.slmpc.prismrhi.shader.RhiShaderCompileOptions;
import com.github.slmpc.prismrhi.shader.RhiShaderCompilers;
import com.github.slmpc.prismrhi.shader.RhiShaderModule;
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceDescriptorIndexingFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceMultiDrawFeaturesEXT;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.util.vma.Vma.vmaCreateBuffer;
import static org.lwjgl.util.vma.Vma.vmaCreateImage;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.util.vma.Vma.vmaDestroyBuffer;
import static org.lwjgl.util.vma.Vma.vmaDestroyImage;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES;
import static org.lwjgl.vulkan.EXTMultiDraw.VK_EXT_MULTI_DRAW_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTMultiDraw.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTI_DRAW_FEATURES_EXT;

final class VulkanDevice implements RhiDevice {
    private final VkInstance instance;
    private final VulkanPhysicalDevice physicalDevice;
    private final VkDevice device;
    private final long allocator;
    private final Map<RhiQueueType, Integer> queueFamilies;
    private final Map<RhiQueueType, RhiQueue> queues = new EnumMap<>(RhiQueueType.class);
    private final boolean multiDrawSupported;
    private boolean closed;

    private VulkanDevice(
            VkInstance instance,
            VulkanPhysicalDevice physicalDevice,
            VkDevice device,
            long allocator,
            Map<RhiQueueType, Integer> queueFamilies,
            boolean multiDrawSupported
    ) {
        this.instance = instance;
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.allocator = allocator;
        this.queueFamilies = Map.copyOf(queueFamilies);
        this.multiDrawSupported = multiDrawSupported;
        createQueues();
    }

    static VulkanDevice create(VkInstance instance, VulkanPhysicalDevice physicalDevice, RhiDeviceCreateInfo createInfo) {
        Map<RhiQueueType, Integer> queueFamilies = VulkanQueueFamily.select(physicalDevice.handle());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Set<Integer> uniqueFamilies = new LinkedHashSet<>(queueFamilies.values());
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueFamilies.size(), stack);
            int queueInfoIndex = 0;
            for (int family : uniqueFamilies) {
                queueCreateInfos.get(queueInfoIndex)
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(family)
                        .pQueuePriorities(stack.floats(1.0f));
                queueInfoIndex++;
            }

            boolean multiDrawSupported = supportsExtension(physicalDevice.handle(), VK_EXT_MULTI_DRAW_EXTENSION_NAME);
            Set<String> enabledExtensions = new LinkedHashSet<>(createInfo.enabledExtensions());
            if (multiDrawSupported) {
                enabledExtensions.add(VK_EXT_MULTI_DRAW_EXTENSION_NAME);
            }

            var extensions = enabledExtensions.isEmpty() ? null : stack.mallocPointer(enabledExtensions.size());
            if (extensions != null) {
                for (String extension : enabledExtensions) {
                    extensions.put(stack.UTF8(extension));
                }
                extensions.flip();
            }

            VkPhysicalDeviceDescriptorIndexingFeatures descriptorIndexingFeatures =
                    VkPhysicalDeviceDescriptorIndexingFeatures.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES)
                            .shaderUniformBufferArrayNonUniformIndexing(true)
                            .shaderSampledImageArrayNonUniformIndexing(true)
                            .shaderStorageBufferArrayNonUniformIndexing(true)
                            .shaderStorageImageArrayNonUniformIndexing(true)
                            .descriptorBindingUniformBufferUpdateAfterBind(true)
                            .descriptorBindingSampledImageUpdateAfterBind(true)
                            .descriptorBindingStorageImageUpdateAfterBind(true)
                            .descriptorBindingStorageBufferUpdateAfterBind(true)
                            .descriptorBindingUpdateUnusedWhilePending(true)
                            .descriptorBindingPartiallyBound(true)
                            .descriptorBindingVariableDescriptorCount(true)
                            .runtimeDescriptorArray(true);
            VkPhysicalDeviceDynamicRenderingFeatures dynamicRenderingFeatures =
                    VkPhysicalDeviceDynamicRenderingFeatures.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES)
                            .pNext(descriptorIndexingFeatures.address())
                            .dynamicRendering(true);
            long featureChain = dynamicRenderingFeatures.address();
            VkPhysicalDeviceMultiDrawFeaturesEXT multiDrawFeatures = null;
            if (multiDrawSupported) {
                multiDrawFeatures = VkPhysicalDeviceMultiDrawFeaturesEXT.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTI_DRAW_FEATURES_EXT)
                        .pNext(featureChain)
                        .multiDraw(true);
                featureChain = multiDrawFeatures.address();
            }

            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pNext(featureChain)
                    .pQueueCreateInfos(queueCreateInfos)
                    .ppEnabledExtensionNames(extensions);

            var devicePointer = stack.mallocPointer(1);
            VulkanSupport.check(vkCreateDevice(physicalDevice.handle(), deviceCreateInfo, null, devicePointer), "vkCreateDevice");
            VkDevice device = new VkDevice(devicePointer.get(0), physicalDevice.handle(), deviceCreateInfo);
            long allocator = createAllocator(stack, instance, physicalDevice.handle(), device);
            return new VulkanDevice(instance, physicalDevice, device, allocator, queueFamilies, multiDrawSupported);
        }
    }

    private static boolean supportsExtension(VkPhysicalDevice physicalDevice, String extensionName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            VulkanSupport.check(
                    vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, null),
                    "vkEnumerateDeviceExtensionProperties(count)"
            );
            VkExtensionProperties.Buffer properties = VkExtensionProperties.calloc(count.get(0), stack);
            VulkanSupport.check(
                    vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, properties),
                    "vkEnumerateDeviceExtensionProperties"
            );
            for (int i = 0; i < properties.capacity(); i++) {
                if (extensionName.equals(properties.get(i).extensionNameString())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static long createAllocator(
            MemoryStack stack,
            VkInstance instance,
            VkPhysicalDevice physicalDevice,
            VkDevice device
    ) {
        VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.calloc(stack)
                .physicalDevice(physicalDevice)
                .device(device)
                .instance(instance)
                .vulkanApiVersion(VK_API_VERSION_1_3);
        var allocatorPointer = stack.mallocPointer(1);
        VulkanSupport.check(vmaCreateAllocator(allocatorCreateInfo, allocatorPointer), "vmaCreateAllocator");
        return allocatorPointer.get(0);
    }

    private void createQueues() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (Map.Entry<RhiQueueType, Integer> entry : queueFamilies.entrySet()) {
                var queuePointer = stack.mallocPointer(1);
                vkGetDeviceQueue(device, entry.getValue(), 0, queuePointer);
                queues.put(entry.getKey(), new VulkanQueue(entry.getKey(), new VkQueue(queuePointer.get(0), device)));
            }
        }
    }

    @Override
    public BackendApi api() {
        return BackendApi.VULKAN;
    }

    @Override
    public RhiQueue queue(RhiQueueType type) {
        ensureOpen();
        RhiQueue queue = queues.get(type);
        if (queue == null) {
            throw new RhiException("Vulkan device does not expose a " + type + " queue");
        }
        return queue;
    }

    @Override
    public RhiBuffer createBuffer(RhiBufferCreateInfo createInfo) {
        ensureOpen();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(createInfo.size())
                    .usage(VulkanSupport.bufferUsage(createInfo.usage()))
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            VmaAllocationCreateInfo allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VulkanSupport.memoryUsage(createInfo.memoryUsage()));

            LongBuffer bufferPointer = stack.longs(0L);
            var allocationPointer = stack.mallocPointer(1);
            VulkanSupport.check(
                    vmaCreateBuffer(allocator, bufferCreateInfo, allocationCreateInfo, bufferPointer, allocationPointer, null),
                    "vmaCreateBuffer"
            );
            return new VulkanBuffer(allocator, bufferPointer.get(0), allocationPointer.get(0), createInfo.size());
        }
    }

    @Override
    public RhiImage createImage(RhiImageCreateInfo createInfo) {
        ensureOpen();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent3D extent = VkExtent3D.calloc(stack)
                    .width(createInfo.extent().width())
                    .height(createInfo.extent().height())
                    .depth(createInfo.extent().depth());

            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .extent(extent)
                    .mipLevels(1)
                    .arrayLayers(1)
                    .format(VulkanSupport.format(createInfo.format()))
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .usage(VulkanSupport.imageUsage(createInfo.usage()))
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            VmaAllocationCreateInfo allocationCreateInfo = VmaAllocationCreateInfo.calloc(stack)
                    .usage(VulkanSupport.memoryUsage(createInfo.memoryUsage()));

            LongBuffer imagePointer = stack.longs(0L);
            var allocationPointer = stack.mallocPointer(1);
            VulkanSupport.check(
                    vmaCreateImage(allocator, imageCreateInfo, allocationCreateInfo, imagePointer, allocationPointer, null),
                    "vmaCreateImage"
            );
            return new VulkanImage(
                    allocator,
                    imagePointer.get(0),
                    allocationPointer.get(0),
                    createInfo.extent(),
                    createInfo.format()
            );
        }
    }

    @Override
    public RhiImageView createImageView(RhiImageViewCreateInfo createInfo) {
        ensureOpen();
        if (createInfo.image().api() != BackendApi.VULKAN) {
            throw new RhiException("Vulkan image view requires a Vulkan image");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.calloc(stack)
                    .aspectMask(VulkanSupport.imageAspect(createInfo.aspects()))
                    .baseMipLevel(createInfo.baseMipLevel())
                    .levelCount(createInfo.levelCount())
                    .baseArrayLayer(createInfo.baseArrayLayer())
                    .layerCount(createInfo.layerCount());
            VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(createInfo.image().nativeHandle())
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(VulkanSupport.format(createInfo.format()))
                    .subresourceRange(subresourceRange);

            LongBuffer imageViewPointer = stack.longs(0L);
            VulkanSupport.check(vkCreateImageView(device, imageViewCreateInfo, null, imageViewPointer), "vkCreateImageView");
            return new VulkanImageView(
                    device,
                    imageViewPointer.get(0),
                    createInfo.image(),
                    createInfo.format(),
                    createInfo.aspects()
            );
        }
    }

    @Override
    public RhiSampler createSampler(RhiSamplerCreateInfo createInfo) {
        ensureOpen();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VulkanSupport.filter(createInfo.magFilter()))
                    .minFilter(VulkanSupport.filter(createInfo.minFilter()))
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .addressModeU(VulkanSupport.samplerAddressMode(createInfo.addressModeU()))
                    .addressModeV(VulkanSupport.samplerAddressMode(createInfo.addressModeV()))
                    .addressModeW(VulkanSupport.samplerAddressMode(createInfo.addressModeW()))
                    .anisotropyEnable(createInfo.maxAnisotropy() > 0.0f)
                    .maxAnisotropy(Math.max(1.0f, createInfo.maxAnisotropy()))
                    .minLod(0.0f)
                    .maxLod(1000.0f);
            LongBuffer samplerPointer = stack.longs(0L);
            VulkanSupport.check(vkCreateSampler(device, samplerCreateInfo, null, samplerPointer), "vkCreateSampler");
            return new VulkanSampler(device, samplerPointer.get(0));
        }
    }

    @Override
    public RhiShaderModule createShaderModule(RhiShaderModuleCreateInfo createInfo) {
        ensureOpen();
        if (createInfo.codeType() == RhiShaderCodeType.GLSL) {
            createInfo = RhiShaderCompilers.compile(
                    createInfo,
                    RhiShaderCodeType.SPIRV,
                    RhiShaderCompileOptions.builder()
                            .sourceName(createInfo.stage().name().toLowerCase(java.util.Locale.ROOT) + ".glsl")
                            .build()
            );
        }
        if (createInfo.codeType() != RhiShaderCodeType.SPIRV) {
            throw new RhiException("Vulkan backend accepts SPIR-V shader modules or GLSL with prism-rhi-shaderc");
        }
        if (createInfo.code().length % Integer.BYTES != 0) {
            throw new RhiException("SPIR-V bytecode length must be 4-byte aligned");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer code = stack.malloc(createInfo.code().length);
            code.put(createInfo.code()).flip();
            VkShaderModuleCreateInfo shaderModuleCreateInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(code);

            LongBuffer shaderPointer = stack.longs(0L);
            VulkanSupport.check(vkCreateShaderModule(device, shaderModuleCreateInfo, null, shaderPointer), "vkCreateShaderModule");
            return new VulkanShaderModule(device, shaderPointer.get(0), createInfo.stage());
        }
    }

    @Override
    public RhiCommandPool createCommandPool(RhiCommandPoolCreateInfo createInfo) {
        ensureOpen();
        int flags = 0;
        if (createInfo.transientPool()) {
            flags |= VK_COMMAND_POOL_CREATE_TRANSIENT_BIT;
        }
        if (createInfo.resetCommandBuffer()) {
            flags |= VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
        }
        Integer family = queueFamilies.get(createInfo.queueType());
        if (family == null) {
            throw new RhiException("No queue family for " + createInfo.queueType());
        }
        return VulkanCommandPool.create(device, createInfo.queueType(), family, flags, multiDrawSupported);
    }

    @Override
    public RhiDescriptorSetLayout createDescriptorSetLayout(RhiDescriptorSetLayoutCreateInfo createInfo) {
        ensureOpen();
        return VulkanDescriptors.createLayout(device, createInfo);
    }

    @Override
    public RhiDescriptorSet allocateDescriptorSet(RhiDescriptorSetAllocateInfo allocateInfo) {
        ensureOpen();
        return VulkanDescriptors.allocateSet(device, allocateInfo);
    }

    @Override
    public RhiGraphicsPipeline createGraphicsPipeline(RhiGraphicsPipelineCreateInfo createInfo) {
        ensureOpen();
        return VulkanPipelines.createGraphicsPipeline(device, createInfo);
    }

    @Override
    public void waitIdle() {
        ensureOpen();
        VulkanSupport.check(vkDeviceWaitIdle(device), "vkDeviceWaitIdle");
    }

    @Override
    public void close() {
        if (!closed) {
            vkDeviceWaitIdle(device);
            vmaDestroyAllocator(allocator);
            vkDestroyDevice(device, null);
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed || device.address() == NULL) {
            throw new RhiException("Vulkan device is closed");
        }
    }

    private record VulkanBuffer(long allocator, long handle, long allocation, long size) implements RhiBuffer {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public void close() {
            vmaDestroyBuffer(allocator, handle, allocation);
        }
    }

    private record VulkanImage(long allocator, long handle, long allocation, RhiExtent3D extent,
                               RhiFormat format) implements RhiImage {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public void close() {
            vmaDestroyImage(allocator, handle, allocation);
        }
    }

    private record VulkanImageView(
            VkDevice device,
            long handle,
            RhiImage image,
            RhiFormat format,
            Set<RhiImageAspect> aspects
    ) implements RhiImageView {
        VulkanImageView {
            aspects = Set.copyOf(aspects);
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
        public void close() {
            vkDestroyImageView(device, handle, null);
        }
    }

    private record VulkanSampler(VkDevice device, long handle) implements RhiSampler {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public void close() {
            vkDestroySampler(device, handle, null);
        }
    }

    private record VulkanShaderModule(VkDevice device, long handle, RhiShaderStage stage) implements RhiShaderModule {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public long nativeHandle() {
            return handle;
        }

        @Override
        public RhiShaderCodeType codeType() {
            return RhiShaderCodeType.SPIRV;
        }

        @Override
        public void close() {
            vkDestroyShaderModule(device, handle, null);
        }
    }
}
