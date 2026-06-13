package com.github.slmpc.prismrhi.descriptor;

import com.github.slmpc.prismrhi.resource.RhiBuffer;
import com.github.slmpc.prismrhi.resource.RhiImageView;
import com.github.slmpc.prismrhi.resource.RhiSampler;

import java.util.Objects;

public record RhiDescriptorWrite(
        int binding,
        int arrayElement,
        RhiDescriptorType type,
        RhiBuffer buffer,
        long offset,
        long range,
        RhiImageView imageView,
        RhiSampler sampler
) {
    public RhiDescriptorWrite {
        if (binding < 0) {
            throw new IllegalArgumentException("binding must not be negative");
        }
        if (arrayElement < 0) {
            throw new IllegalArgumentException("arrayElement must not be negative");
        }
        type = Objects.requireNonNull(type, "type");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (range < 0) {
            throw new IllegalArgumentException("range must not be negative");
        }
    }

    public static RhiDescriptorWrite buffer(
            int binding,
            int arrayElement,
            RhiDescriptorType type,
            RhiBuffer buffer,
            long offset,
            long range
    ) {
        return new RhiDescriptorWrite(binding, arrayElement, type, Objects.requireNonNull(buffer, "buffer"), offset, range, null, null);
    }

    public static RhiDescriptorWrite image(
            int binding,
            int arrayElement,
            RhiDescriptorType type,
            RhiImageView imageView,
            RhiSampler sampler
    ) {
        return new RhiDescriptorWrite(
                binding,
                arrayElement,
                type,
                null,
                0,
                0,
                Objects.requireNonNull(imageView, "imageView"),
                sampler
        );
    }
}
