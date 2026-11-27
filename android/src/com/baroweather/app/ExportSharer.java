package com.baroweather.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

/** Creates cache export files and opens the platform share sheet for them. */
public final class ExportSharer {
    private ExportSharer() {
    }

    public static File createFile(Context context, String prefix, String extension) {
        File dir = new File(context.getCacheDir(), "export");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Unable to create export folder");
        }
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",
                java.util.Locale.US).format(new java.util.Date());
        return new File(dir, prefix + "-" + stamp + "." + extension);
    }

    public static Uri uriFor(File file) {
        return Uri.parse("content://" + ExportFileProvider.AUTHORITY + "/export/"
                + Uri.encode(file.getName()));
    }

    public static void share(Context context, File file, String mime, String subject) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, uriFor(file));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, "BaroWeather pressure history export");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent,
                context.getString(R.string.export_title)));
    }
}
