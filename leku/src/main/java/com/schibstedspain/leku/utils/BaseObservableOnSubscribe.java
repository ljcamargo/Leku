package com.schibstedspain.leku.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.Api;
import com.huawei.hms.api.HuaweiApiClient;

import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;

public abstract class BaseObservableOnSubscribe<T> implements ObservableOnSubscribe<T> {
    private final Context ctx;
    private final List<Api<? extends Api.ApiOptions.NotRequiredOptions>> services;

    @SafeVarargs
    protected BaseObservableOnSubscribe(Context ctx, Api<? extends Api.ApiOptions.NotRequiredOptions>... services) {
        this.ctx = ctx;
        this.services = Arrays.asList(services);
    }

    @Override
    public void subscribe(ObservableEmitter<T> emitter) throws Exception {
        final HuaweiApiClient apiClient = createApiClient(emitter);
        try {
            apiClient.connect(0);
        } catch (Throwable ex) {
            if (!emitter.isDisposed()) {
                emitter.onError(ex);
            }
        }

        emitter.setDisposable(Disposable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                onDisposed();
                apiClient.disconnect();
            }
        }));
    }

    private HuaweiApiClient createApiClient(ObservableEmitter<? super T> emitter) {
        ApiClientConnectionCallbacks apiClientConnectionCallbacks = new ApiClientConnectionCallbacks(ctx, emitter);
        HuaweiApiClient.Builder apiClientBuilder = new HuaweiApiClient.Builder(ctx);

        for (Api<? extends Api.ApiOptions.NotRequiredOptions> service : services) {
            apiClientBuilder = apiClientBuilder.addApi(service);
        }

        apiClientBuilder = apiClientBuilder
                .addConnectionCallbacks(apiClientConnectionCallbacks)
                .addOnConnectionFailedListener(apiClientConnectionCallbacks);

        HuaweiApiClient apiClient = apiClientBuilder.build();
        apiClientConnectionCallbacks.setClient(apiClient);
        return apiClient;
    }

    protected void onDisposed() {
    }

    protected abstract void onHuaweiApiClientReady(Context context, HuaweiApiClient googleApiClient, ObservableEmitter<? super T> emitter);

    private class ApiClientConnectionCallbacks implements
            HuaweiApiClient.ConnectionCallbacks,
            HuaweiApiClient.OnConnectionFailedListener {

        final private Context context;

        final private ObservableEmitter<? super T> emitter;

        private HuaweiApiClient apiClient;

        private ApiClientConnectionCallbacks(Context context, ObservableEmitter<? super T> emitter) {
            this.context = context;
            this.emitter = emitter;
        }

        @Override
        public void onConnected() {
            try {
                onHuaweiApiClientReady(context, apiClient, emitter);
            } catch (Throwable ex) {
                if (!emitter.isDisposed()) {
                    emitter.onError(ex);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int cause) {
            if (!emitter.isDisposed()) {
                emitter.onError(new IllegalStateException());
            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            if (!emitter.isDisposed()) {
                emitter.onError(new IllegalStateException("Error connecting to GoogleApiClient"));
            }
        }

        void setClient(HuaweiApiClient client) {
            this.apiClient = client;
        }
    }
}
