package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.backend.BackendApi;
import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.command.RhiCommandBufferLevel;
import com.github.slmpc.prismrhi.command.RhiCommandPool;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.queue.RhiQueueType;

import java.util.concurrent.atomic.AtomicLong;

final class GlDsaCommandPool implements RhiCommandPool {
    private static final AtomicLong NEXT_HANDLE = new AtomicLong(1);

    private final long handle = NEXT_HANDLE.getAndIncrement();
    private final RhiQueueType queueType;
    private boolean closed;

    GlDsaCommandPool(RhiQueueType queueType) {
        this.queueType = queueType;
    }

    @Override
    public BackendApi api() {
        return BackendApi.OPENGL_DSA;
    }

    @Override
    public long nativeHandle() {
        return handle;
    }

    @Override
    public RhiCommandBuffer allocateCommandBuffer(RhiCommandBufferLevel level) {
        if (closed) {
            throw new RhiException("OpenGL DSA command pool is closed");
        }
        return new GlDsaCommandBuffer(level, queueType);
    }

    @Override
    public void close() {
        closed = true;
    }
}
