# PrismRHI

PrismRHI 是一个基于 LWJGL 的 Java Rendering Hardware Interface，API 风格接近 Vulkan，并通过可选模块提供不同后端。

## 模块

- `prism-rhi-core`：后端无关的 RHI API、create-info、资源句柄、队列和命令抽象。
- `prism-rhi-backend-opengl41`：OpenGL 4.1 兼容后端，使用 bind-to-edit 调用。
- `prism-rhi-backend-opengl-dsa`：现代 OpenGL 后端，需要 OpenGL 4.5 DSA 支持。
- `prism-rhi-backend-vulkan`：Vulkan 后端，基于 LWJGL Vulkan 和 LWJGL VMA。
- `prism-rhi-shaderc`：可选的 LWJGL shaderc 集成，用于把 GLSL 编译为 SPIR-V。
- `prism-rhi-demo-triangle`：可运行的 Vulkan 3D 立方体 demo，使用 RHI 管理的 GLFW window 和 swapchain。模块名保留了最初的 triangle 命名以保持兼容。

应用通常只需要依赖 `prism-rhi-core` 和实际发布时需要的后端 jar。后端通过 Java `ServiceLoader` 发现，因此不依赖某个后端模块时，也不会把该后端的 LWJGL 依赖带入包体。

## Vulkan 3D Demo

运行窗口化 Vulkan demo：

```bash
./gradlew :prism-rhi-demo-triangle:runTriangleDemo
```

该 demo 渲染一个 indexed 3D 立方体，使用 JOML camera、uniform buffer、vertex buffer、index buffer、descriptor set、depth attachment 和 frame graph rendering pass。它通过 `RhiContextCreateInfo.autoGlfwWindow(...)` 让 Vulkan 后端自动创建 GLFW window、surface、device、swapchain、semaphore 和 dynamic-rendering frame。应用也可以通过 `RhiContextCreateInfo.glfwWindow(...)` / `externalVulkanSurface(...)` 传入已有 GLFW window 或 native Vulkan surface。

在 macOS 上，Gradle run task 会自动添加 GLFW 创建窗口所需的 `-XstartOnFirstThread`。

## 后端选择

```java
import com.github.slmpc.prismrhi.PrismRHI;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;

var instance = PrismRHI.createInstance(
        RhiInstanceCreateInfo.builder(BackendApi.VULKAN)
                .applicationName("Sample")
                .enableValidation(true)
                .build()
);
```

如果应用已经创建好了 Vulkan instance，可以传入 native `VkInstance` handle，以及该 instance 创建时启用过的 instance extension。除非显式设置 `ownsNativeInstance(true)` 或 `ownsSurface(true)`，RHI 不会销毁外部传入的 Vulkan object。

```java
import com.github.slmpc.prismrhi.context.RhiContextCreateInfo;

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

创建 instance 前，也可以枚举当前可用的后端 provider：

```java
import com.github.slmpc.prismrhi.PrismRHI;
import com.github.slmpc.prismrhi.backend.RhiBackendProvider;

for (RhiBackendProvider provider : PrismRHI.providers()) {
    System.out.println(provider.info().displayName() + ": " + provider.isSupported());
}
```

## OpenGL Context 归属

OpenGL 后端要求应用先创建并 make current 一个 OpenGL context，例如通过 GLFW 创建。RHI 会包装当前 context，并暴露 Vulkan 风格的 device、queue、resource 和 command-pool 接口。

`OPENGL_41` 面向兼容场景，避免使用 DSA 入口点。`OPENGL_DSA` 需要 OpenGL 4.5 context，并使用 `glCreateBuffers`、`glNamedBufferData`、`glCreateTextures`、`glTextureStorage2D` 等调用。

## Vulkan 内存

Vulkan 后端会为每个 logical device 创建一个 VMA allocator。Buffer 和 image 创建分别通过 `vmaCreateBuffer` / `vmaCreateImage` 完成，`RhiMemoryUsage` 会映射到 VMA 的自动内存使用策略。

## GLSL 与 Shaderc

OpenGL 后端直接接受 GLSL shader module。Vulkan 后端直接接受 SPIR-V；如果 classpath 中存在可选的 `prism-rhi-shaderc` 模块，也可以接受 GLSL 并自动编译。Shader compiler 通过 `ServiceLoader` 发现，因此应用只有在选择 shaderc 模块时才会携带相关依赖。

```java
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;

