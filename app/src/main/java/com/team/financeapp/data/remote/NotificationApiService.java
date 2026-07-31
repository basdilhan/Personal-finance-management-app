package com.team.financeapp.data.remote;

import com.team.financeapp.data.remote.dto.NotificationDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface NotificationApiService {

    @GET("api/notifications")
    Call<List<NotificationDto>> getNotifications();

    @GET("api/notifications/unread")
    Call<List<NotificationDto>> getUnreadNotifications();

    @PATCH("api/notifications/{id}/read")
    Call<NotificationDto> markAsRead(@Path("id") int id);

    @DELETE("api/notifications/{id}")
    Call<Void> deleteNotification(@Path("id") int id);
}
