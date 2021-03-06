package com.roodie.model.tasks;


import com.roodie.model.network.NetworkCallRunnable;
import com.roodie.model.network.NetworkError;
import com.roodie.model.state.ApplicationState;
import com.roodie.model.state.BaseState;
import com.roodie.model.state.EntityMapper;
import com.roodie.model.util.CountryProvider;
import com.squareup.otto.Bus;
import com.uwetrottmann.tmdb.Tmdb;

import javax.inject.Inject;

import dagger.Lazy;
import retrofit.RetrofitError;

/**
 * Created by Roodie on 24.06.2015.
 */
public abstract class BaseRunnable<R> extends NetworkCallRunnable<R> {

    public static final String LOG_TAG = BaseRunnable.class.getSimpleName();

    @Inject
    ApplicationState mState;

    @Inject Lazy<Tmdb> mTmdbClient;
    @Inject Lazy<Bus> mEventBus;
    @Inject Lazy<CountryProvider> mCountryProvider;
    @Inject Lazy<EntityMapper> mLazyEntityMapper;



    private final int mCallingId;

    public BaseRunnable(int callingId) {
        mCallingId = callingId;
    }

    @Override
    public void onPreExecute() {
        getEventBus().post(createLoadingProgressEvent(true));
    }


    @Override
    public void onError(RetrofitError re) {
        getEventBus().post(new BaseState.OnErrorEvent(getCallingId(),
                NetworkError.from(re)));

    }

    @Override
    public void onFinished() {
       getEventBus().post(createLoadingProgressEvent(false));
    }

    protected Bus getEventBus() {return mEventBus.get(); }

    public  Tmdb getTmdbClient() {
        return mTmdbClient.get();
    }

    public CountryProvider getCountryProvider() {
        return mCountryProvider.get();
    }

    public int getCallingId() {
        return mCallingId;
    }

    protected Object createLoadingProgressEvent(boolean show) {
        return new BaseState.ShowLoadingProgressEvent(getCallingId(), show);
    }

    public EntityMapper getEntityMapper() {
        return mLazyEntityMapper.get();
    }
}
