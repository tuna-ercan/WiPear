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

/** Loads images or videos from MediaStore, optionally filtered by date and album. */
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

    public static List<PhotoItem> loadImages(Context context, long startMs, long endMs,
                                             java.util.Collection<String> bucketIds) {
        return loadMedia(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                startMs, endMs, bucketIds, false);
    }

    public static List<PhotoItem> loadVideos(Context context, long startMs, long endMs,
                                             java.util.Collection<String> bucketIds) {
        return loadMedia(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                startMs, endMs, bucketIds, true);
    }

    /**
     * @param startMs   inclusive lower bound (epoch millis), or Long.MIN_VALUE for none
     * @param endMs     inclusive upper bound (epoch millis), or Long.MAX_VALUE for none
     * @param bucketIds album bucket ids to include; null or empty = all albums
     */
    private static List<PhotoItem> loadMedia(Context context, Uri collection, long startMs,
                                             long endMs, java.util.Collection<String> bucketIds,
                                             boolean isVideo) {
        List<PhotoItem> result = new ArrayList<>();

        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_TAKEN,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        };
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";

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
            selection = MediaStore.MediaColumns.BUCKET_ID + " IN (" + placeholders + ")";
        }

        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                collection, projection, selection, selectionArgs, sortOrder)) {
            if (cursor == null) {
                return result;
            }
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            int takenCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN);
            int addedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
            int albumCol = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String name = cursor.getString(nameCol);

                long dateTaken = cursor.isNull(takenCol) ? 0L : cursor.getLong(takenCol);
                long dateAddedSec = cursor.isNull(addedCol) ? 0L : cursor.getLong(addedCol);
                long effectiveMs = dateTaken > 0 ? dateTaken : dateAddedSec * 1000L;

                if (effectiveMs < startMs || effectiveMs > endMs) {
                    continue;
                }

                String album = cursor.isNull(albumCol) ? "" : cursor.getString(albumCol);
                Uri uri = ContentUris.withAppendedId(collection, id);
                result.add(new PhotoItem(id, uri,
                        name != null ? name : (isVideo ? "video" : "image"),
                        album != null ? album : "", effectiveMs, false, isVideo));
            }
        }
        return result;
    }

    /** Lists the device's photo albums (buckets) with counts. */
    public static List<Album> loadAlbums(Context context) {
        return loadBuckets(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    /** Lists the device's video albums (buckets) with counts. */
    public static List<Album> loadVideoAlbums(Context context) {
        return loadBuckets(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
    }

    private static List<Album> loadBuckets(Context context, Uri collection) {
        Map<String, Album> byId = new LinkedHashMap<>();
        String[] projection = {
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        };
        String sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";

        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                collection, projection, null, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID);
                int nameCol = cursor.getColumnIndexOrThrow(
                        MediaStore.MediaColumns.BUCKET_DISPLAY_NAME);
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
