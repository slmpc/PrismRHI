package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.backend.BackendInfo;
import com.github.slmpc.prismrhi.context.RhiContext;
import com.github.slmpc.prismrhi.device.RhiDevice;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.instance.RhiInstance;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;
import com.github.slmpc.prismrhi.device.RhiPhysicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRPortabilityEnumeration.VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.VK_FALSE;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceProperties;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

final class VulkanInstance implements RhiInstance {
    private static final String VALIDATION_LAYER = "VK_LAYER_KHRONOS_validation";

    private final BackendInfo backendInfo;
    private final VkInstance instance;
    private final VulkanContext context;
    private final long debugMessenger;
    private final VkDebugUtilsMessengerCallbackEXT debugCallback;
    private boolean closed;

    private VulkanInstance(
            BackendInfo backendInfo,
            VkInstance instance,
            VulkanContext context,
            long debugMessenger,
            VkDebugUtilsMessengerCallbackEXT debugCallback
    ) {
        this.backendInfo = backendInfo;
        this.instance = instance;
        this.context = context;
        this.debugMessenger = debugMessenger;
        this.debugCallback = debugCallback;
    }

    static VulkanInstance create(BackendInfo backendInfo, RhiInstanceCreateInfo createInfo) {
        VulkanContext.GlfwPreparation glfwPreparation = VulkanContext.prepareGlfw(createInfo.contextCreateInfo());
        Set<String> availableLayers = availableInstanceLayers();
        validateRequestedLayers(createInfo, availableLayers);

        Set<String> availableExtensions = availableInstanceExtensions(null);
        if (createInfo.enableValidation()) {
            availableExtensions.addAll(availableInstanceExtensions(VALIDATION_LAYER));
        }
        Set<String> enabledExtensions = enabledInstanceExtensions(createInfo, glfwPreparation, availableExtensions);
        boolean debugUtilsEnabled = createInfo.enableValidation()
                && enabledExtensions.contains(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        if (createInfo.enableValidation() && !debugUtilsEnabled) {
            System.err.println(
                    "[PrismRHI][Vulkan] VK_EXT_debug_utils is unavailable; validation is enabled without a debug messenger."
            );
        }
        VkDebugUtilsMessengerCallbackEXT debugCallback = debugUtilsEnabled
                ? VkDebugUtilsMessengerCallbackEXT.create(VulkanInstance::debugCallback)
                : null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8(createInfo.applicationName()))
                    .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(stack.UTF8("PrismRHI"))
                    .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK_API_VERSION_1_3);

            PointerBuffer extensions = null;
            if (!enabledExtensions.isEmpty()) {
                extensions = stack.mallocPointer(enabledExtensions.size());
                for (String extension : enabledExtensions) {
                    extensions.put(stack.UTF8(extension));
                }
                extensions.flip();
            }

            PointerBuffer layers = null;
            if (createInfo.enableValidation()) {
                layers = stack.mallocPointer(1);
                layers.put(stack.UTF8(VALIDATION_LAYER));
                layers.flip();
            }

            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = null;
            if (debugUtilsEnabled) {
                debugCreateInfo = debugMessengerCreateInfo(stack, debugCallback);
            }

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(extensions)
                    .ppEnabledLayerNames(layers);
            if (debugCreateInfo != null) {
                instanceCreateInfo.pNext(debugCreateInfo.address());
            }
            if (enabledExtensions.contains(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)) {
                instanceCreateInfo.flags(VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);
            }

