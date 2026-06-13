package com.github.slmpc.prismrhi.swapchain;

import com.github.slmpc.prismrhi.context.RhiContext;
import com.github.slmpc.prismrhi.format.RhiFormat;
import com.github.slmpc.prismrhi.queue.RhiQueue;
import com.github.slmpc.prismrhi.resource.RhiResource;
import com.github.slmpc.prismrhi.sync.RhiSemaphore;

import java.util.List;

public interface RhiSwapchain extends RhiResource {
    RhiContext context();

    RhiFormat format();

    int width();

    int height();

    List<RhiSwapchainImage> images();

    default int imageCount() {
        return images().size();
    }

    default RhiSwapchainImage image(int imageIndex) {
        return images().get(imageIndex);
    }

    int acquireNextImage(RhiSemaphore imageAvailableSemaphore);

    void present(RhiQueue queue, int imageIndex, RhiSemaphore waitSemaphore);
}
