# PrismRHI 中文开发文档

PrismRHI 是一个基于 LWJGL 的 Java RHI，API 风格偏 Vulkan，并通过可选后端模块提供 Vulkan、OpenGL 4.1 兼容后端和现代 OpenGL DSA 后端。

这份文档面向引擎或渲染层开发者，重点说明如何接入、如何组织后端依赖，以及核心抽象如何映射到底层图形 API。

## 文档索引

- [快速开始](quick-start.md)
- [架构与模块](architecture.md)
- [后端选择与加载](backends.md)
- [资源与内存](resources.md)
- [Shader 与 shaderc](shaders.md)
- [Descriptor 设计](descriptors.md)
- [Graphics Pipeline 与 Dynamic Rendering](graphics-pipeline.md)
- [绘制命令](draw-commands.md)
- [Frame Graph](frame-graph.md)

## 当前设计目标

- 使用 Vulkan 风格的 create-info、显式资源、命令缓冲和描述符模型。
- 后端按模块加载，应用只依赖自己要发布的后端，减少包体。
- Vulkan 后端使用 LWJGL Vulkan，并通过 LWJGL VMA 管理 GPU 内存。
- OpenGL 提供两个后端：`OPENGL_41` 用于兼容场景，`OPENGL_DSA` 用于现代 OpenGL 4.5 DSA 调用。
- Shader 使用 GLSL 编写；Vulkan 可通过可选的 `prism-rhi-shaderc` 模块编译为 SPIR-V。
- Graphics Pipeline 使用 Dynamic Rendering，不引入传统 Render Pass 抽象。
- 内置轻量 Frame Graph，用资源状态声明自动生成 pass 边界 barrier。
- multi draw 使用底层真实 multi-draw API，不在 RHI 层拆成多个 draw。

## 包结构概览

核心 API 位于 `prism-rhi-core`，按职责拆分 package：

- `backend`：后端类型、能力描述和 `ServiceLoader` Provider。
- `instance`：RHI 实例创建和物理设备枚举。
- `device`：逻辑设备、物理设备信息和设备创建参数。
- `queue`：队列类型、提交参数和队列接口。
- `command`：命令池、命令缓冲和所有 draw 命令结构。
- `resource`：Buffer、Image、ImageView、Sampler 与内存用途。
- `format`：格式和 extent。
- `shader`：Shader 模块、编译 SPI 与 shader create-info。
- `descriptor`：Descriptor Set Layout、Set、Writer 与 Descriptor Indexing 标志。
- `pipeline`：Graphics Pipeline、顶点输入、光栅化、深度模板和混合状态。
- `rendering`：Dynamic Rendering、附件、Viewport、Scissor。
- `barrier`：资源状态和 pipeline barrier 描述。
- `framegraph`：Frame Graph、Pass 和资源访问声明。

后端实现位于各自模块内：

- `prism-rhi-backend-vulkan`
- `prism-rhi-backend-opengl41`
- `prism-rhi-backend-opengl-dsa`
- `prism-rhi-shaderc`

