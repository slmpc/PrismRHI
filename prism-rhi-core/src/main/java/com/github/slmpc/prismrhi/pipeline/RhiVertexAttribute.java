package com.github.slmpc.prismrhi.pipeline;

import com.github.slmpc.prismrhi.format.RhiFormat;

import java.util.Objects;

public record RhiVertexAttribute(int location, int binding, RhiFormat format, int offset) {
    public RhiVertexAttribute {
        if (location < 0) {
            throw new IllegalArgumentException("location must not be negative");
        }
        if (binding < 0) {
            throw new IllegalArgumentException("binding must not be negative");
        }
        format = Objects.requireNonNull(format, "format");
        if (format == RhiFormat.UNDEFINED) {
            throw new IllegalArgumentException("vertex attribute format must not be UNDEFINED");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }
}
