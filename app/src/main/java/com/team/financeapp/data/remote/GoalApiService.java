package com.team.financeapp.data.remote;

import com.team.financeapp.data.local.entity.GoalEntity;

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

public interface GoalApiService {

    @GET("goals")
    Call<List<GoalEntity>> getGoals();

    @POST("goals")
    Call<GoalEntity> createGoal(@Body GoalEntity goal);

    @PUT("goals/{id}")
    Call<GoalEntity> updateGoal(@Path("id") int id, @Body GoalEntity goal);

    @PATCH("goals/{id}/savings")
    Call<GoalEntity> addSavings(@Path("id") int id, @Body Map<String, Object> body);

    @DELETE("goals/{id}")
    Call<Void> deleteGoal(@Path("id") int id);
}
