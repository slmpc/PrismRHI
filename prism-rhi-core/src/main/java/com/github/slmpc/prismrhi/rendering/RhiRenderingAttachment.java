package com.github.slmpc.prismrhi.rendering;

import com.github.slmpc.prismrhi.resource.RhiImageView;

import java.util.Objects;

public record RhiRenderingAttachment(
        RhiImageView view,
        RhiImageLayout layout,
        RhiAttachmentLoadOp loadOp,
        RhiAttachmentStoreOp storeOp,
        RhiClearValue clearValue
) {
    public RhiRenderingAttachment {
        view = Objects.requireNonNull(view, "view");
        layout = layout == null ? RhiImageLayout.COLOR_ATTACHMENT : layout;
        loadOp = loadOp == null ? RhiAttachmentLoadOp.LOAD : loadOp;
        storeOp = storeOp == null ? RhiAttachmentStoreOp.STORE : storeOp;
        clearValue = clearValue == null ? RhiClearValue.color(0.0f, 0.0f, 0.0f, 1.0f) : clearValue;
    }

    public static RhiRenderingAttachment color(RhiImageView view) {
        return new RhiRenderingAttachment(
                view,
                RhiImageLayout.COLOR_ATTACHMENT,
                RhiAttachmentLoadOp.LOAD,
                RhiAttachmentStoreOp.STORE,
                RhiClearValue.color(0.0f, 0.0f, 0.0f, 1.0f)
        );
    }

    public static RhiRenderingAttachment clearColor(RhiImageView view, float r, float g, float b, float a) {
        return new RhiRenderingAttachment(
                view,
                RhiImageLayout.COLOR_ATTACHMENT,
                RhiAttachmentLoadOp.CLEAR,
                RhiAttachmentStoreOp.STORE,
                RhiClearValue.color(r, g, b, a)
        );
    }

    public static RhiRenderingAttachment depth(RhiImageView view, RhiAttachmentLoadOp loadOp, float clearDepth) {
        return new RhiRenderingAttachment(
                view,
                RhiImageLayout.DEPTH_STENCIL_ATTACHMENT,
                loadOp,
                RhiAttachmentStoreOp.STORE,
                RhiClearValue.depth(clearDepth)
        );
    }
}
