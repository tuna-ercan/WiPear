package com.example.wipear;

import android.net.Uri;

public class PhotoItem {
    public final long id;
    public final Uri uri;
    public final String name;
    public final String albumName;
    /** Date the photo was taken/added, in milliseconds since epoch. */
    public final long dateTakenMs;

    public PhotoItem(long id, Uri uri, String name, String albumName, long dateTakenMs) {
        this.id = id;
        this.uri = uri;
        this.name = name;
        this.albumName = albumName;
        this.dateTakenMs = dateTakenMs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhotoItem)) return false;
        return id == ((PhotoItem) o).id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
