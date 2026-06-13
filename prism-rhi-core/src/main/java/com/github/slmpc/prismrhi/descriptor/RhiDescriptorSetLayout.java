package com.github.slmpc.prismrhi.descriptor;

import com.github.slmpc.prismrhi.resource.RhiResource;

import java.util.List;

public interface RhiDescriptorSetLayout extends RhiResource {
    List<RhiDescriptorBinding> bindings();
}
