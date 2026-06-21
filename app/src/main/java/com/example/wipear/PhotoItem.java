package com.example.wipear;

import android.net.Uri;

/** A swipeable card: a photo, a video, or a PDF (see {@link #isPdf} / {@link #isVideo}). */
public class PhotoItem {
    public final long id;
    public final Uri uri;
    public final String name;
    /** Album name (photos/videos) or folder name (PDFs). */
    public final String albumName;
    /** Date the item was taken/added/modified, in milliseconds since epoch. */
    public final long dateTakenMs;
    public final boolean isPdf;
    public final boolean isVideo;

    public PhotoItem(long id, Uri uri, String name, String albumName, long dateTakenMs) {
        this(id, uri, name, albumName, dateTakenMs, false, false);
    }

    public PhotoItem(long id, Uri uri, String name, String albumName,
                     long dateTakenMs, boolean isPdf) {
        this(id, uri, name, albumName, dateTakenMs, isPdf, false);
    }

    public PhotoItem(long id, Uri uri, String name, String albumName,
                     long dateTakenMs, boolean isPdf, boolean isVideo) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.albumName = albumName;
        this.dateTakenMs = dateTakenMs;
        this.isPdf = isPdf;
        this.isVideo = isVideo;
    }

    /** Stable unique key (works across photo/video/PDF collections). */
    public String key() {
        return uri.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhotoItem)) return false;
        return uri.equals(((PhotoItem) o).uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }
}
