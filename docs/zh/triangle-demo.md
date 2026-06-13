# Vulkan 3D Demo

仓库内提供了一个窗口化 Vulkan 示例模块。模块名仍为 `prism-rhi-demo-triangle`，但当前示例已经从最初的 2D 三角形扩展为一个 3D 立方体，用于展示相机、buffer、descriptor、dynamic rendering 和 frame graph 的组合用法。

```bash
./gradlew :prism-rhi-demo-triangle:runTriangleDemo
```

在 macOS 上，Gradle 运行任务会自动加入 GLFW 需要的 `-XstartOnFirstThread` 参数。

启用 Vulkan 验证层：

```bash
VULKAN_SDK=/path/to/vulkan-sdk/macOS \
VULKAN_VALIDATION_LAYER=1 \
./gradlew :prism-rhi-demo-triangle:runTriangleDemo
```

也可以使用 Gradle 属性传入 SDK 路径：

```bash
./gradlew :prism-rhi-demo-triangle:runTriangleDemo \
  -Pvulkan_sdk=/path/to/vulkan-sdk/macOS \
  -Pvulkan_validation_layer=1
```

在 macOS 上，运行任务会根据 `VULKAN_SDK` 或 `-Pvulkan_sdk` 自动设置：

- `org.lwjgl.vulkan.libname=$VULKAN_SDK/lib/libvulkan.1.dylib`
- `VK_LAYER_PATH=$VULKAN_SDK/share/vulkan/explicit_layer.d`
- `VK_ICD_FILENAMES` / `VK_DRIVER_FILES=$VULKAN_SDK/share/vulkan/icd.d/MoltenVK_icd.json`

如果只想从命令行参数打开验证层，也可以运行：

```bash
VULKAN_SDK=/path/to/vulkan-sdk/macOS \
./gradlew :prism-rhi-demo-triangle:run --args=--vulkanValidation
```

当请求验证层但系统找不到 `VK_LAYER_KHRONOS_validation` 时，Vulkan 后端会直接抛出异常并提示检查 SDK/layer manifest 路径。

该 demo 使用 Vulkan 后端，并走 RHI 自动创建 context 的路径：

```java
RhiContextCreateInfo contextInfo =
        RhiContextCreateInfo.autoGlfwWindow(960, 540, "PrismRHI Vulkan Triangle")
                .resizable(false)
                .build();

var instance = PrismRHI.createInstance(
        RhiInstanceCreateInfo.builder(BackendApi.VULKAN)
                .applicationName("PrismRHI Triangle Demo")
                .enableValidation(enableValidation)
                .context(contextInfo)
                .build()
);
```

窗口标题和 application name 仍保留 `Triangle` 字样，这是示例模块早期名称的历史兼容；当前渲染内容是 3D 立方体。

demo 展示的功能：

- Vulkan 后端自动创建 GLFW window 和 Vulkan surface。
- 通过 `RhiDevice.createSwapchain(...)` 创建 swapchain。
- 使用 GLSL 编写 vertex/fragment shader，并通过 `prism-rhi-shaderc` 自动编译为 SPIR-V。
- 使用 JOML 构建 camera view-projection matrix，并通过 uniform buffer 传入 vertex shader。
- 创建并写入 vertex buffer、index buffer 和 uniform buffer。
- 使用 descriptor set 绑定 uniform buffer。
- 使用 Graphics Pipeline + Dynamic Rendering + depth attachment 绘制 3D。
- 使用内置 Frame Graph 声明 rendering pass，自动包裹 `beginRendering`、默认 viewport/scissor、`endRendering`，并自动插入 `UNDEFINED -> COLOR_ATTACHMENT -> PRESENT` 与 depth attachment 的 image barrier。
- 使用 semaphore 串接 acquire、submit 和 present。

也可以传入外部创建好的对象：

```java
var contextInfo = RhiContextCreateInfo.glfwWindow(windowHandle, width, height).build();
```

如果窗口系统已经创建了 Vulkan surface，可以使用：

```java
var contextInfo = RhiContextCreateInfo.externalSurface(surfaceHandle, width, height).build();
```

自动创建的 context 会由 RHI 负责释放窗口和 surface；外部传入的窗口或 surface 默认不由 RHI 销毁。
