package uk.co.oliverdelange.wcr_android_kt.di;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao;
import uk.co.oliverdelange.wcr_android_kt.db.RouteDao;
import uk.co.oliverdelange.wcr_android_kt.db.TopoDao;
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
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
        return firestore;
    }
}
