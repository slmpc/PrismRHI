package com.github.slmpc.prismrhi.command;

public record RhiDrawCommand(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
    public RhiDrawCommand {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("vertexCount must not be negative");
        }
        if (instanceCount < 0) {
            throw new IllegalArgumentException("instanceCount must not be negative");
        }
        if (firstVertex < 0) {
            throw new IllegalArgumentException("firstVertex must not be negative");
        }
        if (firstInstance < 0) {
            throw new IllegalArgumentException("firstInstance must not be negative");
        }
    }
}
