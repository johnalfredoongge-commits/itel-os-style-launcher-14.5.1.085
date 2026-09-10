package com.ronald.finelog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FineLogDbHelper db = new FineLogDbHelper(getApplicationContext());
        int pending = db.countPendingSync();

        // Fondasi sinkronisasi multi-perangkat. Saat endpoint backend/Firebase
        // dikonfigurasi, kirim perubahan lokal dan tarik perubahan terbaru di sini.
        // Untuk sekarang aplikasi tetap aman digunakan secara offline.
        if (pending >= 0) {
            return Result.success();
        }
        return Result.retry();
    }
}
