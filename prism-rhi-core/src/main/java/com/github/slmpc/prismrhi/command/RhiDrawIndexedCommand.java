package com.github.slmpc.prismrhi.command;

public record RhiDrawIndexedCommand(
        int indexCount,
        int instanceCount,
        int firstIndex,
        int vertexOffset,
        int firstInstance
) {
    public RhiDrawIndexedCommand {
        if (indexCount < 0) {
            throw new IllegalArgumentException("indexCount must not be negative");
        }
        if (instanceCount < 0) {
            throw new IllegalArgumentException("instanceCount must not be negative");
        }
        if (firstIndex < 0) {
            throw new IllegalArgumentException("firstIndex must not be negative");
        }
        if (firstInstance < 0) {
            throw new IllegalArgumentException("firstInstance must not be negative");
        }
    }
}
