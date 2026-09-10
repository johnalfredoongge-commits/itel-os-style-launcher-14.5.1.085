package com.ronald.finelog;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public final class CloudManager {
    private static final String PREFS = "finelog_cloud";
    private static final String APP_NAME = "FineLogCloud";
    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_APP_ID = "app_id";
    private static final String KEY_API_KEY = "api_key";

    private CloudManager() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void saveConfig(Context context, String projectId, String appId, String apiKey) {
        prefs(context).edit()
                .putString(KEY_PROJECT_ID, projectId.trim())
                .putString(KEY_APP_ID, appId.trim())
                .putString(KEY_API_KEY, apiKey.trim())
                .apply();
        resetApp();
    }

    public static String getProjectId(Context context) {
        return prefs(context).getString(KEY_PROJECT_ID, "");
    }

    public static String getAppId(Context context) {
        return prefs(context).getString(KEY_APP_ID, "");
    }

    public static String getApiKey(Context context) {
        return prefs(context).getString(KEY_API_KEY, "");
    }

    public static boolean isConfigured(Context context) {
        return !getProjectId(context).isEmpty()
                && !getAppId(context).isEmpty()
                && !getApiKey(context).isEmpty();
    }

    public static FirebaseApp getOrCreateApp(Context context) {
        if (!isConfigured(context)) return null;
        try {
            return FirebaseApp.getInstance(APP_NAME);
        } catch (IllegalStateException ignored) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId(getProjectId(context))
                    .setApplicationId(getAppId(context))
                    .setApiKey(getApiKey(context))
                    .build();
            return FirebaseApp.initializeApp(context.getApplicationContext(), options, APP_NAME);
        }
    }

    public static FirebaseAuth auth(Context context) {
        FirebaseApp app = getOrCreateApp(context);
        return app == null ? null : FirebaseAuth.getInstance(app);
    }

    public static FirebaseFirestore firestore(Context context) {
        FirebaseApp app = getOrCreateApp(context);
        return app == null ? null : FirebaseFirestore.getInstance(app);
    }

    private static void resetApp() {
        try {
            FirebaseApp.getInstance(APP_NAME).delete();
        } catch (IllegalStateException ignored) {
        }
    }
}
