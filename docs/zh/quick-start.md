# 快速开始

本页展示一个最小接入流程：选择后端、创建设备、创建资源、录制命令并提交。

## 1. 添加模块依赖

应用至少需要依赖 `prism-rhi-core`，再按需选择一个或多个后端模块。

```kotlin
dependencies {
    implementation(project(":prism-rhi-core"))

    // 按需选择，未依赖的后端不会进入运行时包体。
    runtimeOnly(project(":prism-rhi-backend-vulkan"))
    runtimeOnly(project(":prism-rhi-backend-opengl-dsa"))

    // 使用 GLSL 编写 Vulkan shader 时添加。
    runtimeOnly(project(":prism-rhi-shaderc"))
}
```

如果你正在仓库内部开发 PrismRHI，各模块已经在根 `build.gradle.kts` 中声明了 LWJGL 依赖和当前平台 natives。

## 2. 创建 Instance

后端通过 Java `ServiceLoader` 发现。`PrismRHI.createInstance` 会按 `BackendApi` 找到匹配 Provider。

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

也可以先查看当前进程中可用的后端：

```java
for (var provider : PrismRHI.providers()) {
    System.out.println(provider.info().displayName() + ": " + provider.isSupported());
}
```

## 3. 创建设备和队列

默认设备创建参数会请求一个 graphics queue。需要显式能力时，可以通过 `enableFeature` 表示应用期望启用的后端能力。

```java
import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.queue.RhiQueueType;

var physicalDevice = instance.enumeratePhysicalDevices().get(0);

var device = instance.createDevice(
        physicalDevice,
        RhiDeviceCreateInfo.builder()
                .debugName("Main Device")
                .queue(RhiQueueType.GRAPHICS, 1)
                .enableFeature(BackendFeature.DESCRIPTOR_INDEXING)
                .enableFeature(BackendFeature.DYNAMIC_RENDERING)
                .build()
);

var graphicsQueue = device.queue(RhiQueueType.GRAPHICS);
```

## 4. 创建资源

```java
import com.github.slmpc.prismrhi.resource.RhiBufferCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiBufferUsage;
import com.github.slmpc.prismrhi.resource.RhiMemoryUsage;

var vertexBuffer = device.createBuffer(
        RhiBufferCreateInfo.builder(vertexBufferSize)
                .usage(RhiBufferUsage.VERTEX_BUFFER)
                .usage(RhiBufferUsage.TRANSFER_DST)
                .memoryUsage(RhiMemoryUsage.GPU_ONLY)
                .build()
);
```

Vulkan 后端会通过 VMA 创建 buffer/image；OpenGL 后端会映射为 OpenGL buffer/texture 对象。

## 5. 创建命令池并录制命令

```java
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiCommandPoolCreateInfo;
import com.github.slmpc.prismrhi.queue.RhiSubmitInfo;
import com.github.slmpc.prismrhi.queue.RhiQueueType;

var commandPool = device.createCommandPool(
        new RhiCommandPoolCreateInfo(RhiQueueType.GRAPHICS, false, true)
);

var cmd = commandPool.allocateCommandBuffer(RhiCommandBufferLevel.PRIMARY);

cmd.begin();
cmd.beginRendering(renderingInfo);
cmd.setViewport(viewport);
cmd.setScissor(scissor);
cmd.bindGraphicsPipeline(pipeline);
cmd.bindVertexBuffer(0, vertexBuffer, 0);
cmd.draw(3);
cmd.endRendering();
cmd.end();

graphicsQueue.submit(RhiSubmitInfo.of(cmd));
graphicsQueue.waitIdle();
```

## 6. 生命周期

所有设备级资源都实现 `RhiResource`，可在不再使用时关闭。常见顺序是：

1. 等待队列或设备空闲。
2. 释放 pipeline、descriptor、shader、view、image、buffer、command pool。
3. 关闭 device。
4. 关闭 instance。

```java
device.waitIdle();
pipeline.close();
vertexBuffer.close();
commandPool.close();
device.close();
instance.close();
```
