package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorBinding;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorBindingFlag;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorLayoutFlag;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetAllocateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorStage;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorType;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBindingFlagsCreateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDescriptorSetVariableDescriptorCountAllocateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_GENERAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_ALL;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_ALL_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_COMPUTE_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO;

final class VulkanDescriptors {
    private VulkanDescriptors() {
    }

    static VulkanDescriptorSetLayout createLayout(
            VkDevice device,
            RhiDescriptorSetLayoutCreateInfo createInfo
    ) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(createInfo.bindings().size(), stack);
            IntBuffer bindingFlags = stack.callocInt(createInfo.bindings().size());
            for (int i = 0; i < createInfo.bindings().size(); i++) {
                RhiDescriptorBinding binding = createInfo.bindings().get(i);
                bindings.get(i)
                        .binding(binding.binding())
                        .descriptorType(descriptorType(binding.type()))
                        .descriptorCount(binding.count())
                        .stageFlags(stageFlags(binding.stages()));
                bindingFlags.put(i, bindingFlags(binding.flags()));
            }

            VkDescriptorSetLayoutBindingFlagsCreateInfo bindingFlagsCreateInfo =
                    VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO)
                            .pBindingFlags(bindingFlags);

            int flags = 0;
            if (createInfo.flags().contains(RhiDescriptorLayoutFlag.UPDATE_AFTER_BIND_POOL)) {
                flags |= VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT;
            }

            VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pNext(bindingFlagsCreateInfo)
                    .flags(flags)
                    .pBindings(bindings);

            LongBuffer layoutPointer = stack.longs(0L);
            VulkanSupport.check(vkCreateDescriptorSetLayout(device, layoutCreateInfo, null, layoutPointer), "vkCreateDescriptorSetLayout");
            return new VulkanDescriptorSetLayout(device, layoutPointer.get(0), createInfo.bindings(), createInfo.flags().contains(RhiDescriptorLayoutFlag.UPDATE_AFTER_BIND_POOL));
        }
    }

    static VulkanDescriptorSet allocateSet(VkDevice device, RhiDescriptorSetAllocateInfo allocateInfo) {
        if (!(allocateInfo.layout() instanceof VulkanDescriptorSetLayout layout)) {
            throw new RhiException("Vulkan descriptor set allocation requires a Vulkan descriptor layout");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pool = createPool(device, layout, stack);
            LongBuffer layouts = stack.longs(layout.nativeHandle());
            VkDescriptorSetAllocateInfo setAllocateInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(pool)
                    .pSetLayouts(layouts);

            VkDescriptorSetVariableDescriptorCountAllocateInfo variableCountInfo = null;
            if (allocateInfo.variableDescriptorCount() > 0) {
                variableCountInfo = VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO)
                        .pDescriptorCounts(stack.ints(allocateInfo.variableDescriptorCount()));
                setAllocateInfo.pNext(variableCountInfo);
            }

            LongBuffer descriptorSetPointer = stack.longs(0L);
            VulkanSupport.check(vkAllocateDescriptorSets(device, setAllocateInfo, descriptorSetPointer), "vkAllocateDescriptorSets");
            return new VulkanDescriptorSet(device, pool, descriptorSetPointer.get(0), layout);
        }
    }

    private static long createPool(VkDevice device, VulkanDescriptorSetLayout layout, MemoryStack stack) {
        Map<RhiDescriptorType, Integer> counts = new EnumMap<>(RhiDescriptorType.class);
        for (RhiDescriptorBinding binding : layout.bindings()) {
            counts.merge(binding.type(), binding.count(), Integer::sum);
        }

        VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(counts.size(), stack);
        int index = 0;
        for (Map.Entry<RhiDescriptorType, Integer> entry : counts.entrySet()) {
            poolSizes.get(index)
                    .type(descriptorType(entry.getKey()))
                    .descriptorCount(entry.getValue());
            index++;
        }

        int flags = VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT;
        if (layout.updateAfterBindPool()) {
            flags |= VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT;
        }

        VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .flags(flags)
                .maxSets(1)
                .pPoolSizes(poolSizes);
        LongBuffer poolPointer = stack.longs(0L);
        VulkanSupport.check(vkCreateDescriptorPool(device, poolCreateInfo, null, poolPointer), "vkCreateDescriptorPool");
        return poolPointer.get(0);
    }

    private static int descriptorType(RhiDescriptorType type) {
        return switch (type) {
            case SAMPLER -> VK_DESCRIPTOR_TYPE_SAMPLER;
            case COMBINED_IMAGE_SAMPLER -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
            case SAMPLED_IMAGE -> VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE;
            case STORAGE_IMAGE -> VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
            case UNIFORM_BUFFER -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            case STORAGE_BUFFER -> VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
            case UNIFORM_BUFFER_DYNAMIC -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
            case STORAGE_BUFFER_DYNAMIC -> VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC;
            case INPUT_ATTACHMENT -> VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT;
        };
    }

    private static int stageFlags(java.util.Set<RhiDescriptorStage> stages) {
        int flags = 0;
        for (RhiDescriptorStage stage : stages) {
            flags |= switch (stage) {
                case VERTEX -> VK_SHADER_STAGE_VERTEX_BIT;
                case FRAGMENT -> VK_SHADER_STAGE_FRAGMENT_BIT;
                case COMPUTE -> VK_SHADER_STAGE_COMPUTE_BIT;
                case ALL_GRAPHICS -> VK_SHADER_STAGE_ALL_GRAPHICS;
                case ALL -> VK_SHADER_STAGE_ALL;
            };
        }
        return flags;
    }

    private static int bindingFlags(java.util.Set<RhiDescriptorBindingFlag> flags) {
        int result = 0;
        for (RhiDescriptorBindingFlag flag : flags) {
            result |= switch (flag) {
                case UPDATE_AFTER_BIND -> VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT;
                case UPDATE_UNUSED_WHILE_PENDING -> VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT;
                case PARTIALLY_BOUND -> VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT;
                case VARIABLE_DESCRIPTOR_COUNT -> VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT;
            };
        }
        return result;
    }

    record VulkanDescriptorSetLayout(
            VkDevice device,
            long nativeHandle,
            List<RhiDescriptorBinding> bindings,
            boolean updateAfterBindPool
    ) implements RhiDescriptorSetLayout {
        VulkanDescriptorSetLayout {
            bindings = List.copyOf(bindings);
        }

        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public void close() {
            vkDestroyDescriptorSetLayout(device, nativeHandle, null);
        }
    }

    static final class VulkanDescriptorSet implements RhiDescriptorSet {
        private final VkDevice device;
        private final long pool;
        private final long handle;
        private final VulkanDescriptorSetLayout layout;

        VulkanDescriptorSet(VkDevice device, long pool, long handle, VulkanDescriptorSetLayout layout) {
            this.device = device;
            this.pool = pool;
            this.handle = handle;
            this.layout = layout;
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
        public RhiDescriptorSetLayout layout() {
            return layout;
        }

        @Override
        public void update(Iterable<RhiDescriptorWrite> writes) {
            List<RhiDescriptorWrite> writeList = new java.util.ArrayList<>();
            for (RhiDescriptorWrite write : writes) {
                writeList.add(write);
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(writeList.size(), stack);
                VkDescriptorBufferInfo.Buffer bufferInfos = VkDescriptorBufferInfo.calloc(writeList.size(), stack);
                VkDescriptorImageInfo.Buffer imageInfos = VkDescriptorImageInfo.calloc(writeList.size(), stack);
                int bufferInfoIndex = 0;
                int imageInfoIndex = 0;
                for (int i = 0; i < writeList.size(); i++) {
                    RhiDescriptorWrite write = writeList.get(i);
                    descriptorWrites.get(i)
                            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                            .dstSet(handle)
                            .dstBinding(write.binding())
                            .dstArrayElement(write.arrayElement())
                            .descriptorType(descriptorType(write.type()))
                            .descriptorCount(1);

                    if (isBufferDescriptor(write.type())) {
                        if (write.buffer() == null || write.buffer().api() != BackendApi.VULKAN) {
                            throw new RhiException("Vulkan descriptor update requires Vulkan buffers");
                        }
                        VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.create(bufferInfos.address(bufferInfoIndex), 1)
                                .buffer(write.buffer().nativeHandle())
                                .offset(write.offset())
                                .range(write.range() == 0 ? VK_WHOLE_SIZE : write.range());
                        descriptorWrites.get(i).pBufferInfo(bufferInfo);
                        bufferInfoIndex++;
                    } else {
                        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.create(imageInfos.address(imageInfoIndex), 1)
                                .sampler(write.sampler() == null ? NULL : write.sampler().nativeHandle())
                                .imageView(write.imageView() == null ? NULL : write.imageView().nativeHandle())
                                .imageLayout(imageLayout(write.type()));
                        requireImageDescriptorResources(write);
                        descriptorWrites.get(i).pImageInfo(imageInfo);
                        imageInfoIndex++;
                    }
                }
                vkUpdateDescriptorSets(device, descriptorWrites, null);
            }
        }

        @Override
        public void close() {
            vkDestroyDescriptorPool(device, pool, null);
        }
    }

    private static boolean isBufferDescriptor(RhiDescriptorType type) {
        return switch (type) {
            case UNIFORM_BUFFER, STORAGE_BUFFER, UNIFORM_BUFFER_DYNAMIC, STORAGE_BUFFER_DYNAMIC -> true;
            case SAMPLER, COMBINED_IMAGE_SAMPLER, SAMPLED_IMAGE, STORAGE_IMAGE, INPUT_ATTACHMENT -> false;
        };
    }

    private static int imageLayout(RhiDescriptorType type) {
        return switch (type) {
            case STORAGE_IMAGE -> VK_IMAGE_LAYOUT_GENERAL;
            case SAMPLER -> VK_IMAGE_LAYOUT_UNDEFINED;
            case COMBINED_IMAGE_SAMPLER, SAMPLED_IMAGE, INPUT_ATTACHMENT -> VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
            case UNIFORM_BUFFER, STORAGE_BUFFER, UNIFORM_BUFFER_DYNAMIC, STORAGE_BUFFER_DYNAMIC ->
                    throw new RhiException("buffer descriptors do not use image layouts");
        };
    }

    private static void requireImageDescriptorResources(RhiDescriptorWrite write) {
        if ((write.type() == RhiDescriptorType.SAMPLER || write.type() == RhiDescriptorType.COMBINED_IMAGE_SAMPLER)
                && (write.sampler() == null || write.sampler().api() != BackendApi.VULKAN)) {
            throw new RhiException("Vulkan sampler descriptor update requires a Vulkan sampler");
        }
        if (write.type() != RhiDescriptorType.SAMPLER
                && (write.imageView() == null || write.imageView().api() != BackendApi.VULKAN)) {
            throw new RhiException("Vulkan image descriptor update requires a Vulkan image view");
        }
    }
}
