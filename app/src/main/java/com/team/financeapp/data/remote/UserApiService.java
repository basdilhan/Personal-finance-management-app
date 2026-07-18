package com.team.financeapp.data.remote;

import com.team.financeapp.auth.UserProfile;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface UserApiService {

    @GET("users/me")
    Call<UserProfile> getCurrentUser();

    @POST("users")
    Call<UserProfile> createOrUpdateUser(@Body UserProfile user);
}
