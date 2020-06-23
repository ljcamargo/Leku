package com.schibstedspain.leku.utils;

import android.content.Context;

import com.huawei.hms.api.Api;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationServices;

import io.reactivex.rxjava3.core.ObservableEmitter;

public abstract class BaseLocationObservableOnSubscribe<T> extends BaseObservableOnSubscribe<T> {

    protected BaseLocationObservableOnSubscribe(Context ctx) {
        super(ctx);
    }

    @Override
    protected final void onHuaweiApiClientReady(Context context, HuaweiApiClient googleApiClient, ObservableEmitter<? super T> emitter) {
        onLocationProviderClientReady(LocationServices.getFusedLocationProviderClient(context), emitter);
    }

    protected abstract void onLocationProviderClientReady(FusedLocationProviderClient locationProviderClient,
                                                          ObservableEmitter<? super T> emitter);
}
