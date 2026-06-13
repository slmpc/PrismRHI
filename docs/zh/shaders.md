# Shader 与 shaderc

PrismRHI 的 shader 抽象位于 `shader` package。OpenGL 后端直接接受 GLSL；Vulkan 后端接受 SPIR-V，也可以在 `prism-rhi-shaderc` 存在时自动将 GLSL 编译为 SPIR-V。

## 创建 GLSL Shader Module

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

OpenGL 后端会将 GLSL 编译为 OpenGL shader。Vulkan 后端收到 GLSL 时，会通过 `RhiShaderCompilers` 查找可用 compiler；如果 classpath 没有 `prism-rhi-shaderc`，会抛出异常。

## 显式编译 GLSL 到 SPIR-V

```java
import com.github.slmpc.prismrhi.shader.RhiShaderCodeType;
import com.github.slmpc.prismrhi.shader.RhiShaderCompileOptions;
import com.github.slmpc.prismrhi.shader.RhiShaderCompilers;
import com.github.slmpc.prismrhi.shader.RhiShaderOptimizationLevel;

var spirvInfo = RhiShaderCompilers.compile(
        RhiShaderModuleCreateInfo.glsl(RhiShaderStage.FRAGMENT, fragmentSource),
        RhiShaderCodeType.SPIRV,
        RhiShaderCompileOptions.builder()
                .sourceName("fragment.glsl")
                .optimizationLevel(RhiShaderOptimizationLevel.PERFORMANCE)
                .build()
);

var fragmentShader = device.createShaderModule(spirvInfo);
```

## shaderc 模块

模块：`prism-rhi-shaderc`

该模块通过 `ServiceLoader` 注册：

```text
META-INF/services/com.github.slmpc.prismrhi.shader.RhiShaderCompiler
```

只在应用选择依赖此模块时，shaderc 和对应 LWJGL native 才会进入运行时包体。

## Shader Stage

当前 graphics pipeline 常用 stage：

- `VERTEX`
- `FRAGMENT`

后续可扩展 geometry、compute 等 stage。当前文档聚焦 graphics pipeline。

## 建议

- 面向 Vulkan 时推荐使用 `#version 450`。
- 尽量显式声明 `layout(location = X)`，让 vertex input 和 fragment output 更稳定。
- Descriptor set/binding 请与 `RhiDescriptorSetLayoutCreateInfo` 保持一致。
- 需要跨 OpenGL 和 Vulkan 共用 GLSL 时，注意两者在 binding、descriptor indexing 和扩展语义上的差异。