var vertexShader = device.createShaderModule(RhiShaderModuleCreateInfo.glsl(
        RhiShaderStage.VERTEX,
        """
        #version 450
        layout(location = 0) in vec3 inPosition;
        void main() {
            gl_Position = vec4(inPosition, 1.0);
        }
        """
));
```

也可以显式调用编译器：

```java
var spirvInfo = RhiShaderCompilers.compile(
        RhiShaderModuleCreateInfo.glsl(RhiShaderStage.FRAGMENT, fragmentSource),
        RhiShaderCodeType.SPIRV,
        RhiShaderCompileOptions.builder()
                .sourceName("fragment.glsl")
                .optimizationLevel(RhiShaderOptimizationLevel.PERFORMANCE)
                .build()
);
```

## Graphics Pipeline 与 Dynamic Rendering

Graphics pipeline 使用 attachment format 声明兼容性，而不是绑定传统 render pass。Vulkan 后端使用 `VkPipelineRenderingCreateInfo` 创建 pipeline，并通过 `vkCmdBeginRendering` 录制 rendering；OpenGL 后端会把同样的 RHI 形态映射到临时 framebuffer。

```java
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.pipeline.RhiDynamicRenderingState;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputRate;
import com.github.slmpc.prismrhi.rendering.RhiAttachmentLoadOp;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiViewport;
import com.github.slmpc.prismrhi.resource.RhiImageViewCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiIndexType;

var colorView = device.createImageView(RhiImageViewCreateInfo.of(colorImage));
var depthView = device.createImageView(RhiImageViewCreateInfo.of(depthImage));

var pipeline = device.createGraphicsPipeline(
        RhiGraphicsPipelineCreateInfo.builder()
                .shader(VERTEX, vertexShader)
                .shader(FRAGMENT, fragmentShader)
                .descriptorSetLayout(sceneLayout)
                .rendering(RhiDynamicRenderingState.builder()
                        .color(RhiFormat.BGRA8_UNORM)
                        .depth(RhiFormat.D32_FLOAT)
                        .build())
                .vertexBinding(0, 32, RhiVertexInputRate.VERTEX)
                .vertexAttribute(0, 0, RhiFormat.RGBA32_FLOAT, 0)
                .vertexAttribute(1, 0, RhiFormat.RGBA32_FLOAT, 16)
                .build()
);

var renderingInfo = RhiRenderingInfo.builder(RhiRect2D.of(width, height))
        .color(RhiRenderingAttachment.clearColor(colorView, 0.02f, 0.02f, 0.025f, 1.0f))
        .depth(RhiRenderingAttachment.depth(depthView, RhiAttachmentLoadOp.CLEAR, 1.0f))
        .build();

cmd.begin();
cmd.beginRendering(renderingInfo);
cmd.setViewport(RhiViewport.of(width, height));
cmd.setScissor(RhiRect2D.of(width, height));
cmd.bindGraphicsPipeline(pipeline);
cmd.bindDescriptorSet(pipeline, 0, sceneSet);
cmd.bindVertexBuffer(0, vertexBuffer, 0);
cmd.bindIndexBuffer(indexBuffer, 0, RhiIndexType.UINT32);
cmd.drawIndexed(indexCount);
cmd.endRendering();
cmd.end();
```

## Frame Graph

PrismRHI 内置一个轻量 frame graph，用于跟踪声明的资源状态，并在 pass 边界自动插入 barrier。Rendering pass 还可以声明自己的 `RhiRenderingInfo`；graph 会自动在 callback 外包裹 `beginRendering`、根据 render area 设置默认 viewport/scissor，并在最后调用 `endRendering`。Pass callback 因此可以专注于绘制命令。

```java
import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.framegraph.RhiFrameGraph;

var graph = RhiFrameGraph.create()
        .resource(gbufferColor, RhiResourceState.UNDEFINED)
        .resource(depthImage, RhiResourceState.UNDEFINED)
        .resource(backbuffer, RhiResourceState.UNDEFINED);

graph.addPass("gbuffer")
        .writeImage(gbufferColor, RhiResourceState.COLOR_ATTACHMENT)
        .writeImage(depthImage, RhiResourceState.DEPTH_STENCIL_ATTACHMENT)
        .rendering(gbufferRendering)
        .record((cmd, pass) -> {
            cmd.bindGraphicsPipeline(gbufferPipeline);
            cmd.drawIndexed(gbufferIndexCount);
        });

