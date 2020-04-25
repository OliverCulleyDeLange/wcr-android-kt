package uk.co.oliverdelange.wcr_android_kt.db.dao.remote

import android.os.Bundle
import com.google.firebase.Timestamp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import java.util.*
import javax.inject.Inject

class TopoReporter @Inject constructor(private val analytics: FirebaseAnalytics, private val firebase: FirebaseFirestore) {

    /**
     * Saves a topo report to the DB. This happens when a user lets us know something isn't right with a topo
     * For example, a route has the wrong grade, or type, or the topo path is wrong, is the image is offensive etc
     * */
    fun reportTopo(topo: Topo, report: String) {
        firebase.collection("reports")
                .add(mapOf(
                        "date" to Timestamp(Date()),
                        "report" to report,
                        "topo" to topo
                ))
                .addOnSuccessListener {
                    Timber.i("Report submitted: $report. Topo: $topo")
                }
                .addOnFailureListener {
                    Timber.e("Report failed to submit: $report. Topo: $topo \n $it")
                    // This is weird as analytics has all needed info to investigate.
                    // Why not just use Analytics to store reports?
                    analytics.logEvent("wcr_report_topo_failure", Bundle().apply {
                        putString("report", report)
                        putString("date", Date().toString())
                        putString("topoid", topo.id)
                    })
                }
    }
}
