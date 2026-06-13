package com.github.slmpc.prismrhi;

import com.github.slmpc.prismrhi.barrier.RhiPipelineBarrier;
import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiDrawCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndexedCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndexedIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiDrawIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndexedIndirectCountCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndirectCommand;
import com.github.slmpc.prismrhi.command.RhiMultiDrawIndirectCountCommand;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.framegraph.RhiFrameGraph;
import com.github.slmpc.prismrhi.instance.RhiInstanceCreateInfo;
import com.github.slmpc.prismrhi.pipeline.RhiDynamicRenderingState;
import com.github.slmpc.prismrhi.pipeline.RhiVertexAttribute;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputBinding;
import com.github.slmpc.prismrhi.pipeline.RhiVertexInputRate;
import com.github.slmpc.prismrhi.rendering.RhiRect2D;
import com.github.slmpc.prismrhi.rendering.RhiRenderingAttachment;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.resource.RhiBufferCreateInfo;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RhiCreateInfoTest {
    @Test
    void instanceCreateInfoKeepsVulkanStyleDefaults() {
        var info = RhiInstanceCreateInfo.builder(BackendApi.VULKAN)
                .applicationName("Sample")
                .enableValidation(true)
                .addExtension("VK_KHR_surface")
                .build();

        assertEquals(BackendApi.VULKAN, info.backend());
        assertEquals("Sample", info.applicationName());
        assertEquals(1, info.enabledExtensions().size());
    }

    @Test
    void bufferSizeMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> RhiBufferCreateInfo.builder(0).build());
    }

    @Test
    void dynamicRenderingInfoKeepsAttachmentsCompact() {
        RhiImage image = new TestImage();
        RhiImageView view = new TestImageView(image);
        var rendering = RhiRenderingInfo.builder(RhiRect2D.of(1280, 720))
                .color(RhiRenderingAttachment.clearColor(view, 0.1f, 0.2f, 0.3f, 1.0f))
                .build();

        assertEquals(1, rendering.colorAttachments().size());
        assertEquals(1280, rendering.renderArea().extent().width());
    }

    @Test
    void pipelineRenderingStateSupportsVertexLayout() {
        var state = RhiDynamicRenderingState.builder()
                .color(RhiFormat.BGRA8_UNORM)
                .depth(RhiFormat.D32_FLOAT)
                .build();
        var binding = new RhiVertexInputBinding(0, 32, RhiVertexInputRate.INSTANCE);
        var attribute = new RhiVertexAttribute(1, 0, RhiFormat.RGBA32_FLOAT, 16);

        assertEquals(RhiFormat.BGRA8_UNORM, state.colorAttachmentFormats().get(0));
        assertEquals(RhiFormat.D32_FLOAT, state.depthAttachmentFormat());
        assertEquals(RhiVertexInputRate.INSTANCE, binding.inputRate());
        assertEquals(16, attribute.offset());
    }

    @Test
    void frameGraphInsertsBarriersBetweenPasses() {
        RhiImage image = new TestImage();
        var graph = RhiFrameGraph.create()
                .resource(image, RhiResourceState.UNDEFINED);
        graph.addPass("gbuffer")
                .writeImage(image, RhiResourceState.COLOR_ATTACHMENT);
        graph.addPass("lighting")
                .readImage(image, RhiResourceState.SAMPLED_IMAGE);

        var commandBuffer = new CapturingCommandBuffer();
        graph.execute(commandBuffer);

        assertEquals(2, commandBuffer.barrierCount);
        assertEquals(RhiResourceState.UNDEFINED, commandBuffer.barriers.get(0).imageBarriers().get(0).oldState());
        assertEquals(RhiResourceState.COLOR_ATTACHMENT, commandBuffer.barriers.get(0).imageBarriers().get(0).newState());
        assertEquals(RhiResourceState.COLOR_ATTACHMENT, commandBuffer.barriers.get(1).imageBarriers().get(0).oldState());
        assertEquals(RhiResourceState.SAMPLED_IMAGE, commandBuffer.barriers.get(1).imageBarriers().get(0).newState());
    }

    @Test
    void multiDrawRequiresSharedInstanceState() {
        assertThrows(IllegalArgumentException.class, () -> new RhiMultiDrawCommand(List.of(
                new RhiDrawCommand(3, 1, 0, 0),
                new RhiDrawCommand(3, 2, 3, 0)
        )));
        assertThrows(IllegalArgumentException.class, () -> new RhiMultiDrawIndexedCommand(List.of(
                new RhiDrawIndexedCommand(3, 1, 0, 0, 0),
                new RhiDrawIndexedCommand(3, 1, 3, 0, 1)
        )));
    }

    private record TestImage() implements RhiImage {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public long nativeHandle() {
            return 1L;
        }

        @Override
        public com.github.slmpc.prismrhi.format.RhiExtent3D extent() {
            return com.github.slmpc.prismrhi.format.RhiExtent3D.of2D(1280, 720);
        }

        @Override
        public RhiFormat format() {
            return RhiFormat.RGBA8_UNORM;
        }

        @Override
        public void close() {
        }
    }

    private record TestImageView(RhiImage image) implements RhiImageView {
        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public long nativeHandle() {
            return 2L;
        }

        @Override
        public RhiFormat format() {
            return image.format();
        }

        @Override
        public Set<RhiImageAspect> aspects() {
            return Set.of(RhiImageAspect.COLOR);
        }

        @Override
        public void close() {
        }
    }

    private static final class CapturingCommandBuffer implements RhiCommandBuffer {
        private int barrierCount;
        private final List<RhiPipelineBarrier> barriers = new ArrayList<>();

        @Override
        public BackendApi api() {
            return BackendApi.VULKAN;
        }

        @Override
        public long nativeHandle() {
            return 0;
        }

        @Override
        public RhiCommandBufferLevel level() {
            return RhiCommandBufferLevel.PRIMARY;
        }

        @Override
        public void begin() {
        }

        @Override
        public void end() {
        }

        @Override
        public void pipelineBarrier(RhiPipelineBarrier barrier) {
            barrierCount++;
            barriers.add(barrier);
        }

        @Override
        public void draw(RhiDrawCommand command) {
        }

        @Override
        public void drawIndexed(RhiDrawIndexedCommand command) {
        }

        @Override
        public void multiDraw(RhiMultiDrawCommand command) {
        }

        @Override
        public void multiDrawIndexed(RhiMultiDrawIndexedCommand command) {
        }

        @Override
        public void drawIndirect(RhiDrawIndirectCommand command) {
        }

        @Override
        public void drawIndexedIndirect(RhiDrawIndexedIndirectCommand command) {
        }

        @Override
        public void multiDrawIndirect(RhiMultiDrawIndirectCommand command) {
        }

        @Override
        public void multiDrawIndexedIndirect(RhiMultiDrawIndexedIndirectCommand command) {
        }

        @Override
        public void multiDrawIndirectCount(RhiMultiDrawIndirectCountCommand command) {
        }

        @Override
        public void multiDrawIndexedIndirectCount(RhiMultiDrawIndexedIndirectCountCommand command) {
        }

        @Override
        public void close() {
        }
    }
}
