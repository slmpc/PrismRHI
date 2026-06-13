package com.github.slmpc.prismrhi.framegraph;

import com.github.slmpc.prismrhi.barrier.RhiResourceState;
import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;
import com.github.slmpc.prismrhi.resource.RhiResource;

import java.util.Objects;
import java.util.Set;

public record RhiFrameGraphResourceAccess(
        RhiResource resource,
        RhiResourceState state,
        Set<RhiImageAspect> imageAspects,
        int baseMipLevel,
        int levelCount,
        int baseArrayLayer,
        int layerCount,
        long bufferOffset,
        long bufferSize
) {
    public RhiFrameGraphResourceAccess {
        resource = Objects.requireNonNull(resource, "resource");
        state = Objects.requireNonNull(state, "state");
        imageAspects = Set.copyOf(imageAspects == null ? Set.of() : imageAspects);
        if (!(resource instanceof RhiImage) && !(resource instanceof RhiBuffer)) {
            throw new IllegalArgumentException("frame graph access supports RhiImage and RhiBuffer resources only");
        }
        if (baseMipLevel < 0 || levelCount < 1) {
            throw new IllegalArgumentException("mip range must be valid");
        }
        if (baseArrayLayer < 0 || layerCount < 1) {
            throw new IllegalArgumentException("array layer range must be valid");
        }
        if (bufferOffset < 0 || bufferSize < 0) {
            throw new IllegalArgumentException("buffer range must not be negative");
        }
    }

    public static RhiFrameGraphResourceAccess image(RhiImage image, RhiResourceState state) {
        return new RhiFrameGraphResourceAccess(image, state, Set.of(), 0, 1, 0, 1, 0, 0);
    }

    public static RhiFrameGraphResourceAccess image(
            RhiImage image,
            RhiResourceState state,
            Set<RhiImageAspect> aspects,
            int baseMipLevel,
            int levelCount,
            int baseArrayLayer,
            int layerCount
    ) {
        return new RhiFrameGraphResourceAccess(
                image,
                state,
                aspects,
                baseMipLevel,
                levelCount,
                baseArrayLayer,
                layerCount,
                0,
                0
        );
    }

    public static RhiFrameGraphResourceAccess buffer(RhiBuffer buffer, RhiResourceState state) {
        return new RhiFrameGraphResourceAccess(buffer, state, Set.of(), 0, 1, 0, 1, 0, 0);
    }

    public static RhiFrameGraphResourceAccess buffer(RhiBuffer buffer, RhiResourceState state, long offset, long size) {
        return new RhiFrameGraphResourceAccess(buffer, state, Set.of(), 0, 1, 0, 1, offset, size);
    }

    public boolean writes() {
        return state.writes();
    }
}
