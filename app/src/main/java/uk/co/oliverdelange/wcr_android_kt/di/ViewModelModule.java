package uk.co.oliverdelange.wcr_android_kt.di;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import uk.co.oliverdelange.wcr_android_kt.ui.map.MapViewModel;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.RouteViewModel;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitLocationViewModel;
import uk.co.oliverdelange.wcr_android_kt.ui.submit.SubmitTopoViewModel;

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
    @IntoMap
    @ViewModelKey(RouteViewModel.class)
    abstract ViewModel bindRouteViewModel(RouteViewModel routeViewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(WcrViewModelFactory factory);
}