            PointerBuffer instancePointer = stack.mallocPointer(1);
            VulkanSupport.check(vkCreateInstance(instanceCreateInfo, null, instancePointer), "vkCreateInstance");
            VkInstance instance = new VkInstance(instancePointer.get(0), instanceCreateInfo);
            long debugMessenger = createDebugMessenger(instance, debugCreateInfo, stack);
            VulkanContext context = createInfo.contextCreateInfo() == null
                    ? null
                    : VulkanContext.create(instance, createInfo.contextCreateInfo(), glfwPreparation.initializedByRhi());
            return new VulkanInstance(backendInfo, instance, context, debugMessenger, debugCallback);
        } catch (RuntimeException exception) {
            if (debugCallback != null) {
                debugCallback.free();
            }
            throw exception;
        }
    }

    private static Set<String> enabledInstanceExtensions(
            RhiInstanceCreateInfo createInfo,
            VulkanContext.GlfwPreparation glfwPreparation,
            Set<String> availableExtensions
    ) {
        Set<String> extensions = new LinkedHashSet<>(createInfo.enabledExtensions());
        if (createInfo.contextCreateInfo() != null) {
            extensions.addAll(createInfo.contextCreateInfo().requiredInstanceExtensions());
        }
        extensions.addAll(glfwPreparation.requiredInstanceExtensions());
        if (availableExtensions.contains(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME)) {
            extensions.add(VK_KHR_PORTABILITY_ENUMERATION_EXTENSION_NAME);
        }
        if (createInfo.enableValidation() && availableExtensions.contains(VK_EXT_DEBUG_UTILS_EXTENSION_NAME)) {
            extensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        }
        return extensions;
    }

    private static Set<String> availableInstanceExtensions(String layerName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer layerNameBuffer = layerName == null ? null : stack.UTF8(layerName);
            IntBuffer count = stack.ints(0);
            VulkanSupport.check(
                    vkEnumerateInstanceExtensionProperties(layerNameBuffer, count, null),
                    "vkEnumerateInstanceExtensionProperties(count)"
            );
            VkExtensionProperties.Buffer properties = VkExtensionProperties.calloc(count.get(0), stack);
            VulkanSupport.check(
                    vkEnumerateInstanceExtensionProperties(layerNameBuffer, count, properties),
                    "vkEnumerateInstanceExtensionProperties"
            );
            Set<String> extensions = new LinkedHashSet<>();
            for (int i = 0; i < properties.capacity(); i++) {
                extensions.add(properties.get(i).extensionNameString());
            }
            return extensions;
        }
    }

    private static Set<String> availableInstanceLayers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            VulkanSupport.check(
                    vkEnumerateInstanceLayerProperties(count, null),
                    "vkEnumerateInstanceLayerProperties(count)"
            );
            VkLayerProperties.Buffer properties = VkLayerProperties.calloc(count.get(0), stack);
            VulkanSupport.check(
                    vkEnumerateInstanceLayerProperties(count, properties),
                    "vkEnumerateInstanceLayerProperties"
            );
            Set<String> layers = new LinkedHashSet<>();
            for (int i = 0; i < properties.capacity(); i++) {
                layers.add(properties.get(i).layerNameString());
            }
            return layers;
        }
    }

    private static void validateRequestedLayers(RhiInstanceCreateInfo createInfo, Set<String> availableLayers) {
        if (!createInfo.enableValidation() || availableLayers.contains(VALIDATION_LAYER)) {
            return;
        }
        throw new RhiException(
                "Vulkan validation was requested, but " + VALIDATION_LAYER + " is not available. "
                        + "Install the Vulkan SDK and expose its explicit layer manifests, for example "
                        + "set VULKAN_SDK and VK_LAYER_PATH=$VULKAN_SDK/share/vulkan/explicit_layer.d on macOS."
        );
    }

    private static VkDebugUtilsMessengerCreateInfoEXT debugMessengerCreateInfo(
            MemoryStack stack,
            VkDebugUtilsMessengerCallbackEXT debugCallback
    ) {
        return VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(
                        VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                                | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
                                | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                                | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
                )
                .messageType(
                        VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                                | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                                | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                )
                .pfnUserCallback(debugCallback);
    }

    private static long createDebugMessenger(
            VkInstance instance,
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo,
            MemoryStack stack
    ) {
        if (debugCreateInfo == null) {
            return NULL;
        }
        var debugMessengerPointer = stack.longs(NULL);
        VulkanSupport.check(
                vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, debugMessengerPointer),
                "vkCreateDebugUtilsMessengerEXT"
        );
        return debugMessengerPointer.get(0);
    }

    private static int debugCallback(int severity, int type, long callbackDataAddress, long userData) {
        VkDebugUtilsMessengerCallbackDataEXT callbackData =
                VkDebugUtilsMessengerCallbackDataEXT.create(callbackDataAddress);
        System.err.printf(
                "[PrismRHI][Vulkan][%s][%s] %s%n",
                severityName(severity),
                messageTypeName(type),
                callbackData.pMessageString()
        );
        return VK_FALSE;
    }

    private static String severityName(int severity) {
        if ((severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            return "ERROR";
        }
        if ((severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            return "WARN";
        }
        if ((severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
            return "INFO";
        }
        if ((severity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
            return "VERBOSE";
        }
        return "UNKNOWN";
    }

    private static String messageTypeName(int type) {
        StringBuilder builder = new StringBuilder();
        appendMessageType(builder, type, VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT, "GENERAL");
        appendMessageType(builder, type, VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT, "VALIDATION");
        appendMessageType(builder, type, VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT, "PERFORMANCE");
        return builder.isEmpty() ? "UNKNOWN" : builder.toString();
    }

    private static void appendMessageType(StringBuilder builder, int type, int bit, String name) {
        if ((type & bit) == 0) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('|');
        }
        builder.append(name);
    }

    @Override
    public BackendInfo backendInfo() {
        return backendInfo;
    }

    @Override
    public long nativeHandle() {
        return instance.address();
    }

    @Override
    public java.util.Optional<RhiContext> context() {
        return java.util.Optional.ofNullable(context);
    }

    @Override
    public RhiContext createContext(com.github.slmpc.prismrhi.context.RhiContextCreateInfo createInfo) {
        ensureOpen();
        return VulkanContext.create(instance, createInfo, false);
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
        RhiContext effectiveContext = createInfo.context() != null ? createInfo.context() : context;
        return VulkanDevice.create(instance, vulkanPhysicalDevice, createInfo, effectiveContext);
    }

    @Override
    public void close() {
        if (!closed) {
            if (context != null) {
                context.close();
            }
            if (debugMessenger != NULL) {
                vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
            }
            if (debugCallback != null) {
                debugCallback.free();
            }
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
