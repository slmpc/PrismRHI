# 资源与内存

PrismRHI 的资源抽象位于 `resource` package。核心资源包括：

- `RhiBuffer`
- `RhiImage`
- `RhiImageView`
- `RhiSampler`

所有资源都实现 `RhiResource`，用完后应调用 `close()`。

## Buffer

Buffer 使用 `RhiBufferCreateInfo` 创建：

```java
var buffer = device.createBuffer(
        RhiBufferCreateInfo.builder(1024 * 1024)
                .usage(RhiBufferUsage.VERTEX_BUFFER)
                .usage(RhiBufferUsage.TRANSFER_DST)
                .memoryUsage(RhiMemoryUsage.GPU_ONLY)
                .build()
);
```

常见 usage：

- `VERTEX_BUFFER`
- `INDEX_BUFFER`
- `UNIFORM_BUFFER`
- `STORAGE_BUFFER`
- `INDIRECT_BUFFER`
- `TRANSFER_SRC`
- `TRANSFER_DST`

`RhiMemoryUsage` 表示内存访问倾向：

- `GPU_ONLY`：优先 GPU 本地内存，适合长期驻留资源。
- `CPU_TO_GPU`：适合上传数据。
- `GPU_TO_CPU`：适合回读。

Vulkan 后端会将其映射到 VMA 的自动内存使用策略。OpenGL 后端会映射到对应 buffer storage/data 调用。

## Image

Image 使用 extent、format、usage 和 memory usage 创建：

```java
import com.github.slmpc.prismrhi.format.RhiExtent3D;
import com.github.slmpc.prismrhi.format.RhiFormat;

var colorImage = device.createImage(
        RhiImageCreateInfo.builder(RhiExtent3D.of2D(width, height))
                .format(RhiFormat.RGBA8_UNORM)
                .usage(RhiImageUsage.COLOR_ATTACHMENT)
                .usage(RhiImageUsage.SAMPLED)
                .memoryUsage(RhiMemoryUsage.GPU_ONLY)
                .build()
);
```

常见 usage：

- `SAMPLED`
- `STORAGE`
- `COLOR_ATTACHMENT`
- `DEPTH_STENCIL_ATTACHMENT`
- `TRANSFER_SRC`
- `TRANSFER_DST`

## Image View

渲染附件和 image descriptor 都使用 `RhiImageView`，不是直接绑定 `RhiImage`。

```java
var colorView = device.createImageView(RhiImageViewCreateInfo.of(colorImage));
```

Dynamic Rendering 的 color/depth attachment，以及 descriptor writer 的 sampled/storage/combined image 都接收 image view。

## Sampler

```java
var sampler = device.createSampler(RhiSamplerCreateInfo.linearRepeat());
```

Sampler 可用于 combined image sampler，也可单独写入 `SAMPLER` descriptor。

## 资源状态

显式资源状态由 `barrier` 和 `framegraph` package 管理，常见状态包括：

- `UNDEFINED`
- `TRANSFER_SRC`
- `TRANSFER_DST`
- `VERTEX_BUFFER`
- `INDEX_BUFFER`
- `UNIFORM_BUFFER`
- `STORAGE_BUFFER_READ`
- `STORAGE_BUFFER_WRITE`
- `INDIRECT_BUFFER`
- `COLOR_ATTACHMENT`
- `DEPTH_STENCIL_ATTACHMENT`
- `SAMPLED_IMAGE`
- `STORAGE_IMAGE_READ`
- `STORAGE_IMAGE_WRITE`
- `PRESENT`

手写 barrier 时使用 `RhiPipelineBarrier`；使用 Frame Graph 时，在 pass 上声明读写状态即可。
