package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

final class VulkanInstance implements RhiInstance {
    private final BackendInfo backendInfo;
    private final VkInstance instance;
    private boolean closed;

    private VulkanInstance(BackendInfo backendInfo, VkInstance instance) {
        this.backendInfo = backendInfo;
        this.instance = instance;
    }

    static VulkanInstance create(BackendInfo backendInfo, RhiInstanceCreateInfo createInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8(createInfo.applicationName()))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(stack.UTF8("PrismRHI"))
                    .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK_API_VERSION_1_3);

            PointerBuffer extensions = null;
            if (!createInfo.enabledExtensions().isEmpty()) {
                extensions = stack.mallocPointer(createInfo.enabledExtensions().size());
                for (String extension : createInfo.enabledExtensions()) {
                    extensions.put(stack.UTF8(extension));
                }
                extensions.flip();
            }

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(extensions);

            PointerBuffer instancePointer = stack.mallocPointer(1);
            VulkanSupport.check(vkCreateInstance(instanceCreateInfo, null, instancePointer), "vkCreateInstance");
            return new VulkanInstance(backendInfo, new VkInstance(instancePointer.get(0), instanceCreateInfo));
        }
    }

    @Override
    public BackendInfo backendInfo() {
        return backendInfo;
    }

    @Override
    public List<RhiPhysicalDevice> enumeratePhysicalDevices() {
        ensureOpen();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            VulkanSupport.check(vkEnumeratePhysicalDevices(instance, count, null), "vkEnumeratePhysicalDevices(count)");
            PointerBuffer devices = stack.mallocPointer(count.get(0));
            VulkanSupport.check(vkEnumeratePhysicalDevices(instance, count, devices), "vkEnumeratePhysicalDevices");

            List<RhiPhysicalDevice> physicalDevices = new ArrayList<>(count.get(0));
            for (int i = 0; i < count.get(0); i++) {
                VkPhysicalDevice physicalDevice = new VkPhysicalDevice(devices.get(i), instance);
                VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(physicalDevice, properties);
                physicalDevices.add(new VulkanPhysicalDevice(
                        physicalDevice,
                        properties.deviceNameString(),
                        VulkanSupport.physicalDeviceType(properties.deviceType()),
                        properties.vendorID(),
                        properties.deviceID(),
                        Set.of(
                                BackendFeature.EXPLICIT_MEMORY,
                                BackendFeature.EXPLICIT_SYNCHRONIZATION,
                                BackendFeature.SPIRV_SHADER_MODULES,
                                BackendFeature.COMMAND_BUFFERS
                        )
                ));
            }
            return List.copyOf(physicalDevices);
        }
    }

    @Override
    public RhiDevice createDevice(RhiPhysicalDevice physicalDevice, RhiDeviceCreateInfo createInfo) {
        ensureOpen();
        if (!(physicalDevice instanceof VulkanPhysicalDevice vulkanPhysicalDevice)) {
            throw new RhiException("Physical device was not created by this Vulkan instance");
        }
        return VulkanDevice.create(instance, vulkanPhysicalDevice, createInfo);
    }

    @Override
    public void close() {
        if (!closed) {
            vkDestroyInstance(instance, null);
            closed = true;
        }
    }

    VkInstance handle() {
        return instance;
    }

    private void ensureOpen() {
        if (closed || instance.address() == NULL) {
            throw new RhiException("Vulkan instance is closed");
        }
    }
}
