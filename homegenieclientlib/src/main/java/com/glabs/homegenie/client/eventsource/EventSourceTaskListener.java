package com.glabs.homegenie.client.eventsource;

import com.glabs.homegenie.client.data.Event;
import com.glabs.homegenie.client.data.Module;

public interface EventSourceTaskListener {
    void onSseConnect();
    String getAuthUser();
    String getAuthPassword();
    boolean getSsl();
    boolean getSslAcceptAll();
    void onSseEvent(Event event);
    void onSseError(String error);
}
