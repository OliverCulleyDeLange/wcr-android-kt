package uk.co.oliverdelange.wcr_android_kt.di;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.LocationDao;
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.RouteDao;
import uk.co.oliverdelange.wcr_android_kt.db.dao.local.TopoDao;
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb;

@Module(includes = ViewModelModule.class)
class AppModule {
    @Singleton
    @Provides
    WcrDb provideDb(Application app) {
        return WcrDb.Companion.getInstance(app);
    }

    @Singleton
    @Provides
    LocationDao provideLocationDao(WcrDb db) {
        return db.locationDao();
    }

    @Singleton
    @Provides
    TopoDao provideTopoDao(WcrDb db) {
        return db.topoDao();
    }

    @Singleton
    @Provides
    RouteDao provideRouteDao(WcrDb db) {
        return db.routeDao();
    }

    @Singleton
    @Provides
    FirebaseFirestore provideFirebaseFirestore() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        return firestore;
    }
}
