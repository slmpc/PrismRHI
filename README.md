# PrismRHI

PrismRHI is a LWJGL-based Java rendering hardware interface with a Vulkan-style API shape and optional backend modules.

## Modules

- `prism-rhi-core`: backend-neutral RHI API, create-info records, resource handles, queue and command abstractions.
- `prism-rhi-backend-opengl41`: OpenGL 4.1-compatible backend using bind-to-edit calls.
- `prism-rhi-backend-opengl-dsa`: modern OpenGL backend requiring OpenGL 4.5 DSA support.
- `prism-rhi-backend-vulkan`: Vulkan backend using LWJGL Vulkan and LWJGL VMA.
- `prism-rhi-shaderc`: optional LWJGL shaderc integration for compiling GLSL to SPIR-V.
- `prism-rhi-demo-triangle`: runnable Vulkan triangle demo using an RHI-managed GLFW window and swapchain.

Applications should depend on `prism-rhi-core` plus only the backend jars they want to ship. Backends are discovered with Java `ServiceLoader`, so omitting a backend module also omits its LWJGL backend dependency from the package.

## Vulkan Triangle Demo

Run the windowed Vulkan demo with:

```bash
./gradlew :prism-rhi-demo-triangle:runTriangleDemo
```

The demo uses `RhiContextCreateInfo.autoGlfwWindow(...)` so the Vulkan backend creates the GLFW window, surface, device, swapchain, semaphores, and dynamic-rendering frame. Applications can also pass an existing GLFW window or native Vulkan surface through `RhiContextCreateInfo.glfwWindow(...)` / `externalSurface(...)`.

On macOS the Gradle run task automatically adds `-XstartOnFirstThread`, which GLFW requires for window creation.

## Backend Selection

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

You can inspect available backend providers before creating an instance:

```java
import com.github.slmpc.prismrhi.PrismRHI;
import com.github.slmpc.prismrhi.backend.RhiBackendProvider;

for (RhiBackendProvider provider : PrismRHI.providers()) {
    System.out.println(provider.info().displayName() + ": " + provider.isSupported());
}
```

## OpenGL Context Ownership

The OpenGL backends expect the application to create and make current an OpenGL context first, for example with GLFW. The RHI then wraps the current context and exposes a Vulkan-style device, queue, resource, and command-pool surface.

`OPENGL_41` targets compatibility and avoids DSA entry points. `OPENGL_DSA` requires an OpenGL 4.5 context and uses calls such as `glCreateBuffers`, `glNamedBufferData`, `glCreateTextures`, and `glTextureStorage2D`.

## Vulkan Memory

The Vulkan backend creates a VMA allocator per logical device. Buffer and image creation goes through `vmaCreateBuffer` and `vmaCreateImage`, with `RhiMemoryUsage` mapped to VMA automatic memory usage preferences.

## GLSL and Shaderc

OpenGL backends accept GLSL shader modules directly. The Vulkan backend accepts SPIR-V directly, and can also accept GLSL when the optional `prism-rhi-shaderc` module is on the classpath. Shader compilers are discovered through `ServiceLoader`, so applications only ship shaderc when they choose that module.

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

You can also use the compiler explicitly:

```java
var spirvInfo = RhiShaderCompilers.compile(
        RhiShaderModuleCreateInfo.glsl(RhiShaderStage.FRAGMENT, fragmentSource),
        RhiShaderCodeType.SPIRV,
        RhiShaderCompileOptions.builder()
                .sourceName("fragment.glsl")
                .optimizationLevel(RhiShaderOptimizationLevel.PERFORMANCE)
                .build()
);
```

## Graphics Pipelines and Dynamic Rendering

Graphics pipelines are declared with attachment formats instead of render passes. The Vulkan backend creates pipelines with `VkPipelineRenderingCreateInfo` and records rendering with `vkCmdBeginRendering`, while the OpenGL backends map the same RHI shape to transient framebuffers.

```java
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.pipeline.RhiDynamicRenderingState;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputRate;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiViewport;
import com.github.slmpc.prismrhi.resource.RhiImageViewCreateInfo;

var colorView = device.createImageView(RhiImageViewCreateInfo.of(colorImage));

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

cmd.begin();
cmd.beginRendering(RhiRenderingInfo.builder(RhiRect2D.of(width, height))
        .color(RhiRenderingAttachment.clearColor(colorView, 0.02f, 0.02f, 0.025f, 1.0f))
        .build());
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

## Frame Graph

PrismRHI includes a small frame graph that tracks declared resource states and inserts pass-boundary barriers automatically. Pass callbacks still record normal RHI commands, so the graph stays close to the Vulkan mental model without hiding command buffer control.

```java
import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.framegraph.RhiFrameGraph;

