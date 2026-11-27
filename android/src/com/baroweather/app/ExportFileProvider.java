package com.baroweather.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/** Minimal platform-only content provider for sharing files from the app cache. */
public final class ExportFileProvider extends ContentProvider {
    public static final String AUTHORITY = "com.baroweather.app.fileprovider";
    private static final int EXPORTED_FILE = 1;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(AUTHORITY, "export/*", EXPORTED_FILE);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (MATCHER.match(uri) != EXPORTED_FILE || !"r".equals(mode)) {
            throw new FileNotFoundException("Unsupported export URI: " + uri);
        }
        String name = uri.getLastPathSegment();
        if (name == null || name.length() == 0 || name.contains("/") || name.contains("..")) {
            throw new FileNotFoundException("Invalid export name");
        }
        try {
            File dir = new File(getContext().getCacheDir(), "export");
            File file = new File(dir, name);
            if (!file.getCanonicalFile().getParentFile()
                    .equals(dir.getCanonicalFile())) {
                throw new FileNotFoundException("Export outside app cache");
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (Exception error) {
            throw new FileNotFoundException(error.getMessage());
        }
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null && name.endsWith(".png")) {
            return "image/png";
        }
        return "text/csv";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Exports are read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Exports are read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("Exports are read-only");
    }
}
