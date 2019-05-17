package uk.co.oliverdelange.wcr_android_kt.service

import androidx.work.*
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.BuildConfig
import java.util.concurrent.TimeUnit

fun uploadSync() {
    Timber.d("Enqueueing cloud uploads")
    WorkManager.getInstance()
            .enqueue(OneTimeWorkRequestBuilder<UploadWorker>()
                    .setConstraints(
                            Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.CONNECTED)
                                    .build()
                    )
                    .build())
}

fun downloadSync() {
    Timber.d("Enqueueing cloud downloads")
    val duration = if (BuildConfig.DEBUG) 7L else 15L
    val unit = if (BuildConfig.DEBUG) TimeUnit.DAYS else TimeUnit.MINUTES
    WorkManager.getInstance()
            .enqueueUniquePeriodicWork(
                    "download-locations",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    PeriodicWorkRequestBuilder<DownloadWorker>(duration, unit)
                            .setConstraints(
                                    Constraints.Builder()
                                            .setRequiredNetworkType(NetworkType.CONNECTED)
                                            .build()
                            ).build()
            )
}