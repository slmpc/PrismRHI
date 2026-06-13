package com.github.slmpc.prismrhi.resource;

import com.github.slmpc.prismrhi.format.RhiExtent3D;
import com.github.slmpc.prismrhi.format.RhiFormat;

public interface RhiImage extends RhiResource {
    RhiExtent3D extent();

    RhiFormat format();
}
