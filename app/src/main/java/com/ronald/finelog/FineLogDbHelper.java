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
    private static final int DB_VERSION = 2;

    public static class NoteRecord {
        public final String id;
        public final String title;
        public final String body;
        public final long updatedAt;
        public final int syncState;

        public NoteRecord(String id, String title, String body, long updatedAt, int syncState) {
            this.id = id;
            this.title = title;
            this.body = body;
            this.updatedAt = updatedAt;
            this.syncState = syncState;
        }

        @Override public String toString() {
            return body == null || body.isEmpty() ? title : title + "\n" + body;
        }
    }

    public static class TaskRecord {
        public final String id;
        public final String title;
        public final String dueDate;
        public final boolean done;
        public final long updatedAt;
        public final int syncState;

        public TaskRecord(String id, String title, String dueDate, boolean done, long updatedAt, int syncState) {
            this.id = id;
            this.title = title;
            this.dueDate = dueDate;
            this.done = done;
            this.updatedAt = updatedAt;
            this.syncState = syncState;
        }

        @Override public String toString() {
            String mark = done ? "✓ " : "○ ";
            return mark + title + (dueDate == null || dueDate.isEmpty() ? "" : "\nJatuh tempo: " + dueDate);
        }
    }

    public FineLogDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createNotesTable(db);
        createTasksTable(db);
    }

    private void createNotesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS notes (" +
                "id TEXT PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "body TEXT NOT NULL DEFAULT ''," +
                "updated_at INTEGER NOT NULL," +
                "sync_state INTEGER NOT NULL DEFAULT 0)");
    }

    private void createTasksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tasks (" +
                "id TEXT PRIMARY KEY," +
                "title TEXT NOT NULL," +
                "due_date TEXT NOT NULL DEFAULT ''," +
                "done INTEGER NOT NULL DEFAULT 0," +
                "updated_at INTEGER NOT NULL," +
                "sync_state INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createTasksTable(db);
    }

    public void addNote(String title, String body) {
        ContentValues values = new ContentValues();
        values.put("id", UUID.randomUUID().toString());
        values.put("title", title.trim());
        values.put("body", body == null ? "" : body.trim());
        values.put("updated_at", System.currentTimeMillis());
        values.put("sync_state", 0);
        getWritableDatabase().insertOrThrow("notes", null, values);
    }

    public List<NoteRecord> listNoteRecords() {
        List<NoteRecord> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("notes",
                new String[]{"id", "title", "body", "updated_at", "sync_state"},
                null, null, null, null, "updated_at DESC")) {
            while (cursor.moveToNext()) {
                result.add(new NoteRecord(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getLong(3), cursor.getInt(4)));
            }
        }
        return result;
    }

    public List<NoteRecord> pendingNotes() {
        List<NoteRecord> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("notes",
                new String[]{"id", "title", "body", "updated_at", "sync_state"},
                "sync_state=0", null, null, null, "updated_at ASC")) {
            while (cursor.moveToNext()) {
                result.add(new NoteRecord(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getLong(3), cursor.getInt(4)));
            }
        }
        return result;
    }

    public void markNoteSynced(String id) {
        ContentValues values = new ContentValues();
        values.put("sync_state", 1);
        getWritableDatabase().update("notes", values, "id=?", new String[]{id});
    }

    public void upsertNoteFromCloud(String id, String title, String body, long updatedAt) {
        long localUpdated = getUpdatedAt("notes", id);
        if (localUpdated > updatedAt) return;
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("title", title == null ? "" : title);
        values.put("body", body == null ? "" : body);
        values.put("updated_at", updatedAt);
        values.put("sync_state", 1);
        getWritableDatabase().insertWithOnConflict("notes", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addTask(String title, String dueDate) {
        ContentValues values = new ContentValues();
        values.put("id", UUID.randomUUID().toString());
        values.put("title", title.trim());
        values.put("due_date", dueDate == null ? "" : dueDate.trim());
        values.put("done", 0);
        values.put("updated_at", System.currentTimeMillis());
        values.put("sync_state", 0);
        getWritableDatabase().insertOrThrow("tasks", null, values);
    }

    public List<TaskRecord> listTaskRecords() {
        return listTaskRecordsForDate(null);
    }

    public List<TaskRecord> listTaskRecordsForDate(String dueDate) {
        List<TaskRecord> result = new ArrayList<>();
        String selection = dueDate == null ? null : "due_date=?";
        String[] args = dueDate == null ? null : new String[]{dueDate};
        try (Cursor cursor = getReadableDatabase().query("tasks",
                new String[]{"id", "title", "due_date", "done", "updated_at", "sync_state"},
                selection, args, null, null, "done ASC, updated_at DESC")) {
            while (cursor.moveToNext()) {
                result.add(new TaskRecord(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getInt(3) == 1, cursor.getLong(4), cursor.getInt(5)));
            }
        }
        return result;
    }

    public void toggleTask(String id, boolean done) {
        ContentValues values = new ContentValues();
        values.put("done", done ? 1 : 0);
        values.put("updated_at", System.currentTimeMillis());
        values.put("sync_state", 0);
        getWritableDatabase().update("tasks", values, "id=?", new String[]{id});
    }

    public List<TaskRecord> pendingTasks() {
        List<TaskRecord> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("tasks",
                new String[]{"id", "title", "due_date", "done", "updated_at", "sync_state"},
                "sync_state=0", null, null, null, "updated_at ASC")) {
            while (cursor.moveToNext()) {
                result.add(new TaskRecord(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                        cursor.getInt(3) == 1, cursor.getLong(4), cursor.getInt(5)));
            }
        }
        return result;
    }

    public void markTaskSynced(String id) {
        ContentValues values = new ContentValues();
        values.put("sync_state", 1);
        getWritableDatabase().update("tasks", values, "id=?", new String[]{id});
    }

    public void upsertTaskFromCloud(String id, String title, String dueDate, boolean done, long updatedAt) {
        long localUpdated = getUpdatedAt("tasks", id);
        if (localUpdated > updatedAt) return;
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("title", title == null ? "" : title);
        values.put("due_date", dueDate == null ? "" : dueDate);
        values.put("done", done ? 1 : 0);
        values.put("updated_at", updatedAt);
        values.put("sync_state", 1);
        getWritableDatabase().insertWithOnConflict("tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private long getUpdatedAt(String table, String id) {
        try (Cursor cursor = getReadableDatabase().query(table, new String[]{"updated_at"},
                "id=?", new String[]{id}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getLong(0) : -1L;
        }
    }

    public int countPendingSync() {
        int notes = count("notes", "sync_state=0");
        int tasks = count("tasks", "sync_state=0");
        return notes + tasks;
    }

    private int count(String table, String where) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table + " WHERE " + where, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }
}
