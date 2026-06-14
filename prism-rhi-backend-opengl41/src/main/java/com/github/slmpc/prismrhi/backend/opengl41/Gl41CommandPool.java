package com.github.slmpc.prismrhi.backend.opengl41;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiCommandPool;
import com.github.slmpc.prismrhi.backend.RhiGlStateBridge;
import com.github.slmpc.prismrhi.queue.RhiQueueType;

import java.util.concurrent.atomic.AtomicLong;

final class Gl41CommandPool implements RhiCommandPool {
    private static final AtomicLong NEXT_HANDLE = new AtomicLong(1);

    private final long handle = NEXT_HANDLE.getAndIncrement();
    private final RhiQueueType queueType;
    private final RhiGlStateBridge glStateBridge;
    private boolean closed;

    Gl41CommandPool(RhiQueueType queueType, RhiGlStateBridge glStateBridge) {
        this.queueType = queueType;
        this.glStateBridge = glStateBridge;
    }

    @Override
    public BackendApi api() {
        return BackendApi.OPENGL_41;
    }

    @Override
    public long nativeHandle() {
        return handle;
    }

    @Override
    public RhiCommandBuffer allocateCommandBuffer(RhiCommandBufferLevel level) {
        if (closed) {
            throw new IllegalStateException("OpenGL 4.1 command pool is closed");
        }
        return new Gl41CommandBuffer(level, queueType, glStateBridge);
    }

    @Override
    public void close() {
        closed = true;
    }
}
