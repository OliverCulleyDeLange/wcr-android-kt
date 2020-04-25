package uk.co.oliverdelange.wcr_android_kt.db.dao.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.Topo
import java.util.*

/**
 * Saves a topo report to the DB. This happens when a user lets us know something isn't right with a topo
 * For example, a route has the wrong grade, or type, or the topo path is wrong, is the image is offensive etc
 * */
fun reportTopo(topo: Topo, report: String) {
    Firebase.firestore
            .collection("reports")
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
                //TODO Analytics
            }
}