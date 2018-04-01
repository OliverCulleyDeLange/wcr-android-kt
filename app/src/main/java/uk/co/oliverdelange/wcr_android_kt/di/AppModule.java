package uk.co.oliverdelange.wcr_android_kt.di;

import android.app.Application;
import android.arch.persistence.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import uk.co.oliverdelange.wcr_android_kt.db.LocationDao;
import uk.co.oliverdelange.wcr_android_kt.db.WcrDb;

@Module(includes = ViewModelModule.class)
class AppModule {
    @Singleton
    @Provides
    WcrDb provideDb(Application app) {
        return Room.databaseBuilder(app, WcrDb.class, "wcr.db").build();
    }

    @Singleton
    @Provides
    LocationDao provideLocationDao(WcrDb db) {
        return db.locationDao();
    }
}
