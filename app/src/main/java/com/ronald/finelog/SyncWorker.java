package com.ronald.finelog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!CloudManager.isConfigured(context)) return Result.success();

        try {
            FirebaseAuth auth = CloudManager.auth(context);
            FirebaseFirestore cloud = CloudManager.firestore(context);
            if (auth == null || cloud == null) return Result.success();

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return Result.success();

            FineLogDbHelper local = new FineLogDbHelper(context);
            String uid = user.getUid();

            syncNotes(local, cloud, uid);
            syncTasks(local, cloud, uid);
            return Result.success();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.retry();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private void syncNotes(FineLogDbHelper local, FirebaseFirestore cloud, String uid) throws Exception {
        for (FineLogDbHelper.NoteRecord note : local.pendingNotes()) {
            DocumentReference ref = cloud.collection("users").document(uid).collection("notes").document(note.id);
            DocumentSnapshot remote = Tasks.await(ref.get());
            long remoteUpdated = remote.exists() ? longValue(remote.getLong("updatedAt")) : -1L;

            if (remote.exists() && remoteUpdated > note.updatedAt) {
                local.upsertNoteFromCloud(note.id, stringValue(remote.getString("title")),
                        stringValue(remote.getString("body")), remoteUpdated);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("title", note.title);
                data.put("body", note.body);
                data.put("updatedAt", note.updatedAt);
                Tasks.await(ref.set(data));
                local.markNoteSynced(note.id);
            }
        }

        QuerySnapshot snapshot = Tasks.await(cloud.collection("users").document(uid).collection("notes").get());
        for (QueryDocumentSnapshot doc : snapshot) {
            local.upsertNoteFromCloud(doc.getId(), stringValue(doc.getString("title")),
                    stringValue(doc.getString("body")), longValue(doc.getLong("updatedAt")));
        }
    }

    private void syncTasks(FineLogDbHelper local, FirebaseFirestore cloud, String uid) throws Exception {
        for (FineLogDbHelper.TaskRecord task : local.pendingTasks()) {
            DocumentReference ref = cloud.collection("users").document(uid).collection("tasks").document(task.id);
            DocumentSnapshot remote = Tasks.await(ref.get());
            long remoteUpdated = remote.exists() ? longValue(remote.getLong("updatedAt")) : -1L;

            if (remote.exists() && remoteUpdated > task.updatedAt) {
                local.upsertTaskFromCloud(task.id, stringValue(remote.getString("title")),
                        stringValue(remote.getString("dueDate")), boolValue(remote.getBoolean("done")), remoteUpdated);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("title", task.title);
                data.put("dueDate", task.dueDate);
                data.put("done", task.done);
                data.put("updatedAt", task.updatedAt);
                Tasks.await(ref.set(data));
                local.markTaskSynced(task.id);
            }
        }

        QuerySnapshot snapshot = Tasks.await(cloud.collection("users").document(uid).collection("tasks").get());
        for (QueryDocumentSnapshot doc : snapshot) {
            local.upsertTaskFromCloud(doc.getId(), stringValue(doc.getString("title")),
                    stringValue(doc.getString("dueDate")), boolValue(doc.getBoolean("done")),
                    longValue(doc.getLong("updatedAt")));
        }
    }

    private long longValue(Long value) {
        return value == null ? 0L : value;
    }

    private boolean boolValue(Boolean value) {
        return value != null && value;
    }

    private String stringValue(String value) {
        return value == null ? "" : value;
    }
}
