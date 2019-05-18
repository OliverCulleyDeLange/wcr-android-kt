package uk.co.oliverdelange.wcr_android_kt.testutil

import java.util.concurrent.Executor

class InstantExecutors {
    private static Executor instant = {
        command -> command.run()
    }

    InstantExecutors() {
        super(instant, instant, instant)
    }
}