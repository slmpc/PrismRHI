package com.github.slmpc.prismrhi.backend.opengldsa;

import com.github.slmpc.prismrhi.command.RhiCommandBuffer;
import com.github.slmpc.prismrhi.RhiException;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.queue.RhiQueueType;
import com.github.slmpc.prismrhi.queue.RhiSubmitInfo;

import static org.lwjgl.opengl.GL11.glFinish;

final class GlDsaQueue implements RhiQueue {
    private final RhiQueueType type;

    GlDsaQueue(RhiQueueType type) {
        this.type = type;
    }

    @Override
    public RhiQueueType type() {
        return type;
    }

    @Override
    public void submit(RhiSubmitInfo submitInfo) {
        for (RhiCommandBuffer commandBuffer : submitInfo.commandBuffers()) {
            if (!(commandBuffer instanceof GlDsaCommandBuffer glCommandBuffer)) {
                throw new RhiException("Cannot submit command buffer from another backend to OpenGL DSA queue");
            }
            glCommandBuffer.submit();
        }
    }

    @Override
    public void waitIdle() {
        glFinish();
    }
}
