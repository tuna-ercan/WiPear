package com.example.wipear;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finds every PDF on the device via MediaStore (requires all-files access on API 30+). */
public final class PdfRepository {

    private PdfRepository() {}

    /** A folder that contains PDFs. */
    public static final class Folder {
        public final String path;   // unique key (relative path or dir name)
        public final String name;   // display name (last segment)
        public final int count;

        Folder(String path, String name, int count) {
            this.path = path;
            this.name = name;
            this.count = count;
        }
    }

    private static String[] projection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.RELATIVE_PATH
            };
        }
        return new String[]{
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATA
        };
    }

    /** Folder key for grouping/filtering, derived from RELATIVE_PATH or DATA. */
    private static String folderKey(Cursor c, int pathCol) {
        String raw = c.isNull(pathCol) ? null : c.getString(pathCol);
        if (raw == null || raw.isEmpty()) return "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // RELATIVE_PATH, e.g. "Documents/Sub/" -> "Documents/Sub"
            String p = raw;
            if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
            return p;
        } else {
            // DATA is a full file path; take its parent directory.
            int slash = raw.lastIndexOf('/');
            return slash > 0 ? raw.substring(0, slash) : raw;
        }
    }

    private static String folderName(String key) {
        if (key.isEmpty()) return "Storage";
        int slash = key.lastIndexOf('/');
        return slash >= 0 && slash < key.length() - 1 ? key.substring(slash + 1) : key;
    }

    public static List<PhotoItem> loadAllPdfs(Context context, long startMs, long endMs,
                                              Collection<String> folders) {
        List<PhotoItem> out = new ArrayList<>();

        Uri collection = MediaStore.Files.getContentUri("external");
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String[] selectionArgs = {"application/pdf"};
        String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                collection, projection(), selection, selectionArgs, sortOrder)) {
            if (cursor == null) return out;

            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int nameCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DISPLAY_NAME);
            int dateCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Files.FileColumns.DATE_MODIFIED);
            int pathCol = cursor.getColumnIndex(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            ? MediaStore.Files.FileColumns.RELATIVE_PATH
                            : MediaStore.Files.FileColumns.DATA);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String name = cursor.isNull(nameCol) ? null : cursor.getString(nameCol);
                long dateSec = cursor.isNull(dateCol) ? 0L : cursor.getLong(dateCol);
                long dateMs = dateSec * 1000L;

                if (dateMs < startMs || dateMs > endMs) continue;

                String key = pathCol >= 0 ? folderKey(cursor, pathCol) : "";
                if (folders != null && !folders.isEmpty() && !folders.contains(key)) {
                    continue;
                }

                Uri uri = ContentUris.withAppendedId(collection, id);
                out.add(new PhotoItem(id, uri,
                        name != null ? name : "document.pdf",
                        folderName(key), dateMs, true));
            }
        } catch (Exception ignored) {
            // Query can fail if access was revoked mid-scan; return what we have.
        }
        return out;
    }

    /** Lists folders that contain PDFs, with counts. */
    public static List<Folder> loadFolders(Context context) {
        Map<String, int[]> counts = new LinkedHashMap<>();

        Uri collection = MediaStore.Files.getContentUri("external");
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String[] selectionArgs = {"application/pdf"};
        String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";

        ContentResolver resolver = context.getContentResolver();
        try (Cursor cursor = resolver.query(
                collection, projection(), selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int pathCol = cursor.getColumnIndex(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                ? MediaStore.Files.FileColumns.RELATIVE_PATH
                                : MediaStore.Files.FileColumns.DATA);
                while (cursor.moveToNext()) {
                    String key = pathCol >= 0 ? folderKey(cursor, pathCol) : "";
                    int[] cell = counts.get(key);
                    if (cell == null) {
                        counts.put(key, new int[]{1});
                    } else {
                        cell[0]++;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        List<Folder> out = new ArrayList<>();
        for (Map.Entry<String, int[]> e : counts.entrySet()) {
            out.add(new Folder(e.getKey(), folderName(e.getKey()), e.getValue()[0]));
        }
        return out;
    }
}
