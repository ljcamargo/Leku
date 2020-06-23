package com.schibstedspain.leku.utils;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;

import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;

public class LastKnownLocationObservableOnSubscribe extends BaseLocationObservableOnSubscribe<Location> {

    public static Observable<Location> createObservable(Context ctx) {
        return Observable.create(new LastKnownLocationObservableOnSubscribe(ctx));
    }

    private LastKnownLocationObservableOnSubscribe(Context ctx) {
        super(ctx);
    }

    @Override
    protected void onLocationProviderClientReady(FusedLocationProviderClient locationProviderClient,
                                                 final ObservableEmitter<? super Location> emitter) {
        locationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (emitter.isDisposed()) return;
                    if (location != null) {
                        emitter.onNext(location);
                    }
                    emitter.onComplete();
                })
                .addOnFailureListener(new BaseFailureListener<>(emitter));
    }

    public static class BaseFailureListener<T> implements OnFailureListener {

        private final ObservableEmitter<? super T> emitter;

        public BaseFailureListener(ObservableEmitter<? super T> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            if (emitter.isDisposed()) return;
            emitter.onError(exception);
            emitter.onComplete();
        }
    }
}
