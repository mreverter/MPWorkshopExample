package com.mercadopago.adapters;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.gson.reflect.TypeToken;
import com.mercadopago.callbacks.Callback;
import com.mercadopago.util.ApiUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by mreverter on 6/6/16.
 */
public class ErrorHandlingCallAdapter {
    public static class ErrorHandlingCallAdapterFactory extends CallAdapter.Factory {

        @Override
        public CallAdapter<MPCall<?>> get(Type returnType, Annotation[] annotations,
                                          Retrofit retrofit) {
            TypeToken<?> token = TypeToken.get(returnType);
            if (token.getRawType() != MPCall.class) {
                return null;
            }
            if (!(returnType instanceof ParameterizedType)) {
                throw new IllegalStateException(
                        "MPCall must have generic type (e.g., MPCall<ResponseBody>)");
            }
            final Type responseType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
            return new CallAdapter<MPCall<?>>() {
                @Override public Type responseType() {
                    return responseType;
                }

                @Override public <R> MPCall<R> adapt(Call<R> call) {
                    return new MPCallAdapter<>(call);
                }
            };
        }
    }

    // This adapter runs its callbacks on the main thread always
    // TODO: customize executor
    /** Adapts a {@link Call} to {@link MPCall}. */
    static class MPCallAdapter<T> implements MPCall<T> {
        private final Call<T> call;

        MPCallAdapter(Call<T> call) {
            this.call = call;
        }

        @Override public void cancel() {
            call.cancel();
        }

        @Override public void enqueue(final Callback<T> callback) {
            call.enqueue(new retrofit2.Callback<T>() {
                @Override
                public void onResponse(Call<T> call, Response<T> response) {
                    final Response<T> r = response;
                    executeOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            int code = r.code();
                            if (code >= 200 && code < 300) {
                                callback.success(r.body());
                            } else {
                                callback.failure(ApiUtil.getApiException(r));
                            }
                        }
                    });
                }

                @Override
                public void onFailure(final Call<T> call, Throwable t) {
                    final Throwable th  = t;
                    if(callback.attempts++ == 3) {
                        executeOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.failure(ApiUtil.getApiException(th));
                            }
                        });
                    }
                    else {
                        call.clone().enqueue(this);
                    }
                }
            });
        }

        @Override public MPCall<T> clone() {
            return new MPCallAdapter<>(call.clone());
        }
    }

    private static void executeOnMainThread(@NonNull Runnable r) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(r);
    }
}
