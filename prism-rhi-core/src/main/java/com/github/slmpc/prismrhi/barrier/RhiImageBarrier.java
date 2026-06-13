package com.github.slmpc.prismrhi.barrier;

import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageAspect;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record RhiImageBarrier(
        RhiImage image,
        RhiResourceState oldState,
        RhiResourceState newState,
        Set<RhiImageAspect> aspects,
        int baseMipLevel,
        int levelCount,
        int baseArrayLayer,
        int layerCount
) {
    public RhiImageBarrier {
        image = Objects.requireNonNull(image, "image");
        oldState = oldState == null ? RhiResourceState.UNDEFINED : oldState;
        newState = Objects.requireNonNull(newState, "newState");
        aspects = Set.copyOf(aspects == null || aspects.isEmpty() ? defaultAspects(image) : aspects);
        if (baseMipLevel < 0 || levelCount < 1) {
            throw new IllegalArgumentException("mip range must be valid");
        }
        if (baseArrayLayer < 0 || layerCount < 1) {
            throw new IllegalArgumentException("array layer range must be valid");
        }
    }

    public static RhiImageBarrier of(RhiImage image, RhiResourceState oldState, RhiResourceState newState) {
        return new RhiImageBarrier(image, oldState, newState, null, 0, 1, 0, 1);
    }

    public boolean transitionNeeded() {
        return oldState != newState;
    }

    public static Set<RhiImageAspect> defaultAspects(RhiImage image) {
        return switch (image.format()) {
            case D24_UNORM_S8_UINT -> EnumSet.of(RhiImageAspect.DEPTH, RhiImageAspect.STENCIL);
            case D32_FLOAT -> EnumSet.of(RhiImageAspect.DEPTH);
            case UNDEFINED -> throw new IllegalArgumentException("image format must not be UNDEFINED");
            default -> EnumSet.of(RhiImageAspect.COLOR);
        };
    }
}
