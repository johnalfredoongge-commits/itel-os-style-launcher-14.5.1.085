package com.ronald.finelog;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private FineLogDbHelper db;
    private FrameLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new FineLogDbHelper(this);
        schedulePeriodicSync();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(8));

        TextView title = new TextView(this);
        title.setText("FineLog");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, dp(4), 0, 0);
        root.addView(title, fullWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("Catatan • Tugas • Kalender • Sinkronisasi");
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, dp(8));
        root.addView(subtitle, fullWrap());

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);

        Button notes = navButton("Catatan");
        Button tasks = navButton("Tugas");
        Button calendar = navButton("Kalender");
        Button profile = navButton("Profil");
        nav.addView(notes);
        nav.addView(tasks);
        nav.addView(calendar);
        nav.addView(profile);
        root.addView(nav, fullWrap());

        setContentView(root);

        notes.setOnClickListener(v -> showNotes());
        tasks.setOnClickListener(v -> showTasks());
        calendar.setOnClickListener(v -> showCalendar());
        profile.setOnClickListener(v -> showProfile());

        showNotes();
    }

    private void showNotes() {
        LinearLayout page = page();

        TextView heading = heading("Catatan Harian");
        page.addView(heading);

        EditText noteTitle = new EditText(this);
        noteTitle.setHint("Judul catatan");
        page.addView(noteTitle, fullWrap());

        EditText noteBody = new EditText(this);
        noteBody.setHint("Tulis catatan...");
        noteBody.setMinLines(3);
        noteBody.setGravity(Gravity.TOP);
        noteBody.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        page.addView(noteBody, fullWrap());

        LinearLayout actions = row();
        Button save = navButton("Simpan");
        Button sync = navButton("Sinkronkan");
        actions.addView(save);
        actions.addView(sync);
        page.addView(actions, fullWrap());

        TextView info = new TextView(this);
        info.setText(cloudHint());
        info.setPadding(0, dp(8), 0, dp(6));
        page.addView(info, fullWrap());

        ArrayList<FineLogDbHelper.NoteRecord> notes = new ArrayList<>();
        ArrayAdapter<FineLogDbHelper.NoteRecord> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, notes);
        ListView list = new ListView(this);
        list.setAdapter(adapter);
        page.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        Runnable reload = () -> {
            notes.clear();
            notes.addAll(db.listNoteRecords());
            adapter.notifyDataSetChanged();
            info.setText(cloudHint());
        };
        reload.run();

        save.setOnClickListener(v -> {
            String t = noteTitle.getText().toString().trim();
            if (t.isEmpty()) {
                noteTitle.setError("Judul wajib diisi");
                return;
            }
            db.addNote(t, noteBody.getText().toString());
            noteTitle.setText("");
            noteBody.setText("");
            reload.run();
            enqueueSync();
            toast("Catatan disimpan");
        });

        sync.setOnClickListener(v -> {
            enqueueSync();
            toast("Sinkronisasi dijadwalkan");
        });

        setPage(page);
    }

    private void showTasks() {
        LinearLayout page = page();
        page.addView(heading("Tugas & Checklist"));

        EditText taskTitle = new EditText(this);
        taskTitle.setHint("Nama tugas");
        page.addView(taskTitle, fullWrap());

        EditText dueDate = new EditText(this);
        dueDate.setHint("Tanggal jatuh tempo (YYYY-MM-DD)");
        dueDate.setFocusable(false);
        dueDate.setText(today());
        dueDate.setOnClickListener(v -> pickDate(dueDate));
        page.addView(dueDate, fullWrap());

        Button add = new Button(this);
        add.setText("Tambah Tugas");
        page.addView(add, fullWrap());

        TextView help = new TextView(this);
        help.setText("Ketuk tugas untuk menandai selesai/belum selesai.");
        help.setPadding(0, dp(6), 0, dp(6));
        page.addView(help, fullWrap());

        ArrayList<FineLogDbHelper.TaskRecord> tasks = new ArrayList<>();
        ArrayAdapter<FineLogDbHelper.TaskRecord> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasks);
        ListView list = new ListView(this);
        list.setAdapter(adapter);
        page.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        Runnable reload = () -> {
            tasks.clear();
            tasks.addAll(db.listTaskRecords());
            adapter.notifyDataSetChanged();
        };
        reload.run();

        add.setOnClickListener(v -> {
            String t = taskTitle.getText().toString().trim();
            if (t.isEmpty()) {
                taskTitle.setError("Nama tugas wajib diisi");
                return;
            }
            db.addTask(t, dueDate.getText().toString());
            taskTitle.setText("");
            reload.run();
            enqueueSync();
            toast("Tugas ditambahkan");
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            FineLogDbHelper.TaskRecord task = tasks.get(position);
            db.toggleTask(task.id, !task.done);
            reload.run();
            enqueueSync();
        });

        setPage(page);
    }

    private void showCalendar() {
        LinearLayout page = page();
        page.addView(heading("Kalender Kegiatan"));

        TextView selected = new TextView(this);
        selected.setTextSize(16);
        selected.setPadding(0, dp(6), 0, dp(6));

        CalendarView calendar = new CalendarView(this);
        page.addView(calendar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)));
        page.addView(selected, fullWrap());

        ArrayList<FineLogDbHelper.TaskRecord> dayTasks = new ArrayList<>();
        ArrayAdapter<FineLogDbHelper.TaskRecord> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dayTasks);
        ListView list = new ListView(this);
        list.setAdapter(adapter);
        page.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        java.util.function.Consumer<String> reload = date -> {
            selected.setText("Tugas tanggal " + date);
            dayTasks.clear();
            dayTasks.addAll(db.listTaskRecordsForDate(date));
            if (dayTasks.isEmpty()) {
                dayTasks.add(new FineLogDbHelper.TaskRecord("", "Tidak ada tugas", "", false, 0, 1));
            }
            adapter.notifyDataSetChanged();
        };

        reload.accept(today());
        calendar.setOnDateChangeListener((view, year, month, dayOfMonth) ->
                reload.accept(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)));

        setPage(page);
    }

    private void showProfile() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = page();
        page.addView(heading("Profil & Sinkronisasi Cloud"));

        TextView explanation = new TextView(this);
        explanation.setText("FineLog dapat memakai Firebase untuk login dan sinkronisasi data antar perangkat Android. Isi konfigurasi Firebase proyek Anda satu kali di perangkat ini.");
        explanation.setPadding(0, 0, 0, dp(8));
        page.addView(explanation, fullWrap());

        EditText projectId = field("Firebase Project ID", CloudManager.getProjectId(this));
        EditText appId = field("Firebase App ID", CloudManager.getAppId(this));
        EditText apiKey = field("Firebase Web API Key", CloudManager.getApiKey(this));
        apiKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        page.addView(projectId, fullWrap());
        page.addView(appId, fullWrap());
        page.addView(apiKey, fullWrap());

        Button saveConfig = new Button(this);
        saveConfig.setText("Simpan Konfigurasi Cloud");
        page.addView(saveConfig, fullWrap());

        TextView cloudStatus = new TextView(this);
        cloudStatus.setPadding(0, dp(8), 0, dp(10));
        page.addView(cloudStatus, fullWrap());

        EditText email = field("Email", "");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = field("Password", "");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        page.addView(email, fullWrap());
        page.addView(password, fullWrap());

        LinearLayout authRow = row();
        Button register = navButton("Daftar");
        Button login = navButton("Masuk");
        authRow.addView(register);
        authRow.addView(login);
        page.addView(authRow, fullWrap());

        LinearLayout cloudActions = row();
        Button logout = navButton("Keluar");
        Button sync = navButton("Sinkronkan");
        cloudActions.addView(logout);
        cloudActions.addView(sync);
        page.addView(cloudActions, fullWrap());

        TextView about = new TextView(this);
        about.setText("FineLog 1.14.0-beta\nMode offline-first • Sinkronisasi multi-perangkat • Android 7.0+");
        about.setPadding(0, dp(16), 0, dp(20));
        page.addView(about, fullWrap());

        Runnable refreshStatus = () -> updateCloudStatus(cloudStatus);
        refreshStatus.run();

        saveConfig.setOnClickListener(v -> {
            if (projectId.getText().toString().trim().isEmpty() || appId.getText().toString().trim().isEmpty() || apiKey.getText().toString().trim().isEmpty()) {
                toast("Project ID, App ID, dan API Key wajib diisi");
                return;
            }
            CloudManager.saveConfig(this, projectId.getText().toString(), appId.getText().toString(), apiKey.getText().toString());
            refreshStatus.run();
            toast("Konfigurasi cloud disimpan");
        });

        register.setOnClickListener(v -> authenticate(email, password, true, cloudStatus));
        login.setOnClickListener(v -> authenticate(email, password, false, cloudStatus));
        logout.setOnClickListener(v -> {
            try {
                FirebaseAuth auth = CloudManager.auth(this);
                if (auth != null) auth.signOut();
                updateCloudStatus(cloudStatus);
                toast("Keluar dari akun");
            } catch (Exception e) {
                toast("Gagal keluar: " + shortError(e));
            }
        });
        sync.setOnClickListener(v -> {
            enqueueSync();
            toast("Sinkronisasi dijadwalkan");
        });

        scroll.addView(page);
        setPage(scroll);
    }

    private void authenticate(EditText email, EditText password, boolean create, TextView status) {
        String e = email.getText().toString().trim();
        String p = password.getText().toString();
        if (e.isEmpty() || p.length() < 6) {
            toast("Isi email dan password minimal 6 karakter");
            return;
        }
        try {
            FirebaseAuth auth = CloudManager.auth(this);
            if (auth == null) {
                toast("Simpan konfigurasi Firebase terlebih dahulu");
                return;
            }
            if (create) {
                auth.createUserWithEmailAndPassword(e, p).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        toast("Akun berhasil dibuat");
                        enqueueSync();
                    } else {
                        toast("Pendaftaran gagal: " + shortError(task.getException()));
                    }
                    updateCloudStatus(status);
                });
            } else {
                auth.signInWithEmailAndPassword(e, p).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        toast("Berhasil masuk");
                        enqueueSync();
                    } else {
                        toast("Login gagal: " + shortError(task.getException()));
                    }
                    updateCloudStatus(status);
                });
            }
        } catch (Exception ex) {
            toast("Konfigurasi Firebase tidak valid: " + shortError(ex));
        }
    }

    private void updateCloudStatus(TextView status) {
        if (!CloudManager.isConfigured(this)) {
            status.setText("Status: cloud belum dikonfigurasi. Data lokal tetap aman di perangkat.");
            return;
        }
        try {
            FirebaseAuth auth = CloudManager.auth(this);
            FirebaseUser user = auth == null ? null : auth.getCurrentUser();
            if (user == null) {
                status.setText("Status: Firebase siap, belum login.");
            } else {
                status.setText("Status: tersambung sebagai " + (user.getEmail() == null ? user.getUid() : user.getEmail()) + ". Pending lokal: " + db.countPendingSync());
            }
        } catch (Exception e) {
            status.setText("Status: konfigurasi cloud perlu diperiksa.");
        }
    }

    private String cloudHint() {
        if (!CloudManager.isConfigured(this)) {
            return "Mode offline aktif • " + db.countPendingSync() + " perubahan menunggu sinkronisasi. Atur Firebase di menu Profil.";
        }
        return "Offline-first aktif • " + db.countPendingSync() + " perubahan lokal menunggu cloud.";
    }

    private void enqueueSync() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "finelog-cloud-sync", ExistingPeriodicWorkPolicy.KEEP, request);
    }

    private void pickDate(EditText target) {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) ->
                target.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(8), dp(6), dp(8), dp(6));
        return page;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return b;
    }

    private TextView heading(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(22);
        v.setPadding(0, dp(6), 0, dp(8));
        return v;
    }

    private EditText field(String hint, String value) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setText(value);
        return field;
    }

    private LinearLayout.LayoutParams fullWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void setPage(View view) {
        content.removeAllViews();
        content.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String shortError(Exception e) {
        if (e == null || e.getMessage() == null) return "kesalahan tidak diketahui";
        String m = e.getMessage();
        return m.length() > 120 ? m.substring(0, 120) : m;
    }
}
