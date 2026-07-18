package com.team.financeapp.data.remote;

import com.team.financeapp.data.local.entity.BudgetLimitEntity;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BudgetApiService {

    @GET("budgets")
    Call<List<BudgetLimitEntity>> getBudgets(@Query("monthYear") String monthYear);

    @POST("budgets")
    Call<BudgetLimitEntity> createOrUpdateBudget(@Body BudgetLimitEntity budget);

    @DELETE("budgets/{id}")
    Call<Void> deleteBudget(@Path("id") int id);
}
