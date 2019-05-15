package uk.co.oliverdelange.wcr_android_kt.service

import androidx.work.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.BuildConfig
import java.util.concurrent.TimeUnit

fun syncTopoOnSubmit() {
    WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<UploadToposWorker>()
            .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
            .build())

    WorkManager.getInstance().enqueue(OneTimeWorkRequestBuilder<UploadRoutesWorker>()
            .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())
            .build())
}

fun enqueueCloudDownloads() {
    Timber.d("Enqueueing cloud downloads")
    val duration = if (BuildConfig.DEBUG) 7L else 15L
    val unit = if (BuildConfig.DEBUG) TimeUnit.DAYS else TimeUnit.MINUTES
    val workManager = WorkManager.getInstance()
    workManager.enqueueUniquePeriodicWork(
            "download-locations",
            ExistingPeriodicWorkPolicy.REPLACE,
            PeriodicWorkRequestBuilder<DownloadWorker>(duration, unit).build())
}