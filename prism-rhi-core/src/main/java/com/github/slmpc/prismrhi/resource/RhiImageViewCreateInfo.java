package com.github.slmpc.prismrhi.resource;

import com.github.slmpc.prismrhi.format.RhiFormat;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record RhiImageViewCreateInfo(
        RhiImage image,
        RhiFormat format,
        Set<RhiImageAspect> aspects,
        int baseMipLevel,
        int levelCount,
        int baseArrayLayer,
        int layerCount
) {
    public RhiImageViewCreateInfo {
        image = Objects.requireNonNull(image, "image");
        format = format == null ? image.format() : format;
        aspects = Set.copyOf(aspects == null || aspects.isEmpty() ? defaultAspects(format) : aspects);
        if (baseMipLevel < 0 || levelCount < 1) {
            throw new IllegalArgumentException("mip range must be valid");
        }
        if (baseArrayLayer < 0 || layerCount < 1) {
            throw new IllegalArgumentException("array layer range must be valid");
        }
    }

    public static RhiImageViewCreateInfo of(RhiImage image) {
        return builder(image).build();
    }

    public static Builder builder(RhiImage image) {
        return new Builder(image);
    }

    private static Set<RhiImageAspect> defaultAspects(RhiFormat format) {
        return switch (format) {
            case D24_UNORM_S8_UINT -> EnumSet.of(RhiImageAspect.DEPTH, RhiImageAspect.STENCIL);
            case D32_FLOAT -> EnumSet.of(RhiImageAspect.DEPTH);
            case UNDEFINED -> throw new IllegalArgumentException("image view format must not be UNDEFINED");
            default -> EnumSet.of(RhiImageAspect.COLOR);
        };
    }

    public static final class Builder {
        private final RhiImage image;
        private RhiFormat format;
        private final Set<RhiImageAspect> aspects = EnumSet.noneOf(RhiImageAspect.class);
        private int baseMipLevel;
        private int levelCount = 1;
        private int baseArrayLayer;
        private int layerCount = 1;

        private Builder(RhiImage image) {
            this.image = image;
        }

        public Builder format(RhiFormat format) {
            this.format = format;
            return this;
        }

        public Builder aspect(RhiImageAspect aspect) {
            aspects.add(aspect);
            return this;
        }

        public Builder mipRange(int baseMipLevel, int levelCount) {
            this.baseMipLevel = baseMipLevel;
            this.levelCount = levelCount;
            return this;
        }

        public Builder arrayRange(int baseArrayLayer, int layerCount) {
            this.baseArrayLayer = baseArrayLayer;
            this.layerCount = layerCount;
            return this;
        }

        public RhiImageViewCreateInfo build() {
            return new RhiImageViewCreateInfo(
                    image,
                    format,
                    aspects,
                    baseMipLevel,
                    levelCount,
                    baseArrayLayer,
                    layerCount
            );
        }
    }
}
