package uk.co.oliverdelange.wcr_android_kt.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import uk.co.oliverdelange.wcr_android_kt.viewmodel.MapViewModel;
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmitLocationViewModel;
import uk.co.oliverdelange.wcr_android_kt.viewmodel.SubmitTopoViewModel;

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(MapViewModel.class)
    abstract ViewModel bindUserViewModel(MapViewModel mapViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SubmitLocationViewModel.class)
    abstract ViewModel bindLocationViewModel(SubmitLocationViewModel submitLocationViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SubmitTopoViewModel.class)
    abstract ViewModel bindTopoViewModel(SubmitTopoViewModel submitTopoViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(WcrViewModelFactory factory);
}
