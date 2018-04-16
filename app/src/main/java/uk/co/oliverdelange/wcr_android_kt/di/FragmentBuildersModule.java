package uk.co.oliverdelange.wcr_android_kt.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import uk.co.oliverdelange.wcr_android_kt.ui.map.ToposFragment;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.RouteFragment;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitLocationFragment;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitTopoFragment;

@Module
public abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract SubmitLocationFragment contributeSubmitFragment();

    @ContributesAndroidInjector
    abstract SubmitTopoFragment contributeTopoFragment();

    @ContributesAndroidInjector
    abstract RouteFragment contributeRouteFragment();

    @ContributesAndroidInjector
    abstract ToposFragment contributeToposFragment();
}
