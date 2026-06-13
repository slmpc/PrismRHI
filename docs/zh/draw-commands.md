# 绘制命令

`RhiCommandBuffer` 暴露 Vulkan 风格的 draw API，覆盖直接绘制、索引绘制、实例化、多绘制、间接绘制和 indirect count。

## 直接绘制

```java
cmd.draw(3);
cmd.draw(vertexCount, instanceCount, firstVertex, firstInstance);
```

等价 record：

```java
cmd.draw(new RhiDrawCommand(vertexCount, instanceCount, firstVertex, firstInstance));
```

## 索引绘制

```java
cmd.bindIndexBuffer(indexBuffer, 0, RhiIndexType.UINT32);
cmd.drawIndexed(indexCount);
cmd.drawIndexed(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
```

等价 record：

```java
cmd.drawIndexed(new RhiDrawIndexedCommand(
        indexCount,
        instanceCount,
        firstIndex,
        vertexOffset,
        firstInstance
));
```

## 实例化绘制

```java
cmd.drawInstanced(vertexCount, instanceCount);
cmd.drawInstanced(vertexCount, instanceCount, firstVertex, firstInstance);

cmd.drawIndexedInstanced(indexCount, instanceCount);
cmd.drawIndexedInstanced(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
```

这些方法是 `draw` / `drawIndexed` 的便捷封装。

## True Multi Draw

```java
cmd.multiDraw(List.of(
        new RhiDrawCommand(36, 1, 0, 0),
        new RhiDrawCommand(24, 1, 36, 0)
));

cmd.multiDrawIndexed(List.of(
        new RhiDrawIndexedCommand(120, 1, 0, 0, 0),
        new RhiDrawIndexedCommand(96, 1, 120, 0, 0)
));
```

PrismRHI 要求 multi draw 使用底层真实 multi-draw API，不在 RHI 层拆成多个 draw：

- OpenGL 使用 `glMultiDrawArrays` / `glMultiDrawElements`。
- Vulkan 使用 `VK_EXT_multi_draw` 的 `vkCmdDrawMultiEXT` / `vkCmdDrawMultiIndexedEXT`。

由于底层真实 multi-draw API 对 instance 参数有共享限制，`RhiMultiDrawCommand` 和 `RhiMultiDrawIndexedCommand` 会要求一组 draw 使用共同的 `instanceCount` 和 `firstInstance`。

## 间接绘制

```java
cmd.drawIndirect(indirectBuffer, indirectOffset);
cmd.drawIndexedIndirect(indirectBuffer, indexedIndirectOffset);
```

`indirectBuffer` 应带有 `RhiBufferUsage.INDIRECT_BUFFER`。

非索引 indirect 命令布局与 Vulkan 兼容，大小为 16 字节：

```text
uint vertexCount;
uint instanceCount;
uint firstVertex;
uint firstInstance;
```

索引 indirect 命令布局与 Vulkan 兼容，大小为 20 字节：

```text
uint indexCount;
uint instanceCount;
uint firstIndex;
int  vertexOffset;
uint firstInstance;
```

## Multi Draw Indirect

```java
cmd.multiDrawIndirect(indirectBuffer, offset, drawCount, stride);
cmd.multiDrawIndexedIndirect(indirectBuffer, offset, drawCount, stride);
```

推荐 stride：

- 非索引：`16`
- 索引：`20`

如果你在 indirect buffer 中追加自定义数据，stride 可以大于基础结构大小。

## Multi Draw Indirect Count

```java
cmd.multiDrawIndirectCount(
        indirectBuffer,
        offset,
        countBuffer,
        countOffset,
        maxDrawCount,
        stride
);

cmd.multiDrawIndexedIndirectCount(
        indirectBuffer,
        offset,
        countBuffer,
        countOffset,
        maxDrawCount,
        stride
);
```

`countBuffer` 提供实际 draw count，`maxDrawCount` 是安全上限。

便捷方法：

```java
cmd.drawIndirectCount(indirectBuffer, offset, countBuffer, countOffset, stride);
cmd.drawIndexedIndirectCount(indirectBuffer, offset, countBuffer, countOffset, stride);
```

它们等价于 `maxDrawCount = 1` 的 indirect count。

## OpenGL 后端限制

`OPENGL_41`：

- 支持直接 draw、indexed draw、instanced draw。
- 支持真实 `multiDraw` / `multiDrawIndexed`。
- 支持单 indirect draw。
- 不支持 multi indirect 和 indirect count。
- 对部分 Vulkan 风格参数会抛出 `RhiException`，例如非零 `firstInstance`、indexed `vertexOffset` 等。

`OPENGL_DSA`：

- 支持现代 base-instance draw。
- 支持 multi indirect。
- indirect count 需要 OpenGL 4.6 或 `GL_ARB_indirect_parameters`。

Vulkan：

- 直接 draw、indexed draw、instanced draw 和 indirect draw 使用 Vulkan core 命令。
- 非 indirect 的 true multi-draw 依赖 `VK_EXT_multi_draw`。

