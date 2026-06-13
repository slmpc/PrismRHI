# 后端选择与加载

PrismRHI 的后端是模块化的。每个后端 jar 都在 `META-INF/services/com.github.slmpc.prismrhi.backend.RhiBackendProvider` 中注册 Provider，由 Java `ServiceLoader` 在运行时发现。

## 选择后端

```java
var instance = PrismRHI.createInstance(
        RhiInstanceCreateInfo.builder(BackendApi.VULKAN)
                .applicationName("Editor")
                .enableValidation(true)
                .build()
);
```

`BackendApi` 当前包含：

- `VULKAN`
- `OPENGL_41`
- `OPENGL_DSA`

如果 classpath 中没有对应后端模块，`createInstance` 会抛出 `RhiException`。

## 枚举可用后端

```java
for (var provider : PrismRHI.providers()) {
    var info = provider.info();
    System.out.printf(
            "%s (%s): supported=%s, features=%s%n",
            info.displayName(),
            info.api(),
            provider.isSupported(),
            info.features()
    );
}
```

`isSupported()` 用于判断当前进程是否具备后端需要的运行时条件。例如 OpenGL 后端通常要求当前线程已有 OpenGL context。

## Vulkan 后端

模块：`prism-rhi-backend-vulkan`

特性：

- 使用 LWJGL Vulkan。
- 使用 LWJGL VMA 创建和释放 buffer/image。
- 创建设备时启用 Vulkan 1.3、descriptor indexing 和 dynamic rendering 相关能力。
- Graphics pipeline 使用 `VkPipelineRenderingCreateInfo`，不使用传统 render pass。
- Dynamic Rendering 使用 `vkCmdBeginRendering` / `vkCmdEndRendering`。
- Frame graph barrier 映射到 `vkCmdPipelineBarrier`。
- 支持真实 multi-draw 时使用 `VK_EXT_multi_draw` 的 `vkCmdDrawMultiEXT` 和 `vkCmdDrawMultiIndexedEXT`。
- 可以通过 `RhiContextCreateInfo.autoGlfwWindow(...)` 自动创建 GLFW window 和 Vulkan surface。
- 也可以通过 `RhiContextCreateInfo.glfwWindow(...)`、`externalSurface(...)` 或 `externalVulkanSurface(...)` 传入外部创建的 window/surface。
- 可以通过 `RhiInstanceCreateInfo.builder(BackendApi.VULKAN).externalVulkanInstance(...)` 包装已经创建好的 `VkInstance`。
- `RhiDevice.createSwapchain(...)` 提供最小 swapchain/acquire/present 支持。
- `enableValidation(true)` 会启用 `VK_LAYER_KHRONOS_validation`，并在可用时创建 `VK_EXT_debug_utils` messenger，把 validation/performance/general 消息输出到 stderr。

包装外部 `VkInstance` 时，需要把该 instance 创建时已经启用的 instance extension 传给 builder；这些 extension 用于构造 LWJGL capabilities，不会重新创建 Vulkan object。外部传入的 instance/surface 默认不由 RHI 销毁。

```java
var contextInfo = RhiContextCreateInfo.externalVulkanSurface(vkSurface, width, height)
        .build();

var instance = PrismRHI.createInstance(
        RhiInstanceCreateInfo.builder(BackendApi.VULKAN)
                .externalVulkanInstance(vkInstance)
                .addExtension("VK_KHR_surface")
                .context(contextInfo)
                .build()
);
```

注意：

- 如果设备不支持 `VK_EXT_multi_draw`，调用非 indirect 的 `multiDraw` / `multiDrawIndexed` 会抛出 `RhiException`。
- Vulkan shader module 接受 SPIR-V。若传入 GLSL，需要 classpath 中存在 `prism-rhi-shaderc`。
- 在 macOS 开发环境中，通常需要设置 `VULKAN_SDK` 或 `-Pvulkan_sdk`，以便 Gradle run task 找到 Vulkan loader、MoltenVK ICD manifest 和 validation layer manifest。
- 当前同步抽象仍然保持简洁，队列提交以 command buffer 为主；更完整的 semaphore/fence/swapchain 抽象可后续扩展。

## OpenGL 4.1 后端

模块：`prism-rhi-backend-opengl41`

定位：

- 面向 GL 4.1 兼容环境。
- 使用 bind-to-edit 风格 API。
- 由应用负责创建并 make current OpenGL context，RHI 只包装当前 context。

能力与限制：

- 支持直接 draw、indexed draw、instanced draw。
- `multiDraw` 使用 `glMultiDrawArrays`。
- `multiDrawIndexed` 使用 `glMultiDrawElements`。
- 单 draw indirect 可映射到 OpenGL indirect draw。
- 对 OpenGL 4.1 不自然支持的 Vulkan 风格参数会抛出 `RhiException`，例如部分 `firstInstance`、indexed `vertexOffset`、multi indirect、indirect count。

## OpenGL DSA 后端

模块：`prism-rhi-backend-opengl-dsa`

定位：

- 面向现代 OpenGL 4.5。
- 使用 Direct State Access API，例如 `glCreateBuffers`、`glNamedBufferData`、`glCreateTextures`、`glTextureStorage2D`。
- 同样要求应用先创建并 make current OpenGL context。

能力：

- 支持现代 base-instance draw。
- `multiDraw` / `multiDrawIndexed` 使用真实 OpenGL multi-draw。
- 支持 multi indirect。
- indirect count 需要 OpenGL 4.6 或 `GL_ARB_indirect_parameters`。

## OpenGL Context 归属

OpenGL 后端不负责创建窗口和 context。典型流程：

1. 应用使用 GLFW、AWT 或其他窗口库创建 OpenGL context。
2. 在当前线程 make current。
3. 调用 `PrismRHI.createInstance(OPENGL_41)` 或 `PrismRHI.createInstance(OPENGL_DSA)`。
4. 使用 RHI 的 device、queue、command buffer 录制渲染命令。

这使 OpenGL 后端可以嵌入已有应用或引擎窗口系统，而不把 PrismRHI 绑定到某个窗口库。
