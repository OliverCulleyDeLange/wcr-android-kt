package uk.co.oliverdelange.wcr_android_kt.ui.submit

import android.app.Activity
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.ui.map.EXTRA_SECTOR_ID
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitTopoFragment.Companion.newTopoSubmissionFor
import uk.co.oliverdelange.wcr_android_kt.util.addFragment
import javax.inject.Inject

class SubmitActivity : AppCompatActivity(), SubmitTopoFragment.ActivityInteractor,
        HasSupportFragmentInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    override fun supportFragmentInjector(): DispatchingAndroidInjector<Fragment> {
        return dispatchingAndroidInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit)
        val fragment = newTopoSubmissionFor(intent.getLongExtra(EXTRA_SECTOR_ID, -1))
        addFragment(fragment, R.id.submit_topo_container)
    }

    override fun onTopoSubmitted(submittedTopoAndRouteIds: Pair<Long, Array<Long>>?) {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}