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
import com.github.slmpc.prismrhi.format.RhiExtent3D;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.framegraph.RhiFrameGraph;
import com.github.slmpc.prismrhi.pipeline.RhiDepthStencilState;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiCullMode;
import com.github.slmpc.prismrhi.pipeline.RhiDynamicRenderingState;
import com.github.slmpc.prismrhi.pipeline.RhiFrontFace;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipeline;
import com.github.slmpc.prismrhi.pipeline.RhiGraphicsPipelineCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiRasterizationState;
import com.github.slmpc.prismrhi.rendering.RhiAttachmentLoadOp;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.rendering.RhiViewport;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImageUsage;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.resource.RhiImageViewCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderModule;
import com.github.slmpc.prismrhi.shader.RhiShaderModuleCreateInfo;
import com.github.slmpc.prismrhi.shader.RhiShaderStage;
import com.github.slmpc.prismrhi.swapchain.RhiSwapchain;
import com.github.slmpc.prismrhi.swapchain.RhiSwapchainCreateInfo;
import com.github.slmpc.prismrhi.sync.RhiPipelineStage;
import com.github.slmpc.prismrhi.sync.RhiSemaphore;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.queue.RhiSubmitInfo;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class VulkanTriangleDemo {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final RhiFormat DEPTH_FORMAT = RhiFormat.D32_FLOAT;

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

        Camera camera = Camera.lookAt(
                new Vector3f(2.4f, 1.8f, 3.2f),
                new Vector3f(0.0f, 0.0f, 0.0f)
        );
        String vertexShaderSource = createVertexShaderSource(
                camera,
                swapchain.width() / (float) swapchain.height()
        );
        RhiImage depthImage = device.createImage(
                RhiImageCreateInfo.builder(RhiExtent3D.of2D(swapchain.width(), swapchain.height()))
                        .format(DEPTH_FORMAT)
                        .usage(RhiImageUsage.DEPTH_STENCIL_ATTACHMENT)
                        .build()
        );
        RhiImageView depthView = device.createImageView(RhiImageViewCreateInfo.of(depthImage));

        RhiShaderModule vertexShader = device.createShaderModule(RhiShaderModuleCreateInfo.glsl(
                RhiShaderStage.VERTEX,
                vertexShaderSource
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
                        .rendering(RhiDynamicRenderingState.builder()
                                .color(swapchain.format())
                                .depth(DEPTH_FORMAT)
                                .build())
                        .depthStencil(RhiDepthStencilState.depthTest())
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
                recordFrame(
                        commandBuffer,
                        swapchainImage.image(),
                        swapchainImage.view(),
                        depthImage,
                        depthView,
                        swapchain,
                        pipeline
                );

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
            depthView.close();
            depthImage.close();
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
            RhiImageView targetView,
            RhiImage depthImage,
            RhiImageView depthView,
            RhiSwapchain swapchain,
            RhiGraphicsPipeline pipeline
    ) {
        commandBuffer.reset();
        commandBuffer.begin();

        RhiFrameGraph graph = RhiFrameGraph.create()
                .resource(targetImage, RhiResourceState.UNDEFINED)
                .resource(depthImage, RhiResourceState.UNDEFINED);
        graph.addPass("scene")
                .writeImage(targetImage, RhiResourceState.COLOR_ATTACHMENT)
                .writeImage(depthImage, RhiResourceState.DEPTH_STENCIL_ATTACHMENT)
                .record((cmd, pass) -> {
                    cmd.beginRendering(RhiRenderingInfo.builder(RhiRect2D.of(swapchain.width(), swapchain.height()))
                            .color(RhiRenderingAttachment.clearColor(targetView, 0.02f, 0.025f, 0.03f, 1.0f))
                            .depth(RhiRenderingAttachment.depth(depthView, RhiAttachmentLoadOp.CLEAR, 1.0f))
                            .build());
                    cmd.setViewport(RhiViewport.of(swapchain.width(), swapchain.height()));
                    cmd.setScissor(RhiRect2D.of(swapchain.width(), swapchain.height()));
                    cmd.bindGraphicsPipeline(pipeline);
                    cmd.draw(36);
                    cmd.endRendering();
                });
        graph.addPass("present")
                .writeImage(targetImage, RhiResourceState.PRESENT)
                .record((cmd, pass) -> {
                });
        graph.execute(commandBuffer);

        commandBuffer.end();
    }

    private static String createVertexShaderSource(Camera camera, float aspectRatio) {
        return """
                #version 450

                layout(location = 0) out vec3 outColor;

                const mat4 VIEW_PROJECTION = %s;

                vec3 positions[36] = vec3[](
                    vec3(-0.65, -0.65,  0.65), vec3( 0.65, -0.65,  0.65), vec3( 0.65,  0.65,  0.65),
                    vec3( 0.65,  0.65,  0.65), vec3(-0.65,  0.65,  0.65), vec3(-0.65, -0.65,  0.65),

                    vec3( 0.65, -0.65, -0.65), vec3(-0.65, -0.65, -0.65), vec3(-0.65,  0.65, -0.65),
                    vec3(-0.65,  0.65, -0.65), vec3( 0.65,  0.65, -0.65), vec3( 0.65, -0.65, -0.65),

                    vec3(-0.65, -0.65, -0.65), vec3(-0.65, -0.65,  0.65), vec3(-0.65,  0.65,  0.65),
                    vec3(-0.65,  0.65,  0.65), vec3(-0.65,  0.65, -0.65), vec3(-0.65, -0.65, -0.65),

                    vec3( 0.65, -0.65,  0.65), vec3( 0.65, -0.65, -0.65), vec3( 0.65,  0.65, -0.65),
                    vec3( 0.65,  0.65, -0.65), vec3( 0.65,  0.65,  0.65), vec3( 0.65, -0.65,  0.65),

                    vec3(-0.65,  0.65,  0.65), vec3( 0.65,  0.65,  0.65), vec3( 0.65,  0.65, -0.65),
                    vec3( 0.65,  0.65, -0.65), vec3(-0.65,  0.65, -0.65), vec3(-0.65,  0.65,  0.65),

                    vec3(-0.65, -0.65, -0.65), vec3( 0.65, -0.65, -0.65), vec3( 0.65, -0.65,  0.65),
                    vec3( 0.65, -0.65,  0.65), vec3(-0.65, -0.65,  0.65), vec3(-0.65, -0.65, -0.65)
                );

                vec3 faceColors[6] = vec3[](
                    vec3(0.96, 0.22, 0.18),
                    vec3(0.16, 0.70, 0.95),
                    vec3(0.24, 0.82, 0.38),
                    vec3(0.98, 0.76, 0.20),
                    vec3(0.72, 0.34, 0.96),
                    vec3(0.95, 0.44, 0.16)
                );

                void main() {
                    vec3 position = positions[gl_VertexIndex];
                    gl_Position = VIEW_PROJECTION * vec4(position, 1.0);
                    outColor = faceColors[gl_VertexIndex / 6];
                }
                """.formatted(glslMat4(camera.viewProjection(aspectRatio)));
    }

    private static String glslMat4(Matrix4f matrix) {
        float[] values = new float[16];
        matrix.get(values);

        StringBuilder builder = new StringBuilder("mat4(");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(Float.toString(values[i]));
        }
        return builder.append(')').toString();
    }

    private static final class Camera {
        private final Vector3f position;
        private final Vector3f target;
        private final Vector3f up;
        private final float fieldOfViewRadians;
        private final float nearPlane;
        private final float farPlane;

        private Camera(
                Vector3f position,
                Vector3f target,
                Vector3f up,
                float fieldOfViewRadians,
                float nearPlane,
                float farPlane
        ) {
            this.position = new Vector3f(position);
            this.target = new Vector3f(target);
            this.up = new Vector3f(up);
            this.fieldOfViewRadians = fieldOfViewRadians;
            this.nearPlane = nearPlane;
            this.farPlane = farPlane;
        }

        static Camera lookAt(Vector3f position, Vector3f target) {
            return new Camera(
                    position,
                    target,
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    (float) Math.toRadians(55.0),
                    0.1f,
                    100.0f
            );
        }

        Matrix4f viewProjection(float aspectRatio) {
            Matrix4f view = new Matrix4f().lookAt(position, target, up);
            Matrix4f projection = new Matrix4f()
                    .setPerspective(fieldOfViewRadians, aspectRatio, nearPlane, farPlane, true);
            projection.m11(-projection.m11());
            return projection.mul(view);
        }
    }
}
