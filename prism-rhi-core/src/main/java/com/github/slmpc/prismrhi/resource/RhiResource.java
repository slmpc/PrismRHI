package com.github.slmpc.prismrhi.resource;

import com.github.slmpc.prismrhi.backend.BackendApi;

public interface RhiResource extends AutoCloseable {
    BackendApi api();

    long nativeHandle();

    @Override
    void close();
}
