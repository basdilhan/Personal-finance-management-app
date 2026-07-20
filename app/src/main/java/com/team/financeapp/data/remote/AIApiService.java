package com.team.financeapp.data.remote;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AIApiService {
    @POST("ai/scan-receipt")
    Call<Map<String, Object>> scanReceipt(@Body Map<String, String> request);
}
