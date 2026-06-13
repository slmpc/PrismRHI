package com.github.slmpc.prismrhi.framegraph;

import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.rendering.RhiRenderingInfo;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RhiFrameGraphPass {
    private final String name;
    private final List<RhiFrameGraphResourceAccess> reads = new ArrayList<>();
    private final List<RhiFrameGraphResourceAccess> writes = new ArrayList<>();
    private RhiRenderingInfo renderingInfo;
    private RhiFrameGraphPassCallback callback = (commandBuffer, pass) -> {
    };

    RhiFrameGraphPass(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("pass name must not be blank");
        }
        this.name = name;
    }

    public String name() {
        return name;
    }

    public List<RhiFrameGraphResourceAccess> reads() {
        return List.copyOf(reads);
    }

    public List<RhiFrameGraphResourceAccess> writes() {
        return List.copyOf(writes);
    }

    public RhiRenderingInfo renderingInfo() {
        return renderingInfo;
    }

    public RhiFrameGraphPass readImage(RhiImage image, RhiResourceState state) {
        reads.add(RhiFrameGraphResourceAccess.image(image, state));
        return this;
    }

    public RhiFrameGraphPass readImage(
            RhiImage image,
            RhiResourceState state,
            Set<RhiImageAspect> aspects,
            int baseMipLevel,
            int levelCount,
            int baseArrayLayer,
            int layerCount
    ) {
        reads.add(RhiFrameGraphResourceAccess.image(
                image,
                state,
                aspects,
                baseMipLevel,
                levelCount,
                baseArrayLayer,
                layerCount
        ));
        return this;
    }

    public RhiFrameGraphPass writeImage(RhiImage image, RhiResourceState state) {
        writes.add(RhiFrameGraphResourceAccess.image(image, state));
        return this;
    }

    public RhiFrameGraphPass writeImage(
            RhiImage image,
            RhiResourceState state,
            Set<RhiImageAspect> aspects,
            int baseMipLevel,
            int levelCount,
            int baseArrayLayer,
            int layerCount
    ) {
        writes.add(RhiFrameGraphResourceAccess.image(
                image,
                state,
                aspects,
                baseMipLevel,
                levelCount,
                baseArrayLayer,
                layerCount
        ));
        return this;
    }

    public RhiFrameGraphPass readBuffer(RhiBuffer buffer, RhiResourceState state) {
        reads.add(RhiFrameGraphResourceAccess.buffer(buffer, state));
        return this;
    }

    public RhiFrameGraphPass readBuffer(RhiBuffer buffer, RhiResourceState state, long offset, long size) {
        reads.add(RhiFrameGraphResourceAccess.buffer(buffer, state, offset, size));
        return this;
    }

    public RhiFrameGraphPass writeBuffer(RhiBuffer buffer, RhiResourceState state) {
        writes.add(RhiFrameGraphResourceAccess.buffer(buffer, state));
        return this;
    }

    public RhiFrameGraphPass writeBuffer(RhiBuffer buffer, RhiResourceState state, long offset, long size) {
        writes.add(RhiFrameGraphResourceAccess.buffer(buffer, state, offset, size));
        return this;
    }

    public RhiFrameGraphPass rendering(RhiRenderingInfo renderingInfo) {
        this.renderingInfo = Objects.requireNonNull(renderingInfo, "renderingInfo");
        return this;
    }

    public RhiFrameGraphPass record(RhiFrameGraphPassCallback callback) {
        this.callback = Objects.requireNonNull(callback, "callback");
        return this;
    }

    void recordInto(com.github.slmpc.prismrhi.command.RhiCommandBuffer commandBuffer) {
        callback.record(commandBuffer, this);
    }
}
