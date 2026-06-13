package com.github.slmpc.prismrhi.rendering;

import com.github.slmpc.prismrhi.resource.RhiBuffer;

import java.util.Objects;

public record RhiVertexBufferBinding(int binding, RhiBuffer buffer, long offset) {
    public RhiVertexBufferBinding {
        if (binding < 0) {
            throw new IllegalArgumentException("binding must not be negative");
        }
        buffer = Objects.requireNonNull(buffer, "buffer");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }
}
