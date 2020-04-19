package uk.co.oliverdelange.wcr_android_kt.view.submit

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.util.addFragment
import uk.co.oliverdelange.wcr_android_kt.view.map.EXTRA_SECTOR_ID
import javax.inject.Inject

/*
    The other main activity, used to submit a new topo
 */
class SubmitActivity : AppCompatActivity(), HasSupportFragmentInjector {

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun supportFragmentInjector(): DispatchingAndroidInjector<Fragment> {
        return dispatchingAndroidInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit)
        intent.getStringExtra(EXTRA_SECTOR_ID)?.let {
            val fragment = SubmitTopoFragment(it)
            addFragment(fragment, R.id.submit_topo_container)
        }
    }

    fun end() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}