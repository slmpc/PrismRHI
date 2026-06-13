package com.github.slmpc.prismrhi.backend;

public enum BackendApi {
    OPENGL_41("opengl41"),
    OPENGL_DSA("opengl-dsa"),
    VULKAN("vulkan");

    private final String id;

    BackendApi(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
