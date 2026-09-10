package com.ronald.finelog;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private FineLogDbHelper db;
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new FineLogDbHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("FineLog");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, dp(4));
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Catatan, tugas, dan aktivitas harian Anda");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, dp(16));
        root.addView(subtitle);

        EditText noteTitle = new EditText(this);
        noteTitle.setHint("Judul");
        root.addView(noteTitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText noteBody = new EditText(this);
        noteBody.setHint("Tulis catatan...");
        noteBody.setMinLines(3);
        noteBody.setGravity(Gravity.TOP);
        noteBody.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        root.addView(noteBody, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button save = new Button(this);
        save.setText("Simpan");
        actions.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Button sync = new Button(this);
        sync.setText("Sinkronkan");
        actions.addView(sync, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        root.addView(actions);

        TextView localInfo = new TextView(this);
        localInfo.setText("Mode offline aktif. Data tersimpan di perangkat dan siap disinkronkan ke cloud.");
        localInfo.setPadding(0, dp(12), 0, dp(8));
        root.addView(localInfo);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
        reload();

        save.setOnClickListener(v -> {
            String t = noteTitle.getText().toString().trim();
            if (t.isEmpty()) {
                noteTitle.setError("Judul wajib diisi");
                return;
            }
            db.addNote(t, noteBody.getText().toString());
            noteTitle.setText("");
            noteBody.setText("");
            reload();
            Toast.makeText(this, "Catatan disimpan", Toast.LENGTH_SHORT).show();
        });

        sync.setOnClickListener(v -> {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).build();
            WorkManager.getInstance(this).enqueue(request);
            Toast.makeText(this, "Sinkronisasi dijadwalkan", Toast.LENGTH_SHORT).show();
        });
    }

    private void reload() {
        items.clear();
        items.addAll(db.listNotes());
        adapter.notifyDataSetChanged();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
