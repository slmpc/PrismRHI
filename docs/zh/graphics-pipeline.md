# Graphics Pipeline 与 Dynamic Rendering

PrismRHI 不暴露传统 Render Pass。Graphics Pipeline 使用 Dynamic Rendering 的 attachment format 创建，渲染时通过 `RhiRenderingInfo` 指定实际附件。

核心类型：

- `RhiGraphicsPipelineCreateInfo`
- `RhiDynamicRenderingState`
- `RhiPipelineShaderStage`
- `RhiVertexInputBinding`
- `RhiVertexAttribute`
- `RhiRasterizationState`
- `RhiDepthStencilState`
- `RhiColorBlendAttachmentState`
- `RhiRenderingInfo`
- `RhiRenderingAttachment`

## 创建 Pipeline

```java
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.pipeline.RhiDynamicRenderingState;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputRate;
import static com.github.slmpc.prismrhi.shader.RhiShaderStage.FRAGMENT;
import static com.github.slmpc.prismrhi.shader.RhiShaderStage.VERTEX;

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
```

`rendering(...)` 只声明 pipeline 兼容的 attachment format。实际渲染目标由 `RhiRenderingInfo` 指定，通常通过 Frame Graph pass 的 `.rendering(...)` 声明；也可以直接调用 command buffer 的 `beginRendering(...)`。

## 声明 Rendering Info

```java
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;

var renderingInfo = RhiRenderingInfo.builder(RhiRect2D.of(width, height))
        .color(RhiRenderingAttachment.clearColor(colorView, 0.02f, 0.02f, 0.025f, 1.0f))
        .build();
```

深度附件示例：

```java
var renderingInfo = RhiRenderingInfo.builder(RhiRect2D.of(width, height))
        .color(RhiRenderingAttachment.color(colorView))
        .depth(RhiRenderingAttachment.depth(depthView, RhiAttachmentLoadOp.CLEAR, 1.0f))
        .build();
```

## 通过 Frame Graph 录制一次 Draw

```java
var graph = RhiFrameGraph.create()
        .resource(colorImage, RhiResourceState.UNDEFINED)
        .resource(depthImage, RhiResourceState.UNDEFINED);

graph.addPass("scene")
        .writeImage(colorImage, RhiResourceState.COLOR_ATTACHMENT)
        .writeImage(depthImage, RhiResourceState.DEPTH_STENCIL_ATTACHMENT)
        .rendering(renderingInfo)
        .record((cmd, pass) -> {
            cmd.bindGraphicsPipeline(pipeline);
            cmd.bindDescriptorSet(pipeline, 0, sceneSet);
            cmd.bindVertexBuffer(0, vertexBuffer, 0);
            cmd.bindIndexBuffer(indexBuffer, 0, RhiIndexType.UINT32);
            cmd.drawIndexed(indexCount);
        });

cmd.begin();
graph.execute(cmd);
cmd.end();
```

Frame Graph 会根据 `.rendering(renderingInfo)` 自动调用 `beginRendering`，并用 `renderingInfo.renderArea()` 设置默认 viewport/scissor，最后自动 `endRendering`。如果不使用 Frame Graph，也可以手动调用这些 command buffer 方法。

## Vulkan 映射

Vulkan 后端：

- pipeline 创建时使用 `VkPipelineRenderingCreateInfo`。
- `renderPass` 传 `VK_NULL_HANDLE`。
- Frame Graph 自动包裹的 `beginRendering` 映射为 `vkCmdBeginRendering`。
- attachment layout 由 `RhiRenderingAttachment.layout()` 和 frame graph/barrier 状态共同决定。

## OpenGL 映射

OpenGL 后端：

- 使用 RHI pipeline create-info 编译并链接 OpenGL program。
- Dynamic Rendering 映射为临时 framebuffer/attachment 绑定。
- Viewport、scissor、vertex buffer、index buffer 和 descriptor set 绑定映射为 OpenGL 状态。

## 注意事项

- Pipeline 声明的 attachment format 必须与实际 rendering attachment 的 image format 兼容。
- Vulkan 风格的 viewport/scissor 是显式状态；Frame Graph 会为渲染 pass 设置默认 viewport/scissor，特殊视口仍可在 pass callback 中覆盖。
- 当前文档只覆盖 graphics pipeline；compute pipeline 可在后续版本中按同样模块化方式加入。
