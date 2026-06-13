package com.github.slmpc.prismrhi.context;

import com.github.slmpc.prismrhi.backend.BackendApi;

public interface RhiContext extends AutoCloseable {
    BackendApi api();

    RhiContextMode mode();

    int width();

    int height();

    long nativeWindowHandle();

    long nativeSurfaceHandle();

    boolean shouldClose();

    void pollEvents();

    void requestClose();

    @Override
    void close();
}
