# Frame Graph

PrismRHI 内置一个轻量 Frame Graph，用于在 pass 之间根据资源读写声明自动插入 barrier。渲染 pass 还可以声明 `RhiRenderingInfo`，Frame Graph 会自动包裹 `beginRendering`、默认 viewport/scissor 和 `endRendering`，让 pass callback 更接近 UE5 RDG 的“只描述绘制工作”的形态。

核心类型：

- `RhiFrameGraph`
- `RhiFrameGraphPass`
- `RhiFrameGraphResourceAccess`
- `RhiFrameGraphPassCallback`
- `RhiResourceState`
- `RhiPipelineBarrier`

## 基本用法

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

执行 `graph.execute(cmd)` 时，Frame Graph 会在每个 pass callback 前比较当前资源状态和 pass 声明的目标状态。如果不同，则生成 `RhiPipelineBarrier` 并调用 `cmd.pipelineBarrier(...)`。如果 pass 声明了 `.rendering(...)`，Frame Graph 还会按以下顺序执行：

1. `cmd.beginRendering(renderingInfo)`
2. 根据 `renderingInfo.renderArea()` 设置默认 viewport 和 scissor
3. 执行 pass callback
4. `cmd.endRendering()`

## 声明资源

```java
graph.resource(colorImage, RhiResourceState.UNDEFINED);
graph.resource(vertexBuffer, RhiResourceState.VERTEX_BUFFER);
```

未声明的资源在第一次访问时会按 `UNDEFINED` 处理。建议显式注册 frame 内重要资源，便于阅读和调试。

## 声明读写

常见写入：

```java
graph.addPass("shadow")
        .writeImage(shadowMap, RhiResourceState.DEPTH_STENCIL_ATTACHMENT);

graph.addPass("compute-upload")
        .writeBuffer(storageBuffer, RhiResourceState.STORAGE_BUFFER_WRITE);
```

常见读取：

```java
graph.addPass("lighting")
        .readImage(gbufferColor, RhiResourceState.SAMPLED_IMAGE)
        .readBuffer(cameraBuffer, RhiResourceState.UNIFORM_BUFFER);
```

状态枚举在 `RhiResourceState` 中定义，后端会将其映射到各自 API 的访问掩码、阶段和 image layout。

## 声明渲染目标

```java
var renderingInfo = RhiRenderingInfo.builder(RhiRect2D.of(width, height))
        .color(RhiRenderingAttachment.clearColor(backbufferView, 0.02f, 0.025f, 0.03f, 1.0f))
        .depth(RhiRenderingAttachment.depth(depthView, RhiAttachmentLoadOp.CLEAR, 1.0f))
        .build();

graph.addPass("scene")
        .writeImage(backbuffer, RhiResourceState.COLOR_ATTACHMENT)
        .writeImage(depthImage, RhiResourceState.DEPTH_STENCIL_ATTACHMENT)
        .rendering(renderingInfo)
        .record((cmd, pass) -> {
            cmd.bindGraphicsPipeline(scenePipeline);
            cmd.bindDescriptorSet(scenePipeline, 0, sceneSet);
            cmd.bindVertexBuffer(0, vertexBuffer, 0);
            cmd.bindIndexBuffer(indexBuffer, 0, RhiIndexType.UINT16);
            cmd.drawIndexed(indexCount);
        });
```

`.rendering(...)` 描述的是本 pass 的实际附件和 render area；pipeline 创建时的 `RhiDynamicRenderingState` 仍只声明兼容的 attachment format。

## 重置状态

Frame Graph 会保存 initial state 和 current state：

```java
graph.reset();
```

典型用法是在每帧开始时 reset，然后 execute 一次。也可以复用 graph 结构，根据帧资源更新 pass callback 中引用的对象。

## Vulkan 映射

Vulkan 后端会将 `RhiPipelineBarrier` 转换为：

- buffer barrier
- image barrier
- image layout transition
- pipeline stage 和 access mask

这对应 `vkCmdPipelineBarrier`。

## OpenGL 映射

OpenGL 对 draw/texture/framebuffer 的这类同步通常由驱动状态机处理。OpenGL 后端接受相同的 `pipelineBarrier` 调用，但多数 barrier 为 no-op。

## 设计边界

当前 Frame Graph 重点解决 pass 边界资源状态转换，不负责：

- 自动创建 transient resource。
- pass 重排。
- aliasing。
- async compute 调度。
- swapchain acquire/present。

这些功能可以在保留当前 pass/resource 语义的基础上继续扩展。
