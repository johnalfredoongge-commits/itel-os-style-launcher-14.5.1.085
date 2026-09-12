package id.a50fontstudio.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int REQUEST_FONT = 101;
    private static final String PREFS = "a50_font_prefs";
    private static final String KEY_STYLE = "selected_style";
    private static final String KEY_SIZE = "preview_size";

    private final String[] styleNames = {
            "Modern", "Ringan", "Tegas", "Ramping", "Klasik",
            "Mesin Ketik", "Mono Modern", "Tulisan Tangan", "Santai", "Small Caps"
    };

    private final String[] fontFamilies = {
            "sans-serif", "sans-serif-light", "sans-serif-medium", "sans-serif-condensed",
            "serif", "serif-monospace", "monospace", "cursive", "casual", "sans-serif-smallcaps"
    };

    private TextView preview;
    private TextView sizeLabel;
    private Spinner fontSpinner;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(245, 247, 252));

        LinearLayout root = column(20);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        scroll.addView(root);

        TextView badge = text("DIRANCANG UNTUK ITEL A50", 12, Color.rgb(76, 111, 255));
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(badge);

        TextView title = text("A50 Font Studio", 32, Color.rgb(11, 22, 53));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(8), 0, 0);
        root.addView(title);

        TextView subtitle = text("Pilih, pratinjau, dan gunakan font dengan jalur aman tanpa root.", 15, Color.rgb(77, 88, 112));
        subtitle.setPadding(0, dp(6), 0, dp(20));
        root.addView(subtitle);

        LinearLayout deviceCard = card();
        TextView deviceTitle = text("Perangkat terdeteksi", 13, Color.rgb(102, 112, 133));
        TextView device = text(Build.MANUFACTURER + " " + Build.MODEL + " • Android " + Build.VERSION.RELEASE, 18, Color.rgb(11, 22, 53));
        device.setTypeface(Typeface.DEFAULT_BOLD);
        device.setPadding(0, dp(5), 0, 0);
        deviceCard.addView(deviceTitle);
        deviceCard.addView(device);
        root.addView(deviceCard);

        TextView section = text("Pratinjau font", 20, Color.rgb(11, 22, 53));
        section.setTypeface(Typeface.DEFAULT_BOLD);
        section.setPadding(0, dp(24), 0, dp(10));
        root.addView(section);

        LinearLayout previewCard = card();
        preview = text("Itel A50 tampil lebih menarik\nAa Bb Cc 123", 28, Color.rgb(11, 22, 53));
        preview.setGravity(Gravity.CENTER);
        preview.setMinHeight(dp(150));
        preview.setPadding(dp(12), dp(22), dp(12), dp(22));
        previewCard.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(previewCard);

        fontSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, styleNames);
        fontSpinner.setAdapter(adapter);
        int savedStyle = prefs.getInt(KEY_STYLE, 0);
        fontSpinner.setSelection(savedStyle);
        fontSpinner.setBackground(rounded(Color.WHITE, 18, Color.rgb(220, 225, 237)));
        fontSpinner.setPadding(dp(14), dp(4), dp(14), dp(4));
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        spinnerParams.topMargin = dp(14);
        root.addView(fontSpinner, spinnerParams);
        applyBuiltInFont(savedStyle);

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyBuiltInFont(position);
                prefs.edit().putInt(KEY_STYLE, position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        int savedSize = prefs.getInt(KEY_SIZE, 28);
        sizeLabel = text("Ukuran pratinjau: " + savedSize + " sp", 14, Color.rgb(77, 88, 112));
        sizeLabel.setPadding(0, dp(18), 0, dp(4));
        root.addView(sizeLabel);

        SeekBar sizeBar = new SeekBar(this);
        sizeBar.setMax(28);
        sizeBar.setProgress(savedSize - 16);
        preview.setTextSize(savedSize);
        root.addView(sizeBar);
        sizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int size = progress + 16;
                preview.setTextSize(size);
                sizeLabel.setText("Ukuran pratinjau: " + size + " sp");
                prefs.edit().putInt(KEY_SIZE, size).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        Button importButton = primaryButton("Impor font TTF / OTF");
        importButton.setOnClickListener(v -> openFontPicker());
        addButton(root, importButton, 20);

        Button themeButton = primaryButton("Buka XTheme / Tema Itel");
        themeButton.setOnClickListener(v -> openItelTheme());
        addButton(root, themeButton, 12);

        Button settingsButton = secondaryButton("Buka Pengaturan Tampilan & Font");
        settingsButton.setOnClickListener(v -> openDisplaySettings());
        addButton(root, settingsButton, 12);

        TextView note = text("Catatan: aplikasi Android biasa tidak dapat mengganti file font seluruh sistem tanpa izin sistem atau root. Untuk perubahan seluruh perangkat, gunakan menu font resmi di XTheme/Tema Itel jika tersedia pada firmware Anda.", 13, Color.rgb(92, 102, 124));
        note.setBackground(rounded(Color.rgb(233, 237, 255), 16, Color.TRANSPARENT));
        note.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = dp(18);
        root.addView(note, noteParams);

        return scroll;
    }

    private void applyBuiltInFont(int position) {
        if (preview == null) return;
        String family = fontFamilies[Math.max(0, Math.min(position, fontFamilies.length - 1))];
        preview.setTypeface(Typeface.create(family, Typeface.NORMAL));
    }

    private void openFontPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-opentype"});
        startActivityForResult(intent, REQUEST_FONT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_FONT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            loadCustomFont(data.getData());
        }
    }

    private void loadCustomFont(Uri uri) {
        File cachedFont = new File(getCacheDir(), "imported-font");
        try (InputStream input = getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(cachedFont)) {
            if (input == null) throw new IllegalArgumentException("Berkas tidak dapat dibaca");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            preview.setTypeface(Typeface.createFromFile(cachedFont));
            Toast.makeText(this, "Font berhasil dimuat untuk pratinjau", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Toast.makeText(this, "Font tidak valid atau tidak didukung", Toast.LENGTH_LONG).show();
        }
    }

    private void openItelTheme() {
        String[] packages = {"com.transsion.theme", "com.itel.themestore", "com.transsion.phoenix"};
        for (String packageName : packages) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                startActivity(launch);
                Toast.makeText(this, "Pilih menu Font di aplikasi Tema", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Toast.makeText(this, "XTheme tidak ditemukan. Membuka Pengaturan Tampilan.", Toast.LENGTH_LONG).show();
        openDisplaySettings();
    }

    private void openDisplaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        } catch (ActivityNotFoundException error) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private LinearLayout column(int spacingIgnored) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = column(0);
        layout.setPadding(dp(18), dp(18), dp(18), dp(18));
        layout.setBackground(rounded(Color.WHITE, 20, Color.TRANSPARENT));
        layout.setElevation(dp(2));
        return layout;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    private Button primaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setBackground(rounded(Color.rgb(76, 111, 255), 18, Color.TRANSPARENT));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = primaryButton(value);
        button.setTextColor(Color.rgb(43, 65, 145));
        button.setBackground(rounded(Color.WHITE, 18, Color.rgb(183, 194, 230)));
        return button;
    }

    private void addButton(LinearLayout root, Button button, int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        params.topMargin = dp(topMargin);
        root.addView(button, params);
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
