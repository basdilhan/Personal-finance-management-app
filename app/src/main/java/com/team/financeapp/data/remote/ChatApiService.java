package com.team.financeapp.data.remote;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ChatApiService {

    @POST("/api/chat")
    Call<Map<String, String>> sendMessage(
            @Header("X-User-Id") String userId,
            @Body Map<String, String> request
    );
}
