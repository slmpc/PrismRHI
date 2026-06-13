package com.github.slmpc.prismrhi.demo.triangle;

import com.github.slmpc.prismrhi.PrismRHI;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.backend.BackendFeature;
import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiCommandPoolCreateInfo;
import com.github.slmpc.prismrhi.context.RhiContext;
import com.github.slmpc.prismrhi.context.RhiContextCreateInfo;
import com.github.slmpc.prismrhi.device.RhiDeviceCreateInfo;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.framegraph.RhiFrameGraph;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiCullMode;
import com.github.slmpc.prismrhi.pipeline.RhiDynamicRenderingState;
import com.github.slmpc.prismrhi.pipeline.RhiFrontFace;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiRasterizationState;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiViewport;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.shader.RhiShaderModule;
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;
import com.github.slmpc.prismrhi.swapchain.RhiSwapchain;
import com.github.slmpc.prismrhi.swapchain.RhiSwapchainCreateInfo;
import com.github.slmpc.prismrhi.sync.RhiPipelineStage;
import com.github.slmpc.prismrhi.sync.RhiSemaphore;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.queue.RhiSubmitInfo;

public final class VulkanTriangleDemo {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;

    private VulkanTriangleDemo() {
    }

    public static void main(String[] args) {
        boolean enableValidation = isValidationRequested(args);
        RhiContextCreateInfo contextInfo = RhiContextCreateInfo.autoGlfwWindow(WIDTH, HEIGHT, "PrismRHI Vulkan Triangle")
                .resizable(false)
                .build();

        var instance = PrismRHI.createInstance(
                RhiInstanceCreateInfo.builder(BackendApi.VULKAN)
                        .applicationName("PrismRHI Triangle Demo")
                        .enableValidation(enableValidation)
                        .context(contextInfo)
                        .build()
        );

        RhiContext context = instance.context().orElseThrow();
        var physicalDevice = instance.enumeratePhysicalDevices().get(0);
        var device = instance.createDevice(
                physicalDevice,
                RhiDeviceCreateInfo.builder()
                        .debugName("Triangle Demo Device")
                        .context(context)
                        .enableFeature(BackendFeature.DYNAMIC_RENDERING)
                        .build()
        );

        var graphicsQueue = device.queue(RhiQueueType.GRAPHICS);
        RhiSwapchain swapchain = device.createSwapchain(
                RhiSwapchainCreateInfo.builder(context)
                        .preferredImageCount(2)
                        .preferredFormat(RhiFormat.BGRA8_SRGB)
                        .vsync(true)
                        .build()
        );

        RhiShaderModule vertexShader = device.createShaderModule(RhiShaderModuleCreateInfo.glsl(
                RhiShaderStage.VERTEX,
                """
                #version 450

                layout(location = 0) out vec3 outColor;

                vec2 positions[3] = vec2[](
                    vec2(0.0, -0.55),
                    vec2(0.55, 0.45),
                    vec2(-0.55, 0.45)
                );

                vec3 colors[3] = vec3[](
                    vec3(0.95, 0.20, 0.18),
                    vec3(0.18, 0.82, 0.36),
                    vec3(0.20, 0.45, 1.00)
                );

                void main() {
                    gl_Position = vec4(positions[gl_VertexIndex], 0.0, 1.0);
                    outColor = colors[gl_VertexIndex];
                }
                """
        ));
        RhiShaderModule fragmentShader = device.createShaderModule(RhiShaderModuleCreateInfo.glsl(
                RhiShaderStage.FRAGMENT,
                """
                #version 450

                layout(location = 0) in vec3 inColor;
                layout(location = 0) out vec4 outColor;

                void main() {
                    outColor = vec4(inColor, 1.0);
                }
                """
        ));

        RhiGraphicsPipeline pipeline = device.createGraphicsPipeline(
                RhiGraphicsPipelineCreateInfo.builder()
                        .shader(RhiShaderStage.VERTEX, vertexShader)
                        .shader(RhiShaderStage.FRAGMENT, fragmentShader)
                        .rendering(RhiDynamicRenderingState.color(swapchain.format()))
                        .rasterization(new RhiRasterizationState(
                                null,
                                RhiCullMode.NONE,
                                RhiFrontFace.COUNTER_CLOCKWISE,
                                1.0f
                        ))
                        .build()
        );

        var commandPool = device.createCommandPool(
                new RhiCommandPoolCreateInfo(RhiQueueType.GRAPHICS, false, true)
        );
        RhiCommandBuffer commandBuffer = commandPool.allocateCommandBuffer(RhiCommandBufferLevel.PRIMARY);
        RhiSemaphore imageAvailable = device.createSemaphore();
        RhiSemaphore renderFinished = device.createSemaphore();

        try {
            while (!context.shouldClose()) {
                context.pollEvents();

                int imageIndex = swapchain.acquireNextImage(imageAvailable);
                var swapchainImage = swapchain.image(imageIndex);
                recordFrame(commandBuffer, swapchainImage.image(), swapchainImage.view(), swapchain, pipeline);

                graphicsQueue.submit(RhiSubmitInfo.builder()
                        .wait(imageAvailable, RhiPipelineStage.COLOR_ATTACHMENT_OUTPUT)
                        .commandBuffer(commandBuffer)
                        .signal(renderFinished)
                        .build());

                swapchain.present(graphicsQueue, imageIndex, renderFinished);
                graphicsQueue.waitIdle();
            }
            device.waitIdle();
        } finally {
            device.waitIdle();
            renderFinished.close();
            imageAvailable.close();
            commandPool.close();
            pipeline.close();
            fragmentShader.close();
            vertexShader.close();
            swapchain.close();
            device.close();
            instance.close();
        }
    }

    private static boolean isValidationRequested(String[] args) {
        for (String arg : args) {
            if ("--vulkanValidation".equals(arg) || "--vulkan-validation".equals(arg)) {
                return true;
            }
        }
        return "1".equals(System.getenv("VULKAN_VALIDATION_LAYER"))
                || Boolean.getBoolean("prismrhi.vulkanValidation");
    }

    private static void recordFrame(
            RhiCommandBuffer commandBuffer,
            RhiImage targetImage,
            com.github.slmpc.prismrhi.resource.RhiImageView targetView,
            RhiSwapchain swapchain,
            RhiGraphicsPipeline pipeline
    ) {
        commandBuffer.reset();
        commandBuffer.begin();

        RhiFrameGraph graph = RhiFrameGraph.create()
                .resource(targetImage, RhiResourceState.UNDEFINED);
        graph.addPass("triangle")
                .writeImage(targetImage, RhiResourceState.COLOR_ATTACHMENT)
                .record((cmd, pass) -> {
                    cmd.beginRendering(RhiRenderingInfo.builder(RhiRect2D.of(swapchain.width(), swapchain.height()))
                            .color(RhiRenderingAttachment.clearColor(targetView, 0.02f, 0.025f, 0.03f, 1.0f))
                            .build());
                    cmd.setViewport(RhiViewport.of(swapchain.width(), swapchain.height()));
                    cmd.setScissor(RhiRect2D.of(swapchain.width(), swapchain.height()));
                    cmd.bindGraphicsPipeline(pipeline);
                    cmd.draw(3);
                    cmd.endRendering();
                });
        graph.addPass("present")
                .writeImage(targetImage, RhiResourceState.PRESENT)
                .record((cmd, pass) -> {
                });
        graph.execute(commandBuffer);

        commandBuffer.end();
    }
}
