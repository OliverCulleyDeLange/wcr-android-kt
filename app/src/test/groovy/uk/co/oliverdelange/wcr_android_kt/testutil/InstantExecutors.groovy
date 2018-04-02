package uk.co.oliverdelange.wcr_android_kt.testutil

import uk.co.oliverdelange.wcr_android_kt.util.AppExecutors

import java.util.concurrent.Executor

class InstantExecutors extends AppExecutors {
    private static Executor instant = {
        command -> command.run()
    }

    InstantExecutors() {
        super(instant, instant, instant)
    }
}