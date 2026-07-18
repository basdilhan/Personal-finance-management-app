package com.team.financeapp.data.remote;

import com.team.financeapp.data.local.entity.BillEntity;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface BillApiService {

    @GET("bills")
    Call<List<BillEntity>> getBills();

    @POST("bills")
    Call<BillEntity> createBill(@Body BillEntity bill);

    @PUT("bills/{id}")
    Call<BillEntity> updateBill(@Path("id") int id, @Body BillEntity bill);

    @PATCH("bills/{id}/status")
    Call<BillEntity> updateBillStatus(@Path("id") int id, @Body Map<String, String> statusMap);

    @DELETE("bills/{id}")
    Call<Void> deleteBill(@Path("id") int id);
}
