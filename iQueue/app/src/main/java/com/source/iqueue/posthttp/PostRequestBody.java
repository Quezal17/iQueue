package com.source.iqueue.posthttp;

import com.google.gson.annotations.SerializedName;

public class PostRequestBody {
    @SerializedName("to")
    private String to;
    @SerializedName("notification")
    private PostRequestNotification notification;

    public PostRequestBody(String to, PostRequestNotification notification) {
        this.to = to;
        this.notification = notification;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public PostRequestNotification getNotification() {
        return notification;
    }

    public void setNotification(PostRequestNotification notification) {
        this.notification = notification;
    }
}
