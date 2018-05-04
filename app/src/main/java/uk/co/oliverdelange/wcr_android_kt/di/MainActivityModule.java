package uk.co.oliverdelange.wcr_android_kt.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapsActivity;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitActivity;

@Module
public abstract class MainActivityModule {
    @ContributesAndroidInjector(modules = FragmentBuildersModule.class)
    abstract MapsActivity contributeMainActivity();

    @ContributesAndroidInjector(modules = FragmentBuildersModule.class)
    abstract SubmitActivity contributesSubmitActivity();
}
