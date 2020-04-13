package uk.co.oliverdelange.wcr_android_kt

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec

// Based off https://jeroenmols.com/blog/2019/01/17/livedatajunit5/
class InstantExecutorListener : TestListener {
    override suspend fun afterSpec(spec: Spec) {
        super.afterSpec(spec)
        print("Removing Instant Executor Delegate")
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    override suspend fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread(): Boolean = true
        })
        print("Delegating to Instant Executor")
    }
}