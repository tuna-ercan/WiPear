package com.example.wipear;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads images from the device's MediaStore, optionally filtered by date and album. */
public final class MediaRepository {

    private MediaRepository() {}

    /** An album (MediaStore "bucket"). */
    public static final class Album {
        public final String id;   // null = all albums
        public final String name;
        public final int count;

        Album(String id, String name, int count) {
            this.id = id;
            this.name = name;
            this.count = count;
        }
    }

    /**
     * @param startMs   inclusive lower bound (epoch millis), or Long.MIN_VALUE for none
     * @param endMs     inclusive upper bound (epoch millis), or Long.MAX_VALUE for none
     * @param bucketIds album bucket ids to include; null or empty = all albums
     */
    public static List<PhotoItem> loadImages(Context context, long startMs, long endMs,
                                             java.util.Collection<String> bucketIds) {
        List<PhotoItem> result = new ArrayList<>();

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        String selection = null;
        String[] selectionArgs = null;
        if (bucketIds != null && !bucketIds.isEmpty()) {
            StringBuilder placeholders = new StringBuilder();
            selectionArgs = new String[bucketIds.size()];
            int i = 0;
            for (String id : bucketIds) {
                if (i > 0) placeholders.append(',');
                placeholders.append('?');
                selectionArgs[i++] = id;
            }
            selection = MediaStore.Images.Media.BUCKET_ID
                    + " IN (" + placeholders + ")";
        }

        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                collection, projection, selection, selectionArgs, sortOrder)) {
            if (cursor == null) {
                return result;
            }
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
            int addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
            int albumCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String name = cursor.getString(nameCol);

                long dateTaken = cursor.isNull(takenCol) ? 0L : cursor.getLong(takenCol);
                long dateAddedSec = cursor.isNull(addedCol) ? 0L : cursor.getLong(addedCol);
                long effectiveMs = dateTaken > 0 ? dateTaken : dateAddedSec * 1000L;

                if (effectiveMs < startMs || effectiveMs > endMs) {
                    continue;
                }

                String album = cursor.isNull(albumCol)
                        ? "" : cursor.getString(albumCol);
                Uri uri = ContentUris.withAppendedId(collection, id);
                result.add(new PhotoItem(id, uri, name != null ? name : "image",
                        album != null ? album : "", effectiveMs));
            }
        }
        return result;
    }

    /** Lists the device's photo albums (buckets) with photo counts. */
    public static List<Album> loadAlbums(Context context) {
        Map<String, Album> byId = new LinkedHashMap<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                collection, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.BUCKET_ID);
                int nameCol = cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                while (cursor.moveToNext()) {
                    String bid = cursor.isNull(idCol) ? null : cursor.getString(idCol);
                    if (bid == null) continue;
                    String bname = cursor.isNull(nameCol)
                            ? "Unknown" : cursor.getString(nameCol);
                    Album existing = byId.get(bid);
                    int c = existing == null ? 1 : existing.count + 1;
                    byId.put(bid, new Album(bid, bname, c));
                }
            }
        }
        return new ArrayList<>(byId.values());
    }
}
