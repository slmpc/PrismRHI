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
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSet;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetAllocateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayout;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorSetLayoutCreateInfo;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorStage;
import com.github.slmpc.prismrhi.descriptor.RhiDescriptorType;
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
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImageUsage;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.resource.RhiImageViewCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiBufferCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiBufferUsage;
import com.github.slmpc.prismrhi.resource.RhiIndexType;
import com.github.slmpc.prismrhi.resource.RhiMemoryUsage;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class VulkanTriangleDemo {
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final RhiFormat DEPTH_FORMAT = RhiFormat.D32_FLOAT;
    private static final int FLOAT_BYTES = Float.BYTES;
    private static final int INDEX_BYTES = Short.BYTES;
    private static final int VERTEX_FLOATS = 8;
    private static final int VERTEX_STRIDE = VERTEX_FLOATS * FLOAT_BYTES;
    private static final float[] CUBE_VERTICES = {
            -0.65f, -0.65f, 0.65f, 1.0f, 0.96f, 0.22f, 0.18f, 1.0f,
            0.65f, -0.65f, 0.65f, 1.0f, 0.96f, 0.22f, 0.18f, 1.0f,
            0.65f, 0.65f, 0.65f, 1.0f, 0.96f, 0.22f, 0.18f, 1.0f,
            -0.65f, 0.65f, 0.65f, 1.0f, 0.96f, 0.22f, 0.18f, 1.0f,

            0.65f, -0.65f, -0.65f, 1.0f, 0.16f, 0.70f, 0.95f, 1.0f,
            -0.65f, -0.65f, -0.65f, 1.0f, 0.16f, 0.70f, 0.95f, 1.0f,
            -0.65f, 0.65f, -0.65f, 1.0f, 0.16f, 0.70f, 0.95f, 1.0f,
            0.65f, 0.65f, -0.65f, 1.0f, 0.16f, 0.70f, 0.95f, 1.0f,

            -0.65f, -0.65f, -0.65f, 1.0f, 0.24f, 0.82f, 0.38f, 1.0f,
            -0.65f, -0.65f, 0.65f, 1.0f, 0.24f, 0.82f, 0.38f, 1.0f,
            -0.65f, 0.65f, 0.65f, 1.0f, 0.24f, 0.82f, 0.38f, 1.0f,
            -0.65f, 0.65f, -0.65f, 1.0f, 0.24f, 0.82f, 0.38f, 1.0f,

            0.65f, -0.65f, 0.65f, 1.0f, 0.98f, 0.76f, 0.20f, 1.0f,
            0.65f, -0.65f, -0.65f, 1.0f, 0.98f, 0.76f, 0.20f, 1.0f,
            0.65f, 0.65f, -0.65f, 1.0f, 0.98f, 0.76f, 0.20f, 1.0f,
            0.65f, 0.65f, 0.65f, 1.0f, 0.98f, 0.76f, 0.20f, 1.0f,

            -0.65f, 0.65f, 0.65f, 1.0f, 0.72f, 0.34f, 0.96f, 1.0f,
            0.65f, 0.65f, 0.65f, 1.0f, 0.72f, 0.34f, 0.96f, 1.0f,
            0.65f, 0.65f, -0.65f, 1.0f, 0.72f, 0.34f, 0.96f, 1.0f,
            -0.65f, 0.65f, -0.65f, 1.0f, 0.72f, 0.34f, 0.96f, 1.0f,

            -0.65f, -0.65f, -0.65f, 1.0f, 0.95f, 0.44f, 0.16f, 1.0f,
            0.65f, -0.65f, -0.65f, 1.0f, 0.95f, 0.44f, 0.16f, 1.0f,
            0.65f, -0.65f, 0.65f, 1.0f, 0.95f, 0.44f, 0.16f, 1.0f,
            -0.65f, -0.65f, 0.65f, 1.0f, 0.95f, 0.44f, 0.16f, 1.0f
    };
    private static final short[] CUBE_INDICES = {
            0, 1, 2, 2, 3, 0,
            4, 5, 6, 6, 7, 4,
            8, 9, 10, 10, 11, 8,
            12, 13, 14, 14, 15, 12,
            16, 17, 18, 18, 19, 16,
            20, 21, 22, 22, 23, 20
    };

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
        RhiBuffer uniformBuffer = createUniformBuffer(device, camera, swapchain.width() / (float) swapchain.height());
        RhiBuffer vertexBuffer = createVertexBuffer(device);
        RhiBuffer indexBuffer = createIndexBuffer(device);
        RhiDescriptorSetLayout sceneLayout = device.createDescriptorSetLayout(
                RhiDescriptorSetLayoutCreateInfo.builder()
                        .binding(0, RhiDescriptorType.UNIFORM_BUFFER, 1, RhiDescriptorStage.VERTEX)
                        .build()
        );
        RhiDescriptorSet sceneSet = device.allocateDescriptorSet(RhiDescriptorSetAllocateInfo.of(sceneLayout));
        sceneSet.update(writer -> writer.uniformBuffer(0, uniformBuffer));

        RhiImage depthImage = device.createImage(
                RhiImageCreateInfo.builder(RhiExtent3D.of2D(swapchain.width(), swapchain.height()))
                        .format(DEPTH_FORMAT)
                        .usage(RhiImageUsage.DEPTH_STENCIL_ATTACHMENT)
                        .build()
        );
        RhiImageView depthView = device.createImageView(RhiImageViewCreateInfo.of(depthImage));

        RhiShaderModule vertexShader = device.createShaderModule(RhiShaderModuleCreateInfo.glsl(
                RhiShaderStage.VERTEX,
                createVertexShaderSource()
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
                        .descriptorSetLayout(sceneLayout)
                        .vertexBinding(0, VERTEX_STRIDE)
                        .vertexAttribute(0, 0, RhiFormat.RGBA32_FLOAT, 0)
                        .vertexAttribute(1, 0, RhiFormat.RGBA32_FLOAT, 4 * FLOAT_BYTES)
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
                        pipeline,
                        sceneSet,
                        vertexBuffer,
                        indexBuffer
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
            sceneSet.close();
            sceneLayout.close();
            indexBuffer.close();
            vertexBuffer.close();
            uniformBuffer.close();
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
            RhiGraphicsPipeline pipeline,
            RhiDescriptorSet sceneSet,
            RhiBuffer vertexBuffer,
            RhiBuffer indexBuffer
    ) {
        commandBuffer.reset();
        commandBuffer.begin();

        RhiFrameGraph graph = RhiFrameGraph.create()
                .resource(targetImage, RhiResourceState.UNDEFINED)
                .resource(depthImage, RhiResourceState.UNDEFINED);
        graph.addPass("scene")
                .writeImage(targetImage, RhiResourceState.COLOR_ATTACHMENT)
                .writeImage(depthImage, RhiResourceState.DEPTH_STENCIL_ATTACHMENT)
                .rendering(RhiRenderingInfo.builder(RhiRect2D.of(swapchain.width(), swapchain.height()))
                        .color(RhiRenderingAttachment.clearColor(targetView, 0.02f, 0.025f, 0.03f, 1.0f))
                        .depth(RhiRenderingAttachment.depth(depthView, RhiAttachmentLoadOp.CLEAR, 1.0f))
                        .build())
                .record((cmd, pass) -> {
                    cmd.bindGraphicsPipeline(pipeline);
                    cmd.bindDescriptorSet(pipeline, 0, sceneSet);
                    cmd.bindVertexBuffer(0, vertexBuffer, 0);
                    cmd.bindIndexBuffer(indexBuffer, 0, RhiIndexType.UINT16);
                    cmd.drawIndexed(CUBE_INDICES.length);
                });
        graph.addPass("present")
                .writeImage(targetImage, RhiResourceState.PRESENT)
                .record((cmd, pass) -> {
                });
        graph.execute(commandBuffer);

        commandBuffer.end();
    }

    private static RhiBuffer createUniformBuffer(
            com.github.slmpc.prismrhi.device.RhiDevice device,
            Camera camera,
            float aspectRatio
    ) {
        RhiBuffer buffer = device.createBuffer(
                RhiBufferCreateInfo.builder(16L * FLOAT_BYTES)
                        .usage(RhiBufferUsage.UNIFORM_BUFFER)
                        .memoryUsage(RhiMemoryUsage.CPU_TO_GPU)
                        .build()
        );
        buffer.write(matrixBuffer(camera.viewProjection(aspectRatio)));
        return buffer;
    }

    private static RhiBuffer createVertexBuffer(com.github.slmpc.prismrhi.device.RhiDevice device) {
        RhiBuffer buffer = device.createBuffer(
                RhiBufferCreateInfo.builder((long) CUBE_VERTICES.length * FLOAT_BYTES)
                        .usage(RhiBufferUsage.VERTEX_BUFFER)
                        .memoryUsage(RhiMemoryUsage.CPU_TO_GPU)
                        .build()
        );
        buffer.write(floatBuffer(CUBE_VERTICES));
        return buffer;
    }

    private static RhiBuffer createIndexBuffer(com.github.slmpc.prismrhi.device.RhiDevice device) {
        RhiBuffer buffer = device.createBuffer(
                RhiBufferCreateInfo.builder((long) CUBE_INDICES.length * INDEX_BYTES)
                        .usage(RhiBufferUsage.INDEX_BUFFER)
                        .memoryUsage(RhiMemoryUsage.CPU_TO_GPU)
                        .build()
        );
        buffer.write(shortBuffer(CUBE_INDICES));
        return buffer;
    }

    private static String createVertexShaderSource() {
        return """
                #version 450

                layout(set = 0, binding = 0) uniform CameraUniforms {
                    mat4 viewProjection;
                } camera;

                layout(location = 0) in vec4 inPosition;
                layout(location = 1) in vec4 inColor;
                layout(location = 0) out vec3 outColor;

                void main() {
                    gl_Position = camera.viewProjection * inPosition;
                    outColor = inColor.rgb;
                }
                """;
    }

    private static ByteBuffer matrixBuffer(Matrix4f matrix) {
        float[] values = new float[16];
        matrix.get(values);
        return floatBuffer(values);
    }

    private static ByteBuffer floatBuffer(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length * FLOAT_BYTES).order(ByteOrder.nativeOrder());
        for (float value : values) {
            buffer.putFloat(value);
        }
        return buffer.flip();
    }

    private static ByteBuffer shortBuffer(short[] values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length * INDEX_BYTES).order(ByteOrder.nativeOrder());
        for (short value : values) {
            buffer.putShort(value);
        }
        return buffer.flip();
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
