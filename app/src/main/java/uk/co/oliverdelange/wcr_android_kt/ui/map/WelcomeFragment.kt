package uk.co.oliverdelange.wcr_android_kt.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.R
import uk.co.oliverdelange.wcr_android_kt.di.Injectable

class WelcomeFragment : Fragment(), Injectable {
    companion object {
        fun newWelcomeFragment(): WelcomeFragment {
            return WelcomeFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Timber.d("WelcomeFragment : onCreateView")
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }
}