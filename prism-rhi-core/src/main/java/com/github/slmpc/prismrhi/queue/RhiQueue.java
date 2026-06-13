package com.github.slmpc.prismrhi.queue;

public interface RhiQueue {
    RhiQueueType type();

    void submit(RhiSubmitInfo submitInfo);

    void waitIdle();
}
