package com.github.slmpc.prismrhi.queue;

public interface RhiQueue {
    RhiQueueType type();

    default long nativeHandle() {
        return 0L;
    }

    void submit(RhiSubmitInfo submitInfo);

    void waitIdle();
}
