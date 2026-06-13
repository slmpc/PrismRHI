package com.github.slmpc.prismrhi.command;

import com.github.slmpc.prismrhi.resource.RhiBuffer;

import java.util.Objects;

public record RhiMultiDrawIndirectCommand(RhiBuffer buffer, long offset, int drawCount, int stride) {
    public static final int PACKED_STRIDE = 0;

    public RhiMultiDrawIndirectCommand {
        buffer = Objects.requireNonNull(buffer, "buffer");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (drawCount < 0) {
            throw new IllegalArgumentException("drawCount must not be negative");
        }
        if (stride < 0) {
            throw new IllegalArgumentException("stride must not be negative");
        }
    }
}
