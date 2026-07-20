package com.team.financeapp.data.remote;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import okhttp3.Authenticator;
import okhttp3.Route;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.IOException;
import com.team.financeapp.BuildConfig;

public class ApiClient {

    // Base URL is now provided by BuildConfig
    private static Retrofit retrofit = null;
    private static Context appContext = null;
    
    public static final String ACTION_LOGOUT = "com.team.financeapp.ACTION_LOGOUT";

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(new AuthInterceptor())
                    .authenticator(new TokenAuthenticator())
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder();
            
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Pass Firebase UID in the header for the backend to read
                requestBuilder.header("X-User-Id", user.getUid());
                
                if (requestBuilder.build().header("Authorization") == null) {
                    try {
                        String token = com.google.android.gms.tasks.Tasks.await(user.getIdToken(false)).getToken();
                        if (token != null) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            
            Request request = requestBuilder.build();
            return chain.proceed(request);
        }
    }

    private static class TokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null || responseCount(response) >= 2) {
                if (appContext != null) {
                    LocalBroadcastManager.getInstance(appContext).sendBroadcast(new Intent(ACTION_LOGOUT));
                }
                return null;
            }

            try {
                // Force a token refresh
                String token = com.google.android.gms.tasks.Tasks.await(user.getIdToken(true)).getToken();
                if (token != null) {
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private int responseCount(Response response) {
            int result = 1;
            while ((response = response.priorResponse()) != null) {
                result++;
            }
            return result;
        }
    }
}