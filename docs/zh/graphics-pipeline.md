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

`rendering(...)` 只声明 pipeline 兼容的 attachment format。实际渲染目标由 command buffer 的 `beginRendering` 指定。

## Begin Rendering

```java
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiViewport;

var renderingInfo = RhiRenderingInfo.builder(RhiRect2D.of(width, height))
        .color(RhiRenderingAttachment.clearColor(colorView, 0.02f, 0.02f, 0.025f, 1.0f))
        .build();

cmd.beginRendering(renderingInfo);
cmd.setViewport(RhiViewport.of(width, height));
cmd.setScissor(RhiRect2D.of(width, height));
```

深度附件示例：

```java
var renderingInfo = RhiRenderingInfo.builder(RhiRect2D.of(width, height))
        .color(RhiRenderingAttachment.color(colorView))
        .depth(RhiRenderingAttachment.depth(depthView, RhiAttachmentLoadOp.CLEAR, 1.0f))
        .build();
```

## 录制一次 Draw

```java
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

## Vulkan 映射

Vulkan 后端：

- pipeline 创建时使用 `VkPipelineRenderingCreateInfo`。
- `renderPass` 传 `VK_NULL_HANDLE`。
- `beginRendering` 映射为 `vkCmdBeginRendering`。
- attachment layout 由 `RhiRenderingAttachment.layout()` 和 frame graph/barrier 状态共同决定。

## OpenGL 映射

OpenGL 后端：

- 使用 RHI pipeline create-info 编译并链接 OpenGL program。
- Dynamic Rendering 映射为临时 framebuffer/attachment 绑定。
- Viewport、scissor、vertex buffer、index buffer 和 descriptor set 绑定映射为 OpenGL 状态。

## 注意事项

- Pipeline 声明的 attachment format 必须与实际 rendering attachment 的 image format 兼容。
- Vulkan 风格的 viewport/scissor 是显式命令；OpenGL 后端也通过 command buffer 接收这些状态。
- 当前文档只覆盖 graphics pipeline；compute pipeline 可在后续版本中按同样模块化方式加入。

