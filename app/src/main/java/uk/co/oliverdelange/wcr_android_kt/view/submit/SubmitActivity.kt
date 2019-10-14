package uk.co.oliverdelange.wcr_android_kt.view.submit

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.util.addFragment
import uk.co.oliverdelange.wcr_android_kt.view.map.EXTRA_SECTOR_ID
import uk.co.oliverdelange.wcr_android_kt.view.submit.SubmitTopoFragment.Companion.newTopoSubmissionFor
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
        val fragment = newTopoSubmissionFor(intent.getStringExtra(EXTRA_SECTOR_ID))
        addFragment(fragment, R.id.submit_topo_container)
    }

    override fun onTopoSubmitted(submittedTopoId: String?) {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}