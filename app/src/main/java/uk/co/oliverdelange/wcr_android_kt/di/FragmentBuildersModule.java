package uk.co.oliverdelange.wcr_android_kt.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import uk.co.oliverdelange.wcr_android_kt.view.map.BottomSheetFragment;
import uk.co.oliverdelange.wcr_android_kt.view.map.WelcomeFragment;
import uk.co.oliverdelange.wcr_android_kt.view.submit.SubmitLocationFragment;
import uk.co.oliverdelange.wcr_android_kt.view.submit.SubmitRouteFragment;
import uk.co.oliverdelange.wcr_android_kt.view.submit.SubmitTopoFragment;

@Module
public abstract class FragmentBuildersModule {
    @ContributesAndroidInjector
    abstract SubmitLocationFragment contributeSubmitFragment();

    @ContributesAndroidInjector
    abstract SubmitTopoFragment contributeTopoFragment();

    @ContributesAndroidInjector
    abstract SubmitRouteFragment contributeSubmitRouteFragment();

    @ContributesAndroidInjector
    abstract BottomSheetFragment contributeBottomSheetFragment();

    @ContributesAndroidInjector
    abstract WelcomeFragment contributeWelcomeFragment();
}
