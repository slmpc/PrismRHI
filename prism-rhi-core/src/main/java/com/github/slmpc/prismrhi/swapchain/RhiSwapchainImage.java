package com.github.slmpc.prismrhi.swapchain;

import com.github.slmpc.prismrhi.resource.RhiImage;
import com.github.slmpc.prismrhi.resource.RhiImageView;

public record RhiSwapchainImage(int index, RhiImage image, RhiImageView view) {
}
