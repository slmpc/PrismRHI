package com.github.slmpc.prismrhi.command;

import com.github.slmpc.prismrhi.resource.RhiBuffer;

import java.util.Objects;

public record RhiDrawIndexedIndirectCommand(RhiBuffer buffer, long offset) {
    public RhiDrawIndexedIndirectCommand {
        buffer = Objects.requireNonNull(buffer, "buffer");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
    }
}