graph.addPass("lighting")
        .readImage(gbufferColor, RhiResourceState.SAMPLED_IMAGE)
        .readImage(depthImage, RhiResourceState.SAMPLED_IMAGE)
        .writeImage(backbuffer, RhiResourceState.COLOR_ATTACHMENT)
        .rendering(lightingRendering)
        .record((cmd, pass) -> {
            cmd.bindGraphicsPipeline(lightingPipeline);
            cmd.bindDescriptorSet(lightingPipeline, 0, lightingSet);
            cmd.draw(3);
        });

cmd.begin();
graph.execute(cmd);
cmd.end();
```

Vulkan 后端会把 frame graph transition 映射为 `vkCmdPipelineBarrier` 的 image/buffer barrier，包括 image layout transition。OpenGL 后端接受同样的 graph；由于 OpenGL 对这类 draw/texture/framebuffer 使用通常由驱动状态机隐式同步，barrier 多数情况下是 no-op。

## Descriptor

Descriptor API 保持紧凑：先创建 layout，再从 layout 分配 set，最后用 writer 更新。

```java
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetAllocateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorStage;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorType;

var layout = device.createDescriptorSetLayout(
        RhiDescriptorSetLayoutCreateInfo.builder()
                .binding(0, RhiDescriptorType.UNIFORM_BUFFER, 1, RhiDescriptorStage.ALL_GRAPHICS)
                .bindless(1, RhiDescriptorType.SAMPLED_IMAGE, 4096, RhiDescriptorStage.FRAGMENT)
                .build()
);

var set = device.allocateDescriptorSet(RhiDescriptorSetAllocateInfo.bindless(layout, 1024));
set.update(writer -> writer.uniformBuffer(0, cameraBuffer));
```

Image descriptor 使用 image view 和可选 sampler：

```java
var sampler = device.createSampler(RhiSamplerCreateInfo.linearRepeat());
set.update(writer -> writer.combinedImageSampler(1, 0, colorView, sampler));
```

`bindless` 会为对应 binding 启用 descriptor indexing flags：update-after-bind、update-unused-while-pending、partially-bound 和 variable descriptor count。Vulkan 后端会在 logical device 创建时启用 descriptor indexing features，并映射到 Vulkan 1.2 descriptor indexing。OpenGL 后端保留同样的 RHI descriptor 形态，在 command buffer 绑定 descriptor set 时绑定对应 buffer/texture。

## 绘制命令

`RhiCommandBuffer` 暴露 Vulkan 风格的绘制命令：

- `draw` / `drawInstanced`
- `drawIndexed` / `drawIndexedInstanced`
- `multiDraw` / `multiDrawIndexed`
- `drawIndirect` / `drawIndexedIndirect`
- `multiDrawIndirect` / `multiDrawIndexedIndirect`
- `multiDrawIndirectCount` / `multiDrawIndexedIndirectCount`

Indirect command 使用 `RhiBuffer` 加 byte offset，因此同一套 API 可以映射到 Vulkan buffer 和 OpenGL indirect buffer。Packed indirect stride 使用 Vulkan 兼容布局：non-indexed indirect draw 为 16 bytes，indexed indirect draw 为 20 bytes。

`multiDraw` 和 `multiDrawIndexed` 是真实 multi-draw 命令，不是在 RHI 层循环调用多个 `draw`。OpenGL 后端使用 `glMultiDrawArrays` 和 `glMultiDrawElements`；Vulkan 使用 `VK_EXT_multi_draw`，如果扩展不可用则抛出 `RhiException`。由于真实 multi-draw API 会在所有 draw 之间共享 instance 参数，`RhiMultiDrawCommand` 和 `RhiMultiDrawIndexedCommand` 要求统一的 `instanceCount` 和 `firstInstance`。

OpenGL 后端在 command buffer 上暴露 `setPrimitiveTopology` 和 `setIndexType`，因为 OpenGL draw 调用在执行时需要这些状态。Vulkan 的 topology 由 pipeline state 控制，index type 通过 `bindIndexBuffer` 指定。

`OPENGL_41` 支持 direct draw、instanced draw 和单个 indirect draw。对于 OpenGL 4.1 不支持的 Vulkan 风格参数，例如 `firstInstance`、indexed `vertexOffset`、multi indirect、indirect count draw，会抛出 `RhiException`。`OPENGL_DSA` 支持现代 base-instance 和 multi-indirect 调用；indirect count draw 需要 OpenGL 4.6 或 `GL_ARB_indirect_parameters`。
