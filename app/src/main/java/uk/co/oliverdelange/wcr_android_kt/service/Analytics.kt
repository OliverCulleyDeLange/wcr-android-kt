package uk.co.oliverdelange.wcr_android_kt.service

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.*

class Analytics(private val firebaseAnalytics: FirebaseAnalytics) {
    fun logReportTopoFailure(report: String, topoId: String?) {
        firebaseAnalytics.logEvent("wcr_report_topo_failure", Bundle().apply {
            putString("report", report)
            putString("date", Date().toString())
            putString("topoid", topoId)
        })
    }

    fun logLoadTopos(locationId: String, locationType: String?) {
        firebaseAnalytics.logEvent("wcr_load_topos", Bundle().apply {
            putString("locationId", locationId)
            putString("locationType", locationType)
        })
    }

    fun logSearch(query: String) {
        firebaseAnalytics.logEvent("wcr_search", Bundle().apply {
            putString("query", query)
        })
    }


    fun logSubmissionSucceeded(topoId: String) {
        firebaseAnalytics.logEvent("wcr_submission_succeeded", Bundle().apply {
            putString("topoId", topoId)
        })
    }

    fun logSubmissionFailed(error: String?) {
        firebaseAnalytics.logEvent("wcr_submission_failed", Bundle().apply {
            putString("error", error)
        })
    }

    fun logTutorialBegin() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, null)
    }

    fun logLogin() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, null)
    }

    fun logTestEvent() {
        firebaseAnalytics.logEvent("wct_test_analytics_event", Bundle().apply {
            putString("key", "value")
            putString("date", Date().toString())
        })
    }
}