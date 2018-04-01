package uk.co.oliverdelange.wcr_android_kt.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import uk.co.oliverdelange.wcr_android_kt.MapsActivity;

@Module
public abstract class MainActivityModule {
    @ContributesAndroidInjector(modules = FragmentBuildersModule.class)
    abstract MapsActivity contributeMainActivity();
}
