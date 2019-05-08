package uk.co.oliverdelange.wcr_android_kt.service

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

fun syncTopoOnSubmit() {
    WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<SyncToposWorker>()
            .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
            .build())

    WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<SyncRoutesWorker>()
            .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
            .build())
}