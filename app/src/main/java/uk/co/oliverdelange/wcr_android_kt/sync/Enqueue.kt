package uk.co.oliverdelange.wcr_android_kt.sync

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
    // 15 mins is the minimum
    val duration = if (BuildConfig.DEBUG) 15 else 24L
    val unit = if (BuildConfig.DEBUG) TimeUnit.MINUTES else TimeUnit.HOURS
    Timber.d("Enqueueing cloud downloads every $duration $unit")
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