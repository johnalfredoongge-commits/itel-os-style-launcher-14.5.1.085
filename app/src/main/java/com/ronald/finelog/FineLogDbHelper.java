package com.ronald.finelog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FineLogDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "finelog.db";
    private static final int DB_VERSION = 1;

    public FineLogDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (" +
                "id TEXT PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "body TEXT NOT NULL DEFAULT ''," +
                "updated_at INTEGER NOT NULL," +
                "sync_state INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future schema migrations go here.
    }

    public void addNote(String title, String body) {
        ContentValues values = new ContentValues();
        values.put("id", UUID.randomUUID().toString());
        values.put("title", title.trim());
        values.put("body", body.trim());
        values.put("updated_at", System.currentTimeMillis());
        values.put("sync_state", 0);
        getWritableDatabase().insertOrThrow("notes", null, values);
    }

    public List<String> listNotes() {
        List<String> notes = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "notes",
                new String[]{"title", "body"},
                null, null, null, null,
                "updated_at DESC")) {
            while (cursor.moveToNext()) {
                String title = cursor.getString(0);
                String body = cursor.getString(1);
                notes.add(body == null || body.isEmpty() ? title : title + "\n" + body);
            }
        }
        return notes;
    }

    public int countPendingSync() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM notes WHERE sync_state = 0", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }
}
