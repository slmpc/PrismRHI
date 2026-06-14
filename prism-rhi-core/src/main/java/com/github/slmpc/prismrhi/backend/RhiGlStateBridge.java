package com.github.slmpc.prismrhi.backend;

public interface RhiGlStateBridge {
    void enable(int cap);

    void disable(int cap);

    void viewport(int x, int y, int width, int height);

    void scissor(int x, int y, int width, int height);

    void polygonMode(int face, int mode);

    void frontFace(int mode);

    void lineWidth(float width);

    void cullFace(int mode);

    void depthFunc(int func);

    void depthMask(boolean mask);

    void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha);

    void blendEquationSeparate(int modeRgb, int modeAlpha);

    int genFramebuffer();

    void bindFramebuffer(int target, int framebuffer);

    void deleteFramebuffer(int framebuffer);

    void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

    void useProgram(int program);

    void activeTexture(int texture);

    void bindTexture(int target, int texture);

    void bindSampler(int unit, int sampler);

    void bindBuffer(int target, int buffer);

    void bindBufferRange(int target, int index, int buffer, long offset, long size);

    int genVertexArray();

    void bindVertexArray(int array);

    void deleteVertexArray(int array);

    void enableVertexAttribArray(int index);

    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer);

    void vertexAttribDivisor(int index, int divisor);
}
