package uk.co.oliverdelange.wcr_android_kt.di;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapViewModel;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitViewModel;

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(MapViewModel.class)
    abstract ViewModel bindUserViewModel(MapViewModel mapViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SubmitViewModel.class)
    abstract ViewModel bindSearchViewModel(SubmitViewModel submitViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(WcrViewModelFactory factory);
}
