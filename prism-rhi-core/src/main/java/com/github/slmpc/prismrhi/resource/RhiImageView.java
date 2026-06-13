package com.github.slmpc.prismrhi.resource;

import com.github.slmpc.prismrhi.format.RhiFormat;

import java.util.Set;

public interface RhiImageView extends RhiResource {
    RhiImage image();

    RhiFormat format();

    Set<RhiImageAspect> aspects();
}
