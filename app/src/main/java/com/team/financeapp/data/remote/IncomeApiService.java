package com.team.financeapp.data.remote;

import com.team.financeapp.data.local.entity.IncomeEntity;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface IncomeApiService {

    @GET("incomes")
    Call<List<IncomeEntity>> getIncomes();

    @POST("incomes")
    Call<IncomeEntity> createIncome(@Body IncomeEntity income);

    @PUT("incomes/{id}")
    Call<IncomeEntity> updateIncome(@Path("id") int id, @Body IncomeEntity income);

    @DELETE("incomes/{id}")
    Call<Void> deleteIncome(@Path("id") int id);
}
