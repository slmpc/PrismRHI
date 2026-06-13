package com.github.slmpc.prismrhi.backend.vulkan;

import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.context.RhiContext;
import com.github.slmpc.prismrhi.context.RhiContextCreateInfo;
import com.github.slmpc.prismrhi.context.RhiContextMode;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;

final class VulkanContext implements RhiContext {
    private final VkInstance instance;
    private final RhiContextMode mode;
    private final int width;
    private final int height;
    private final long window;
    private final long surface;
    private final boolean ownsWindow;
    private final boolean ownsSurface;
    private final boolean ownsGlfw;
    private boolean closed;

    private VulkanContext(
            VkInstance instance,
            RhiContextMode mode,
            int width,
            int height,
            long window,
            long surface,
            boolean ownsWindow,
            boolean ownsSurface,
            boolean ownsGlfw
    ) {
        this.instance = instance;
        this.mode = mode;
        this.width = width;
        this.height = height;
        this.window = window;
        this.surface = surface;
        this.ownsWindow = ownsWindow;
        this.ownsSurface = ownsSurface;
        this.ownsGlfw = ownsGlfw;
    }

    static GlfwPreparation prepareGlfw(RhiContextCreateInfo createInfo) {
        if (createInfo == null || !createInfo.needsGlfwSurface()) {
            return new GlfwPreparation(Set.of(), false);
        }
        boolean initializedByRhi = false;
        if (createInfo.mode() == RhiContextMode.AUTO_GLFW_WINDOW) {
            if (!glfwInit()) {
                throw new RhiException("glfwInit failed");
            }
            initializedByRhi = true;
        }
        if (!glfwVulkanSupported()) {
            throw new RhiException("GLFW reports that Vulkan is not supported");
        }
        PointerBuffer requiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        if (requiredExtensions == null) {
            throw new RhiException("GLFW did not provide Vulkan instance extensions");
        }
        Set<String> extensions = new LinkedHashSet<>();
        for (int i = 0; i < requiredExtensions.capacity(); i++) {
            extensions.add(requiredExtensions.getStringUTF8(i));
        }
        return new GlfwPreparation(extensions, initializedByRhi);
    }

    static VulkanContext create(VkInstance instance, RhiContextCreateInfo createInfo, boolean ownsGlfw) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long window = createInfo.windowHandle();
            if (createInfo.mode() == RhiContextMode.AUTO_GLFW_WINDOW) {
                glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
                glfwWindowHint(GLFW_RESIZABLE, createInfo.resizable() ? GLFW_TRUE : GLFW_FALSE);
                glfwWindowHint(GLFW_VISIBLE, createInfo.visible() ? GLFW_TRUE : GLFW_FALSE);
                window = glfwCreateWindow(createInfo.width(), createInfo.height(), createInfo.title(), NULL, NULL);
                if (window == NULL) {
                    throw new RhiException("glfwCreateWindow failed");
                }
            }

            long surface = createInfo.surfaceHandle();
            if (createInfo.needsGlfwSurface()) {
                var surfacePointer = stack.longs(0L);
                VulkanSupport.check(
                        GLFWVulkan.glfwCreateWindowSurface(instance, window, null, surfacePointer),
                        "glfwCreateWindowSurface"
                );
                surface = surfacePointer.get(0);
            }

            return new VulkanContext(
                    instance,
                    createInfo.mode(),
                    createInfo.width(),
                    createInfo.height(),
                    window,
                    surface,
                    createInfo.ownsWindow(),
                    createInfo.ownsSurface(),
                    ownsGlfw
            );
        }
    }

    @Override
    public BackendApi api() {
        return BackendApi.VULKAN;
    }

    @Override
    public RhiContextMode mode() {
        return mode;
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
    public long nativeWindowHandle() {
        return window;
    }

    @Override
    public long nativeSurfaceHandle() {
        return surface;
    }

    @Override
    public boolean shouldClose() {
        return window != NULL && glfwWindowShouldClose(window);
    }

    @Override
    public void pollEvents() {
        if (window != NULL) {
            glfwPollEvents();
        }
    }

    @Override
    public void requestClose() {
        if (window != NULL) {
            glfwSetWindowShouldClose(window, true);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        if (ownsSurface && surface != NULL) {
            vkDestroySurfaceKHR(instance, surface, null);
        }
        if (ownsWindow && window != NULL) {
            glfwDestroyWindow(window);
        }
        if (ownsGlfw) {
            glfwTerminate();
        }
        closed = true;
    }

    record GlfwPreparation(Set<String> requiredInstanceExtensions, boolean initializedByRhi) {
        GlfwPreparation {
            requiredInstanceExtensions = Set.copyOf(requiredInstanceExtensions);
        }
    }
}
