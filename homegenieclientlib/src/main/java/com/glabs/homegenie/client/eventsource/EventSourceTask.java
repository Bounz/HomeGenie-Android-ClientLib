/*
    This file is part of HomeGenie for Android.

    HomeGenie for Android is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HomeGenie for Android is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with HomeGenie for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 *     Author: Generoso Martello <gene@homegenie.it>
 */

package com.glabs.homegenie.client.eventsource;

import android.os.AsyncTask;
import android.util.Log;

import com.glabs.homegenie.client.Control;
import com.glabs.homegenie.client.data.Event;

import org.json.JSONException;
import org.json.JSONObject;
import org.threemusketeers.eventsource.EventSource;
import org.threemusketeers.eventsource.EventSourceNotification;
import org.threemusketeers.eventsource.Message;

import java.util.Date;

import io.netty.channel.nio.NioEventLoopGroup;


/**
 * Created by Gene on 29/04/14.
 */
public class EventSourceTask extends AsyncTask<String, Void, Void> implements EventSourceNotification {

    private Exception exception;
    private EventSource eventSource;

    public EventSourceTask()
    {
    }

    public void stop()
    {
        if (eventSource != null)
        {
            eventSource.close();
            eventSource = null;
        }
    }

    protected Void doInBackground(String... urls) {
        try {
            eventSource = new EventSource(urls[0], Control.getAuthUser(), Control.getAuthPassword(), new NioEventLoopGroup(), this);
        } catch (Exception e) {
            this.exception = e;
        }
        return null;
    }

    protected void onPostExecute() {
        // TODO: check this.exception
    }

    @Override
    public void onOpen() {
        Control.onSseConnect();
    }

    @Override
    public void onMessage(Message message) {
        Log.d("EventSource.Message", message.data);
        //
        try {
            JSONObject jevent = new JSONObject(message.data);
            final Event event = new Event();
            event.UnixTimestamp = jevent.getDouble("UnixTimestamp");
            event.Timestamp = new Date(event.UnixTimestamp.longValue());
            event.Domain = jevent.getString("Domain");
            event.Source = jevent.getString("Source");
            event.Description = jevent.getString("Description");
            event.Property = jevent.getString("Property");
            event.Value = jevent.getString("Value");
            Control.onSseEvent(event);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(String error) {
        Control.onSseError(error);
    }
}
