package com.source.iqueue.posthttp;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import static com.source.iqueue.posthttp.RequestConstantsValue.CONTENT_TYPE;
import static com.source.iqueue.posthttp.RequestConstantsValue.FCM_SERVER_KEY;

public interface FirebaseNotificationAPI {

    @Headers({"Authorization: key=", "Content-Type:application/json"})
    @POST("fcm/send")
    Call<ResponseBody> sendNotification(@Body PostRequestBody body);
}
