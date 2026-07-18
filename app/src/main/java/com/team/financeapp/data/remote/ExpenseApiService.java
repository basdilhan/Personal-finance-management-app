package com.team.financeapp.data.remote;

import com.team.financeapp.data.local.entity.ExpenseEntity;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ExpenseApiService {

    @GET("expenses")
    Call<List<ExpenseEntity>> getExpenses();

    @POST("expenses")
    Call<ExpenseEntity> createExpense(@Body ExpenseEntity expense);

    @PUT("expenses/{id}")
    Call<ExpenseEntity> updateExpense(@Path("id") int id, @Body ExpenseEntity expense);

    @DELETE("expenses/{id}")
    Call<Void> deleteExpense(@Path("id") int id);

    @POST("expenses/categorize")
    Call<java.util.Map<String, String>> categorizeExpense(@Body java.util.Map<String, String> request);
}
