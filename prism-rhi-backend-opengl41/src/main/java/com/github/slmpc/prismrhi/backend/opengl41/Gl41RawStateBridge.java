package com.github.slmpc.prismrhi.backend.opengl41;

import com.github.slmpc.prismrhi.backend.RhiGlStateBridge;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFrontFace;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glScissor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL20.glBlendEquationSeparate;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindBufferRange;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL33.glBindSampler;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

final class Gl41RawStateBridge implements RhiGlStateBridge {
    static final Gl41RawStateBridge INSTANCE = new Gl41RawStateBridge();

    private Gl41RawStateBridge() {
    }

    @Override
    public void enable(int cap) {
        glEnable(cap);
    }

    @Override
    public void disable(int cap) {
        glDisable(cap);
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        glViewport(x, y, width, height);
    }

    @Override
    public void scissor(int x, int y, int width, int height) {
        glScissor(x, y, width, height);
    }

    @Override
    public void polygonMode(int face, int mode) {
        glPolygonMode(face, mode);
    }

    @Override
    public void frontFace(int mode) {
        glFrontFace(mode);
    }

    @Override
    public void lineWidth(float width) {
        glLineWidth(width);
    }

    @Override
    public void cullFace(int mode) {
        glCullFace(mode);
    }

    @Override
    public void depthFunc(int func) {
        glDepthFunc(func);
    }

    @Override
    public void depthMask(boolean mask) {
        glDepthMask(mask);
    }

    @Override
    public void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    @Override
    public void blendEquationSeparate(int modeRgb, int modeAlpha) {
        glBlendEquationSeparate(modeRgb, modeAlpha);
    }

    @Override
    public int genFramebuffer() {
        return glGenFramebuffers();
    }

    @Override
    public void bindFramebuffer(int target, int framebuffer) {
        glBindFramebuffer(target, framebuffer);
    }

    @Override
    public void deleteFramebuffer(int framebuffer) {
        glDeleteFramebuffers(framebuffer);
    }

    @Override
    public void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public void useProgram(int program) {
        glUseProgram(program);
    }

    @Override
    public void activeTexture(int texture) {
        glActiveTexture(texture);
    }

    @Override
    public void bindTexture(int target, int texture) {
        glBindTexture(target, texture);
    }

    @Override
    public void bindSampler(int unit, int sampler) {
        glBindSampler(unit, sampler);
    }

    @Override
    public void bindBuffer(int target, int buffer) {
        glBindBuffer(target, buffer);
    }

    @Override
    public void bindBufferRange(int target, int index, int buffer, long offset, long size) {
        glBindBufferRange(target, index, buffer, offset, size);
    }

    @Override
    public void enableVertexAttribArray(int index) {
        glEnableVertexAttribArray(index);
    }

    @Override
    public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void vertexAttribDivisor(int index, int divisor) {
        glVertexAttribDivisor(index, divisor);
    }
}