var graph = RhiFrameGraph.create()
        .resource(gbufferColor, RhiResourceState.UNDEFINED)
        .resource(depthImage, RhiResourceState.UNDEFINED);

graph.addPass("gbuffer")
        .writeImage(gbufferColor, RhiResourceState.COLOR_ATTACHMENT)
        .writeImage(depthImage, RhiResourceState.DEPTH_STENCIL_ATTACHMENT)
        .record((cmd, pass) -> {
            cmd.beginRendering(gbufferRendering);
            cmd.bindGraphicsPipeline(gbufferPipeline);
            cmd.drawIndexed(gbufferIndexCount);
            cmd.endRendering();
        });

graph.addPass("lighting")
        .readImage(gbufferColor, RhiResourceState.SAMPLED_IMAGE)
        .readImage(depthImage, RhiResourceState.SAMPLED_IMAGE)
        .writeImage(backbuffer, RhiResourceState.COLOR_ATTACHMENT)
        .record((cmd, pass) -> {
            cmd.beginRendering(lightingRendering);
            cmd.bindGraphicsPipeline(lightingPipeline);
            cmd.bindDescriptorSet(lightingPipeline, 0, lightingSet);
            cmd.draw(3);
            cmd.endRendering();
        });

cmd.begin();
graph.execute(cmd);
cmd.end();
```

The Vulkan backend maps frame graph transitions to `vkCmdPipelineBarrier` image and buffer barriers, including image layout transitions. OpenGL backends accept the same graph and leave barriers as no-ops because OpenGL synchronizes this class of draw/texture/framebuffer usage implicitly.

## Descriptors

Descriptors are intentionally compact: create one layout, allocate sets from it, then update with a writer.

```java
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetAllocateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorStage;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorType;

var layout = device.createDescriptorSetLayout(
        RhiDescriptorSetLayoutCreateInfo.builder()
                .binding(0, RhiDescriptorType.UNIFORM_BUFFER, 1, RhiDescriptorStage.ALL_GRAPHICS)
                .bindless(1, RhiDescriptorType.SAMPLED_IMAGE, 4096, RhiDescriptorStage.FRAGMENT)
                .build()
);

var set = device.allocateDescriptorSet(RhiDescriptorSetAllocateInfo.bindless(layout, 1024));
set.update(writer -> writer.uniformBuffer(0, cameraBuffer));
```

Image descriptors use image views and optional samplers:

```java
var sampler = device.createSampler(RhiSamplerCreateInfo.linearRepeat());
set.update(writer -> writer.combinedImageSampler(1, 0, colorView, sampler));
```

`bindless` enables descriptor indexing flags for that binding: update-after-bind, update-unused-while-pending, partially-bound, and variable descriptor count. The Vulkan backend enables descriptor indexing features during logical device creation and maps those flags to Vulkan 1.2 descriptor indexing. OpenGL backends keep the same RHI-facing descriptor shape and bind buffers/textures when descriptor sets are bound on a command buffer.

## Draw Commands

`RhiCommandBuffer` exposes Vulkan-style draw commands:

- `draw` / `drawInstanced`
- `drawIndexed` / `drawIndexedInstanced`
- `multiDraw` / `multiDrawIndexed`
- `drawIndirect` / `drawIndexedIndirect`
- `multiDrawIndirect` / `multiDrawIndexedIndirect`
- `multiDrawIndirectCount` / `multiDrawIndexedIndirectCount`

Indirect commands use `RhiBuffer` plus byte offsets so the same API maps to Vulkan buffers and OpenGL indirect buffers. Packed indirect strides use Vulkan-compatible layouts: 16 bytes for non-indexed indirect draws and 20 bytes for indexed indirect draws.

`multiDraw` and `multiDrawIndexed` are true multi-draw commands, not loops over `draw`. OpenGL backends use `glMultiDrawArrays` and `glMultiDrawElements`; Vulkan uses `VK_EXT_multi_draw` and throws `RhiException` if the extension is unavailable. Because true multi-draw APIs share instance parameters across all draws, `RhiMultiDrawCommand` and `RhiMultiDrawIndexedCommand` require a common `instanceCount` and `firstInstance`.

OpenGL backends expose `setPrimitiveTopology` and `setIndexType` on the command buffer because OpenGL draw calls need those values at call time. Vulkan topology is controlled by pipeline state, and index type is supplied through `bindIndexBuffer`.

`OPENGL_41` supports direct and instanced draw calls, plus single indirect draws. It throws `RhiException` for unsupported Vulkan-style parameters such as `firstInstance`, indexed `vertexOffset`, multi indirect, and indirect count draws. `OPENGL_DSA` supports modern base-instance and multi-indirect calls; indirect count draws require OpenGL 4.6 or `GL_ARB_indirect_parameters`.
