package com.example.wipear;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

/** Renders the first page of a PDF to a bitmap. */
public final class PdfThumb {

    private PdfThumb() {}

    public static Bitmap renderFirstPage(Context ctx, Uri uri) {
        return renderFirstPage(ctx, uri, 1200);
    }

    public static Bitmap renderFirstPage(Context ctx, Uri uri, int targetWidth) {
        ParcelFileDescriptor pfd = null;
        PdfRenderer renderer = null;
        PdfRenderer.Page page = null;
        try {
            pfd = ctx.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return null;
            renderer = new PdfRenderer(pfd);
            if (renderer.getPageCount() == 0) return null;
            page = renderer.openPage(0);
            int targetHeight = Math.max(1,
                    (int) ((long) targetWidth * page.getHeight() / page.getWidth()));
            Bitmap bmp = Bitmap.createBitmap(
                    targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.WHITE);
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bmp;
        } catch (Exception e) {
            return null;
        } finally {
            try { if (page != null) page.close(); } catch (Exception ignored) {}
            try { if (renderer != null) renderer.close(); } catch (Exception ignored) {}
            try { if (pfd != null) pfd.close(); } catch (Exception ignored) {}
        }
    }
}
