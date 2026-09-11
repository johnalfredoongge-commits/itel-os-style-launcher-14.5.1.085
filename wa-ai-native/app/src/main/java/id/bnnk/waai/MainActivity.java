package id.bnnk.waai;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<JSONObject> chats = new ArrayList<>();
    private final ArrayList<JSONObject> currentMessages = new ArrayList<>();
    private LinearLayout root, body;
    private TextView title, subtitle;
    private String selectedWaId = null;
    private String selectedName = null;
    private boolean destroyed = false;
    private final Runnable poller = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            if (selectedWaId == null) loadChats(false); else loadSelectedChat(false);
            main.postDelayed(this, 5000);
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 94, 84));
        showChatList();
        if (serverBase().isEmpty()) main.postDelayed(this::showServerDialog, 350);
        main.postDelayed(poller, 1000);
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    private TextView text(String s, float sp, int color) {
        TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); return v;
    }
    private void pad(View v, int l, int t, int r, int b) { v.setPadding(dp(l), dp(t), dp(r), dp(b)); }

    private void buildShell(String screenTitle, String screenSubtitle, boolean back) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(245,247,248));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setBackgroundColor(Color.rgb(7,94,84)); pad(top,8,8,8,8);
        if (back) {
            Button b = new Button(this); b.setText("‹"); b.setTextSize(26); b.setTextColor(Color.WHITE); b.setBackgroundColor(Color.TRANSPARENT); b.setMinWidth(dp(48)); b.setOnClickListener(v -> showChatList());
            top.addView(b, new LinearLayout.LayoutParams(dp(52), dp(52)));
        }
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); titles.setPadding(dp(6),0,0,0);
        title = text(screenTitle, 20, Color.WHITE); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle = text(screenSubtitle, 12, 0xFFDBF4EF);
        titles.addView(title); titles.addView(subtitle);
        top.addView(titles, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button settings = new Button(this); settings.setText("⚙"); settings.setTextSize(20); settings.setTextColor(Color.WHITE); settings.setBackgroundColor(Color.TRANSPARENT); settings.setOnClickListener(v -> showServerDialog());
        top.addView(settings, new LinearLayout.LayoutParams(dp(56), dp(52)));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(72)));
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showChatList() {
        selectedWaId = null; selectedName = null;
        buildShell("WA AI Operator", "WhatsApp Business • sinkron server", false);
        TextView notice = text("Status resmi: terkirim • diterima • dibaca. Presence kontak WhatsApp (online/last seen/typing) tidak dipantau.", 12, 0xFF4A5A5A);
        notice.setBackgroundColor(0xFFE8F5E9); pad(notice,14,10,14,10); body.addView(notice);
        Button refresh = new Button(this); refresh.setText("Muat ulang percakapan"); refresh.setOnClickListener(v -> loadChats(true)); body.addView(refresh, new LinearLayout.LayoutParams(-1, dp(48)));
        loadChats(true);
    }

    private void loadChats(boolean showLoading) {
        if (serverBase().isEmpty()) { if (showLoading) showEmpty("Atur alamat server terlebih dahulu melalui ikon ⚙."); return; }
        if (showLoading) showEmpty("Menghubungkan ke server...");
        io.execute(() -> {
            try {
                JSONArray arr = new JSONArray(get("/api/chats"));
                chats.clear(); for (int i=0;i<arr.length();i++) chats.add(arr.getJSONObject(i));
                main.post(this::renderChats);
            } catch (Exception e) { main.post(() -> showEmpty("Belum dapat terhubung.\n" + readableError(e))); }
        });
    }

    private void renderChats() {
        if (selectedWaId != null) return;
        body.removeViews(2, Math.max(0, body.getChildCount()-2));
        if (chats.isEmpty()) { showEmpty("Belum ada percakapan masuk."); return; }
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for (JSONObject c: chats) {
            String wa = c.optString("waId", ""); String name = c.optString("name", wa); JSONArray msgs = c.optJSONArray("messages"); String last = "Belum ada pesan";
            if (msgs != null && msgs.length()>0) last = msgs.optJSONObject(msgs.length()-1).optString("text", "Pesan");
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); pad(row,14,10,14,10); row.setBackgroundColor(Color.WHITE);
            TextView avatar = text(name.isEmpty()?"?":name.substring(0,1).toUpperCase(Locale.ROOT), 20, Color.WHITE); avatar.setGravity(Gravity.CENTER); avatar.setBackgroundColor(0xFF128C7E);
            row.addView(avatar, new LinearLayout.LayoutParams(dp(48),dp(48)));
            LinearLayout tt = new LinearLayout(this); tt.setOrientation(LinearLayout.VERTICAL); tt.setPadding(dp(12),0,0,0);
            TextView n = text(name,16,0xFF172B2B); n.setTypeface(Typeface.DEFAULT,Typeface.BOLD); TextView l = text(last,13,0xFF687676); l.setSingleLine(true); l.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tt.addView(n); tt.addView(l); row.addView(tt,new LinearLayout.LayoutParams(0,dp(56),1));
            row.setOnClickListener(v -> { selectedWaId = wa; selectedName = name; showChatDetail(); });
            list.addView(row,new LinearLayout.LayoutParams(-1,dp(72)));
            View divider = new View(this); divider.setBackgroundColor(0xFFE6EAEA); list.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
        }
        body.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }

    private void showChatDetail() {
        buildShell(selectedName == null ? "Percakapan" : selectedName, selectedWaId == null ? "" : selectedWaId, true);
        FrameLayout messagesHolder = new FrameLayout(this); messagesHolder.setId(View.generateViewId()); body.addView(messagesHolder,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer = new LinearLayout(this); composer.setGravity(Gravity.CENTER_VERTICAL); composer.setBackgroundColor(Color.WHITE); pad(composer,8,6,8,6);
        EditText input = new EditText(this); input.setHint("Tulis balasan..."); input.setTextSize(15); input.setMaxLines(4); input.setSingleLine(false); input.setImeOptions(EditorInfo.IME_ACTION_SEND); input.setBackgroundColor(0xFFF2F4F4); pad(input,12,8,12,8);
        Button send = new Button(this); send.setText("Kirim"); send.setTextColor(Color.WHITE); send.setBackgroundColor(0xFF25D366);
        composer.addView(input,new LinearLayout.LayoutParams(0,dp(52),1)); composer.addView(send,new LinearLayout.LayoutParams(dp(86),dp(52))); body.addView(composer,new LinearLayout.LayoutParams(-1,dp(64)));
        send.setOnClickListener(v -> { String msg=input.getText().toString().trim(); if(!msg.isEmpty()){ input.setText(""); sendMessage(msg); }});
        input.setOnEditorActionListener((v, actionId, event) -> { if(actionId==EditorInfo.IME_ACTION_SEND){ send.performClick(); return true;} return false; });
        loadSelectedChat(true);
    }

    private void loadSelectedChat(boolean loading) {
        final String wa = selectedWaId; if (wa == null || serverBase().isEmpty()) return;
        io.execute(() -> {
            try {
                JSONArray arr = new JSONArray(get("/api/chats")); JSONObject found=null;
                for(int i=0;i<arr.length();i++){ JSONObject c=arr.getJSONObject(i); if(wa.equals(c.optString("waId"))){found=c;break;} }
                currentMessages.clear(); if(found!=null){ JSONArray msgs=found.optJSONArray("messages"); if(msgs!=null) for(int i=0;i<msgs.length();i++) currentMessages.add(msgs.getJSONObject(i)); }
                main.post(this::renderMessages);
            } catch(Exception e){ if(loading) main.post(() -> Toast.makeText(this,"Gagal memuat chat: "+readableError(e),Toast.LENGTH_LONG).show()); }
        });
    }

    private void renderMessages() {
        if(selectedWaId==null || body.getChildCount()<1) return;
        View first = body.getChildAt(0); if(!(first instanceof FrameLayout)) return;
        FrameLayout holder=(FrameLayout)first; holder.removeAllViews(); ScrollView scroll=new ScrollView(this); LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); pad(list,8,8,8,12); scroll.addView(list);
        for(JSONObject m: currentMessages){ boolean out="out".equals(m.optString("direction")); String txt=m.optString("text",""); String st=out?m.optString("status","sent"):"masuk";
            LinearLayout wrap=new LinearLayout(this); wrap.setGravity(out?Gravity.RIGHT:Gravity.LEFT); TextView bubble=text(txt+"\n"+(out?"✓ "+st:""),15,0xFF152020); bubble.setBackgroundColor(out?0xFFDCF8C6:Color.WHITE); pad(bubble,12,8,12,8); bubble.setMaxWidth(dp(285)); wrap.addView(bubble,new LinearLayout.LayoutParams(-2,-2)); list.addView(wrap,new LinearLayout.LayoutParams(-1,-2));
        }
        holder.addView(scroll,new FrameLayout.LayoutParams(-1,-1)); main.postDelayed(() -> scroll.fullScroll(View.FOCUS_DOWN),120);
    }

    private void sendMessage(String message) {
        final String wa=selectedWaId; if(wa==null)return;
        io.execute(() -> {
            try {
                JSONObject j=new JSONObject(); j.put("to",wa); j.put("text",message); j.put("operatorId","itel-a50-operator"); post("/api/send",j.toString());
                main.post(() -> loadSelectedChat(false));
            } catch(Exception e){ main.post(() -> Toast.makeText(this,"Pesan gagal dikirim: "+readableError(e),Toast.LENGTH_LONG).show()); }
        });
    }

    private void showEmpty(String message) {
        if(body==null)return; while(body.getChildCount()>2) body.removeViewAt(2); TextView t=text(message,15,0xFF5B6868); t.setGravity(Gravity.CENTER); pad(t,24,30,24,30); body.addView(t,new LinearLayout.LayoutParams(-1,0,1));
    }

    private String serverBase(){ return getSharedPreferences("waai",MODE_PRIVATE).getString("server","").trim().replaceAll("/+$",""); }
    private void showServerDialog(){
        final EditText input=new EditText(this); input.setHint("http://192.168.1.10:8080 atau https://serveranda.com"); input.setText(serverBase()); input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI); pad(input,12,8,12,8);
        new AlertDialog.Builder(this).setTitle("Alamat server WA AI").setMessage("Masukkan URL backend yang menjalankan WhatsApp Business API. Untuk akses dari semua perangkat, gunakan server HTTPS publik.").setView(input)
            .setPositiveButton("Simpan",(d,w)->{ String s=input.getText().toString().trim(); if(!s.isEmpty()&&!s.startsWith("http://")&&!s.startsWith("https://"))s="https://"+s; getSharedPreferences("waai",MODE_PRIVATE).edit().putString("server",s).apply(); showChatList(); })
            .setNegativeButton("Batal",null).show();
    }

    private String get(String path) throws Exception { return request("GET",path,null); }
    private String post(String path,String body) throws Exception { return request("POST",path,body); }
    private String request(String method,String path,String body) throws Exception {
        URL url=new URL(serverBase()+path); HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setRequestMethod(method); c.setConnectTimeout(8000); c.setReadTimeout(12000); c.setRequestProperty("Accept","application/json");
        if(body!=null){ c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json; charset=utf-8"); try(OutputStream os=c.getOutputStream()){os.write(body.getBytes(StandardCharsets.UTF_8));} }
        int code=c.getResponseCode(); InputStream is=(code>=200&&code<300)?c.getInputStream():c.getErrorStream(); String text=readAll(is); c.disconnect(); if(code<200||code>=300)throw new IOException("HTTP "+code+" "+text); return text;
    }
    private String readAll(InputStream is) throws IOException { if(is==null)return""; ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n; while((n=is.read(buf))>0)b.write(buf,0,n); return b.toString("UTF-8"); }
    private String readableError(Exception e){ String m=e.getMessage(); if(m==null||m.isEmpty())m=e.getClass().getSimpleName(); return m.length()>160?m.substring(0,160):m; }

    @Override public void onBackPressed(){ if(selectedWaId!=null) showChatList(); else super.onBackPressed(); }
    @Override protected void onDestroy(){ destroyed=true; main.removeCallbacks(poller); io.shutdownNow(); super.onDestroy(); }
}
