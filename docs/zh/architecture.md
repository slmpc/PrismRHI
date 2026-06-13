# 架构与模块

PrismRHI 采用“核心 API + 可选后端”的结构。核心模块不直接绑定 Vulkan 或 OpenGL 的实现细节，后端模块通过 `ServiceLoader` 注册 Provider。

## 模块

| 模块 | 职责 |
| --- | --- |
| `prism-rhi-core` | 后端无关 API、create-info、资源句柄、命令缓冲、descriptor、pipeline、frame graph。 |
| `prism-rhi-backend-vulkan` | Vulkan 后端，使用 LWJGL Vulkan 和 LWJGL VMA。 |
| `prism-rhi-backend-opengl41` | OpenGL 4.1 兼容后端，使用 bind-to-edit 风格调用。 |
| `prism-rhi-backend-opengl-dsa` | OpenGL 4.5 DSA 后端，使用 `glCreateBuffers`、`glNamedBufferData` 等现代接口。 |
| `prism-rhi-shaderc` | 可选 shaderc 集成，将 GLSL 编译为 SPIR-V。 |

应用发布时只需要带上自己选择的后端 jar。没有放入 classpath 的后端不会被 `ServiceLoader` 发现，也不会引入对应 LWJGL 后端依赖。

## 分层

典型对象关系如下：

```text
PrismRHI
  -> RhiBackendProvider
      -> RhiInstance
          -> RhiPhysicalDevice
          -> RhiDevice
              -> RhiQueue
              -> RhiCommandPool
                  -> RhiCommandBuffer
              -> RhiBuffer / RhiImage / RhiImageView / RhiSampler
              -> RhiShaderModule
              -> RhiDescriptorSetLayout / RhiDescriptorSet
              -> RhiGraphicsPipeline
```

## Vulkan 风格约定

PrismRHI 的核心 API 借鉴 Vulkan：

- 使用不可变 create-info record 表达创建参数。
- command buffer 显式 `begin/end`。
- pipeline、descriptor set、vertex/index buffer 显式绑定。
- draw 命令参数使用 Vulkan 命名，如 `firstVertex`、`firstInstance`、`vertexOffset`。
- graphics pipeline 不绑定传统 render pass，而是声明 Dynamic Rendering attachment format。
- barrier 和 frame graph 使用资源状态转换模型。

OpenGL 后端会把这些概念映射到 OpenGL 对象和状态机上。由于 OpenGL 与 Vulkan 能力不完全一致，部分 Vulkan 风格参数在 OpenGL 4.1 后端中会抛出 `RhiException`。

## Package 拆分

核心 API 没有堆在单一 package 下，而是按职责拆分：

| Package | 内容 |
| --- | --- |
| `com.github.slmpc.prismrhi.backend` | 后端枚举、能力、Provider。 |
| `com.github.slmpc.prismrhi.instance` | Instance 创建和物理设备枚举。 |
| `com.github.slmpc.prismrhi.device` | Device、physical device、队列请求和设备功能。 |
| `com.github.slmpc.prismrhi.queue` | Queue、submit info。 |
| `com.github.slmpc.prismrhi.command` | Command pool/buffer 和 draw command record。 |
| `com.github.slmpc.prismrhi.resource` | Buffer、Image、ImageView、Sampler、usage、memory usage。 |
| `com.github.slmpc.prismrhi.format` | 格式和 extent。 |
| `com.github.slmpc.prismrhi.shader` | Shader module 和 shader compiler SPI。 |
| `com.github.slmpc.prismrhi.descriptor` | Descriptor set layout、descriptor set、writer。 |
| `com.github.slmpc.prismrhi.pipeline` | Graphics pipeline 状态。 |
| `com.github.slmpc.prismrhi.rendering` | Dynamic Rendering 参数和附件。 |
| `com.github.slmpc.prismrhi.barrier` | Resource state 和 pipeline barrier。 |
| `com.github.slmpc.prismrhi.framegraph` | Frame graph pass 和资源访问声明。 |

## 后端能力标识

后端通过 `BackendInfo.features()` 暴露能力，例如：

- `COMPATIBILITY_PROFILE`
- `DIRECT_STATE_ACCESS`
- `EXPLICIT_MEMORY`
- `EXPLICIT_SYNCHRONIZATION`
- `SPIRV_SHADER_MODULES`
- `GLSL_SHADER_MODULES`
- `DESCRIPTORS`
- `DESCRIPTOR_INDEXING`
- `GRAPHICS_PIPELINES`
- `DYNAMIC_RENDERING`
- `FRAME_GRAPH`
- `MULTI_DRAW`

这些能力用于查询和表达期望，不替代具体 API 的运行时限制检查。比如 Vulkan multi-draw 仍然依赖设备支持 `VK_EXT_multi_draw`。

