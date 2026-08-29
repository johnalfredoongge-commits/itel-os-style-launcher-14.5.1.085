package com.ronald.itelstylelauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private final ArrayList<AppItem> allApps = new ArrayList<>();
    private final ArrayList<AppItem> visibleApps = new ArrayList<>();
    private GridView grid;
    private EditText search;
    private AppAdapter adapter;
    private TextView clock;
    private TextView date;
    private int columns = 4;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        buildUi();
        loadApps();
        startClock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadApps();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(12));
        root.setBackgroundColor(Color.TRANSPARENT);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout timeBox = new LinearLayout(this);
        timeBox.setOrientation(LinearLayout.VERTICAL);
        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(34);
        clock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        date = new TextView(this);
        date.setTextColor(0xDDFFFFFF);
        date.setTextSize(13);
        timeBox.addView(clock);
        timeBox.addView(date);
        header.addView(timeBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button defaultButton = button("Default");
        defaultButton.setOnClickListener(v -> requestDefaultLauncher());
        header.addView(defaultButton, new LinearLayout.LayoutParams(dp(96), dp(44)));

        Button gridButton = button("Grid 4");
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(dp(86), dp(44));
        gridLp.leftMargin = dp(8);
        header.addView(gridButton, gridLp);
        gridButton.setOnClickListener(v -> {
            columns = columns == 4 ? 5 : columns == 5 ? 6 : 4;
            grid.setNumColumns(columns);
            gridButton.setText("Grid " + columns);
        });

        root.addView(header);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Cari aplikasi…");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(0xCCFFFFFF);
        search.setPadding(dp(16), 0, dp(16), 0);
        search.setBackground(roundRect(0xAA20242B, 22));
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        searchLp.topMargin = dp(18);
        searchLp.bottomMargin = dp(10);
        root.addView(search, searchLp);

        grid = new GridView(this);
        grid.setNumColumns(columns);
        grid.setVerticalSpacing(dp(10));
        grid.setHorizontalSpacing(dp(6));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(4), 0, dp(16));
        adapter = new AppAdapter();
        grid.setAdapter(adapter);
        root.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterApps(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        grid.setOnItemClickListener((parent, view, position, id) -> openApp(visibleApps.get(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            showAppMenu(view, visibleApps.get(position));
            return true;
        });

        setContentView(root);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(query, 0);
        ArrayList<AppItem> loaded = new ArrayList<>();
        for (ResolveInfo info : infos) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (getPackageName().equals(pkg)) continue;
            CharSequence labelCs = info.loadLabel(pm);
            String label = labelCs == null ? pkg : labelCs.toString();
            Drawable icon = info.loadIcon(pm);
            ComponentName component = new ComponentName(pkg, info.activityInfo.name);
            loaded.add(new AppItem(label, pkg, component, icon));
        }
        Collections.sort(loaded, Comparator.comparing(a -> a.label.toLowerCase(Locale.ROOT)));
        allApps.clear();
        allApps.addAll(loaded);
        filterApps(search == null ? "" : search.getText().toString());
    }

    private void filterApps(String text) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        visibleApps.clear();
        for (AppItem item : allApps) {
            if (q.isEmpty() || item.label.toLowerCase(Locale.ROOT).contains(q) || item.packageName.toLowerCase(Locale.ROOT).contains(q)) {
                visibleApps.add(item);
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void openApp(AppItem item) {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(item.component);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Aplikasi tidak dapat dibuka", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAppMenu(View anchor, AppItem item) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Buka");
        menu.getMenu().add("Info aplikasi");
        menu.getMenu().add("Uninstall");
        menu.setOnMenuItemClickListener(mi -> {
            String title = mi.getTitle().toString();
            if (title.equals("Buka")) openApp(item);
            else if (title.equals("Info aplikasi")) {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + item.packageName));
                startActivity(i);
            } else if (title.equals("Uninstall")) {
                Intent i = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + item.packageName));
                startActivity(i);
            }
            return true;
        });
        menu.show();
    }

    private void requestDefaultLauncher() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RoleManager rm = (RoleManager) getSystemService(ROLE_SERVICE);
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    if (rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                        Toast.makeText(this, "Launcher ini sudah menjadi Home default", Toast.LENGTH_SHORT).show();
                    } else {
                        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 701);
                    }
                    return;
                }
            }
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Pilih launcher default")
                    .setMessage("Tekan tombol Home pada perangkat, lalu pilih itel OS Style Launcher dan pilih Selalu.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void startClock() {
        handler.post(new Runnable() {
            @Override public void run() {
                Locale locale = new Locale("id", "ID");
                clock.setText(new SimpleDateFormat("HH:mm", locale).format(new Date()));
                date.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(new Date()));
                handler.postDelayed(this, 30000);
            }
        });
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setBackground(roundRect(0xDD20C67A, 20));
        return b;
    }

    private GradientDrawable roundRect(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private static class AppItem {
        final String label;
        final String packageName;
        final ComponentName component;
        final Drawable icon;
        AppItem(String label, String packageName, ComponentName component, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.component = component;
            this.icon = icon;
        }
    }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return visibleApps.size(); }
        @Override public Object getItem(int position) { return visibleApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (convertView == null) {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(3), dp(8), dp(3), dp(8));
                icon = new ImageView(MainActivity.this);
                icon.setId(android.R.id.icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                cell.addView(icon, new LinearLayout.LayoutParams(dp(58), dp(58)));
                label = new TextView(MainActivity.this);
                label.setId(android.R.id.text1);
                label.setTextColor(Color.WHITE);
                label.setTextSize(12);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(5);
                cell.addView(label, lp);
            } else {
                cell = (LinearLayout) convertView;
                icon = cell.findViewById(android.R.id.icon);
                label = cell.findViewById(android.R.id.text1);
            }
            AppItem item = visibleApps.get(position);
            icon.setImageDrawable(item.icon);
            label.setText(item.label);
            return cell;
        }
    }
}
