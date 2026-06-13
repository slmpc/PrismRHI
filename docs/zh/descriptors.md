# Descriptor 设计

PrismRHI 的 descriptor 设计目标是比 Vulkan 原生样板更简洁，同时保留 Vulkan 风格的 set/layout/binding 模型，并支持 Descriptor Indexing / bindless 用法。

核心类型：

- `RhiDescriptorSetLayoutCreateInfo`
- `RhiDescriptorSetLayout`
- `RhiDescriptorSetAllocateInfo`
- `RhiDescriptorSet`
- `RhiDescriptorWriter`

## 创建 Layout

```java
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorStage;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorType;

var sceneLayout = device.createDescriptorSetLayout(
        RhiDescriptorSetLayoutCreateInfo.builder()
                .binding(0, RhiDescriptorType.UNIFORM_BUFFER, 1, RhiDescriptorStage.ALL_GRAPHICS)
                .binding(1, RhiDescriptorType.COMBINED_IMAGE_SAMPLER, 1, RhiDescriptorStage.FRAGMENT)
                .build()
);
```

## 分配 Set

```java
var sceneSet = device.allocateDescriptorSet(
        RhiDescriptorSetAllocateInfo.of(sceneLayout)
);
```

## 更新 Descriptor

Descriptor 更新通过 writer 完成：

```java
var sampler = device.createSampler(RhiSamplerCreateInfo.linearRepeat());

sceneSet.update(writer -> writer
        .uniformBuffer(0, cameraBuffer)
        .combinedImageSampler(1, 0, colorView, sampler)
);
```

Writer 支持：

- `uniformBuffer`
- `storageBuffer`
- `sampler`
- `sampledImage`
- `combinedImageSampler`
- `storageImage`

Image descriptor 使用 `RhiImageView`，combined image sampler 额外提供 `RhiSampler`。

## Bindless Descriptor

使用 `bindless` 可以快速创建 descriptor indexing binding：

```java
var bindlessLayout = device.createDescriptorSetLayout(
        RhiDescriptorSetLayoutCreateInfo.builder()
                .binding(0, RhiDescriptorType.UNIFORM_BUFFER, 1, RhiDescriptorStage.ALL_GRAPHICS)
                .bindless(1, RhiDescriptorType.SAMPLED_IMAGE, 4096, RhiDescriptorStage.FRAGMENT)
                .build()
);

var bindlessSet = device.allocateDescriptorSet(
        RhiDescriptorSetAllocateInfo.bindless(bindlessLayout, 1024)
);
```

`bindless` 会为该 binding 设置：

- `UPDATE_AFTER_BIND`
- `UPDATE_UNUSED_WHILE_PENDING`
- `PARTIALLY_BOUND`
- `VARIABLE_DESCRIPTOR_COUNT`

Layout 会自动加入 `UPDATE_AFTER_BIND_POOL` 标志。

## 绑定 Descriptor Set

Descriptor set 在 command buffer 中绑定到 graphics pipeline：

```java
cmd.bindGraphicsPipeline(pipeline);
cmd.bindDescriptorSet(pipeline, 0, sceneSet);
```

多个 set 可一次绑定：

```java
cmd.bindDescriptorSets(pipeline, 0, List.of(sceneSet, materialSet));
```

## Vulkan 映射

Vulkan 后端会将 layout/binding flag 映射到 Vulkan descriptor set layout 和 descriptor indexing 标志，并在逻辑设备创建时启用相关特性。

## OpenGL 映射

OpenGL 后端保留相同的 RHI descriptor 形状，在 command buffer 绑定 descriptor set 时把 buffer、texture 和 sampler 绑定到 OpenGL 状态。它不是 Vulkan descriptor heap 的完整模拟，而是为跨后端 API 形状提供一致入口。

