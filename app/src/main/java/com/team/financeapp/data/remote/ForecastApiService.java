package com.team.financeapp.data.remote;

import com.team.financeapp.data.remote.dto.MlForecastResponse;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ForecastApiService {
    @GET("forecasts/ml-predict")
    Call<MlForecastResponse> getMlForecast();
}
