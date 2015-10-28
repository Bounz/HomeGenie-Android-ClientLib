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

package com.glabs.homegenie.client;

import android.net.Uri;
import android.os.AsyncTask;

import com.glabs.homegenie.client.data.Event;
import com.glabs.homegenie.client.data.Group;
import com.glabs.homegenie.client.data.Module;
import com.glabs.homegenie.client.data.ModuleParameter;
import com.glabs.homegenie.client.eventsource.EventSourceListener;
import com.glabs.homegenie.client.eventsource.EventSourceTask;
import com.glabs.homegenie.client.httprequest.HttpRequest;
import com.glabs.homegenie.client.httprequest.HttpRequest.HttpRequestException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;

public class Control {

    public interface ApiRequestCallback {
        void onRequestSuccess(ApiRequestResult result);
        void onRequestError(ApiRequestResult result);
    }

    public interface GroupsRequestCallback {
        void onRequestSuccess(ArrayList<Group> groups);
        void onRequestError(ApiRequestResult result);
    }

    public interface GroupModulesRequestCallback {
        void onRequestSuccess(ArrayList<Module> modules);
        void onRequestError(ApiRequestResult result);
    }

    public interface DataUpdatedCallback {
        void onRequestSuccess();
        void onRequestError(ApiRequestResult result);
    }

    public static class ApiRequestResult
    {
        public boolean Success;
        public int StatusCode;
        public String ResponseBody;
    }


    private static String _hg_address;
    private static String _hg_user;
    private static String _hg_pass;
    private static boolean _hg_ssl;
    private static boolean _hg_acceptAll;

    private static String _protocol = "http://";
    private static int _requestTimeout = 15000;

    private static ArrayList<Module> _modules;
    private static ArrayList<Group> _groups;

    private static EventSourceListener _listener;
    private static EventSourceTask _sseTask;
    private static boolean _isPaused;
    //private static Semaphore _connectSemaphore = new Semaphore(1, true);
    private static boolean _disableAutoreconnect;

    public static boolean enableDebug = false;

    public static void setServer(String ip, String user, String pass, boolean ssl, boolean acceptAll) {
        _hg_address = ip;
        _hg_user = user;
        _hg_pass = pass;
        _hg_ssl = ssl;
        _hg_acceptAll = acceptAll;
        if (_hg_ssl)
            _protocol = "https://";
        else
            _protocol = "http://";
    }

    public static void setServerEventsListener(EventSourceListener listener) {
        _listener = listener;
    }

    public static void setTimeout(int millis) {
        _requestTimeout = millis;
    }
    public static void setDisableAutoreconnect(boolean disable) {
        _disableAutoreconnect = disable;
    }

    public static void connect(final DataUpdatedCallback callback) throws InterruptedException {
        disconnect();
        debug("[Control] connect() begin");
        //debug("[Control] connect() waiting semaphore");
        //_connectSemaphore.acquire();
        //debug("[Control] connect() semaphore acquired");
        updateData(new DataUpdatedCallback() {
            @Override
            public void onRequestSuccess() {
                debug("[Control] connect() onRequestSuccess");
                if (!_isPaused)
                    _sseTask = new EventSourceTask(getHgBaseHttpAddress() + "api/HomeAutomation.HomeGenie/Logging/RealTime.EventStream/", _disableAutoreconnect);
                //_connectSemaphore.release();
                //debug("[Control] connect() semaphore released");
                callback.onRequestSuccess();
            }

            @Override
            public void onRequestError(ApiRequestResult result) {
                debug("[Control] connect() onRequestError");
                //_connectSemaphore.release();
                //debug("[Control] connect() semaphore released");
                callback.onRequestError(result);
            }
        });
        debug("[Control] connect() end");
    }

    public static void connect(final DataUpdatedCallback callback, EventSourceListener listener) throws InterruptedException {
        connect(new DataUpdatedCallback() {
            @Override
            public void onRequestSuccess() {
                callback.onRequestSuccess();
            }

            @Override
            public void onRequestError(ApiRequestResult result) {
                callback.onRequestError(result);
            }
        });
        _listener = listener;
    }

    public static void resume(final DataUpdatedCallback callback) throws InterruptedException {
        debug("[Control] resume() begin");
        pause();
        _isPaused = false;
        //debug("[Control] resume() waiting semaphore");
        //_connectSemaphore.acquire();
        //debug("[Control] resume() semaphore acquired");
        debug("[Control] resume() getGroupModules");
        Control.getGroupModules("", new GroupModulesRequestCallback() {
            @Override
            public void onRequestSuccess(ArrayList<Module> modules) {
                debug("[Control] resume() getGroupModules -> onRequestSuccess");
                // update modules
                if (modules != null) {
                    if (_modules == null)
                        _modules = new ArrayList<Module>();
                    for (Module m : modules) {
                        Module cm = getModule(m.Domain, m.Address);
                        if (cm != null) {
                            cm.Name = m.Name;
                            for (ModuleParameter p : m.Properties) {
                                ModuleParameter cp = cm.getParameter(p.Name);
                                if (cp == null || !cp.UpdateTime.equals(p.UpdateTime))
                                    cm.setParameter(p.Name, p.Value, p.UpdateTime);
                            }
                        } else {
                            _modules.add(m);
                        }
                    }
                }
                if (!_isPaused)
                    _sseTask = new EventSourceTask(getHgBaseHttpAddress() + "api/HomeAutomation.HomeGenie/Logging/RealTime.EventStream/", _disableAutoreconnect);
                //_connectSemaphore.release();
                //debug("[Control] resume() getGroupModules semaphore released");
                callback.onRequestSuccess();
            }

            @Override
            public void onRequestError(ApiRequestResult result) {
                debug("[Control] resume() getGroupModules -> onRequestError");
                //_connectSemaphore.release();
                //debug("[Control] resume() getGroupModules semaphore released");
                callback.onRequestError(result);
            }
        });
        debug("[Control] resume() end");
    }

    public static void pause() throws InterruptedException {
        _isPaused = true;
        debug("[Control] pause() begin");
        if (_sseTask != null) {
            _sseTask.stop();
            //_sseTask.cancel(true);
            _sseTask = null;
        }
        debug("[Control] pause() end");
    }
    
    public static void disconnect() throws InterruptedException {
        debug("[Control] disconnect() begin");
        //debug("[Control] disconnect() waiting semaphore");
        //_connectSemaphore.acquire();
        //debug("[Control] disconnect() semaphore acquired");
        _listener = null;
        if (_sseTask != null) {
            _sseTask.stop();
            //_sseTask.cancel(true);
            _sseTask = null;
        }
        if (_groups != null) {
            _groups.clear();
            _groups = null;
        }
        if (_modules != null) {
            _modules.clear();
            _modules = null;
        }
        //_connectSemaphore.release();
        //debug("[Control] resume() getGroupModules semaphore released");
        debug("[Control] disconnect() end");
    }

    public static String getAuthUser() {
        return _hg_user;
    }

    public static String getAuthPassword() {
        return _hg_pass;
    }

    public static boolean getSSL() {
        return _hg_ssl;
    }

    public static boolean getAcceptAll() {
        return _hg_acceptAll;
    }

    public static String getHgBaseHttpAddress() {
        return _protocol + _hg_address + "/";
    }

    public static ArrayList<Module> getModules() {
        return _modules;
    }

    public static ArrayList<Group> getGroups() {
        return _groups;
    }

    public static Module getModule(String domain, String address) {
        return getModule(_modules, domain, address);
    }

    public static Module getModule(ArrayList<Module> modules, String domain, String address) {
        Module module = null;
        if (modules != null)
        for(Module m : modules) {
            if (m.Domain.equals(domain) && m.Address.equals(address)) {
                module = m;
                break;
            }
        }
        return module;
    }
    
    public static void updateData(final DataUpdatedCallback callback) {
        // get complete list of modules
        Control.getGroupModules("", new GroupModulesRequestCallback() {
            @Override
            public void onRequestSuccess(ArrayList<Module> modules) {
                _modules = modules;
                // get groups list
                updateGroups(new GroupsRequestCallback() {
                    @Override
                    public void onRequestSuccess(ArrayList<Group> groups) {
                        callback.onRequestSuccess();
                    }

                    @Override
                    public void onRequestError(ApiRequestResult result) {
                        callback.onRequestError(result);
                    }
                });
            }

            @Override
            public void onRequestError(ApiRequestResult result) {
                callback.onRequestError(result);
            }
        });
    }

    public static void updateGroups(final GroupsRequestCallback callback) {
        Control.getGroups(new GroupsRequestCallback() {
            @Override
            public void onRequestSuccess(ArrayList<Group> groups) {
                if (groups.size() > 0) {
                    _groups = groups;
                    // link groups modules
                    for (Group g : _groups) {
                        for (int m = 0; m < g.Modules.size(); m++) {
                            String domain = g.Modules.get(m).Domain;
                            String address = g.Modules.get(m).Address;
                            if (domain.equals("HomeGenie.UI.Separator")) {
                                // UI.Separator is not a real module, it is
                                // a fake module used by the UI
                                Module gm = g.Modules.get(m);
                                gm.Name = address;
                                gm.DeviceType = "";
                                gm.Description = "UI Separator";
                                gm.Properties = new ArrayList<ModuleParameter>();
                            } else {
                                Module module = getModule(domain, address);
                                if (module == null)
                                    g.Modules.remove(m);
                                else
                                    g.Modules.set(m, module);
                            }
                        }
                    }
                    callback.onRequestSuccess(_groups);
                } else {
                    callback.onRequestError(new ApiRequestResult());
                }
            }
            @Override
            public void onRequestError(ApiRequestResult result) {
                callback.onRequestError(result);
            }
        });
    }

    public static void getGroups(GroupsRequestCallback callback) {
        new GetGroupsRequest(callback).execute();
    }

    public static void getGroupModules(String group, GroupModulesRequestCallback callback) {
        new GetGroupModulesRequest(group, callback).execute();

    }

    public static void apiRequest(String servicecall, ApiRequestCallback callback) {
        new ApiRequest(servicecall, callback).execute();
    }

    public static HttpRequest getHttpGetRequest(String url) {
        HttpRequest request = HttpRequest.get(url);
        // Set the timeout in milliseconds until a connection is established.
        // The default value is zero, that means the timeout is not used.
        int timeoutConnection = _requestTimeout;
        request.connectTimeout(timeoutConnection);
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = _requestTimeout;
        request.readTimeout(timeoutSocket);
        if (!_hg_user.equals("") && !_hg_pass.equals(""))
            request.basic(_hg_user, _hg_pass);
        if (_hg_acceptAll && _hg_ssl) {
            request.trustAllCerts();
            request.trustAllHosts();
        }
        return request;
    }

    // TODO: move this to the Utility class
    public static String getUpnpDisplayName(Module m) {
        String desc = m.getDisplayAddress();
        if (m.getParameter("UPnP.ModelDescription") != null && !m.getParameter("UPnP.ModelDescription").Value.trim().equals("")) {
            desc = m.getParameter("UPnP.ModelDescription").Value;
        } else if (m.getParameter("UPnP.ModelName") != null && !m.getParameter("UPnP.ModelName").Value.trim().equals("")) {
            desc = m.getParameter("UPnP.ModelName").Value;
        }
        return desc;
    }

    public static class ApiRequest extends AsyncTask<String, Boolean, ApiRequestResult> {

        private String serviceUrl;
        //
        private ApiRequestCallback callback;

        public ApiRequest(String servicecall, ApiRequestCallback callback) {
            this.serviceUrl = _protocol + _hg_address + "/api/" + servicecall;
            this.callback = callback;
        }

        @Override
        protected ApiRequestResult doInBackground(String... params) {
            ApiRequestResult result = new ApiRequestResult();
            //execute the request
            try {
                HttpRequest request = getHttpGetRequest(serviceUrl);
                result.ResponseBody = request.body();
                result.StatusCode = request.code();
                result.Success = (request.code() < 400);
                return result;
            } catch (HttpRequestException e) {
                //Log.e("AsyncOperationFailed", e.getMessage());
                result.ResponseBody = e.getMessage();
                e.printStackTrace();
            }
            return result;
        }

        protected void onPostExecute(ApiRequestResult result) {
            if (callback != null) {
                if (result.Success)
                    callback.onRequestSuccess(result);
                else
                    callback.onRequestError(result);
            }
        }
    }

    public static class GetGroupsRequest extends AsyncTask<String, Boolean, ApiRequestResult> {

        private String serviceUrl;
        //
        private GroupsRequestCallback callback;

        public GetGroupsRequest(GroupsRequestCallback callback) {
            this.serviceUrl = _protocol + _hg_address + "/api/HomeAutomation.HomeGenie/Config/Groups.List/";
            this.callback = callback;
        }

        @Override
        protected ApiRequestResult doInBackground(String... params) {
            ApiRequestResult result = new ApiRequestResult();
            //execute the request
            try {
                HttpRequest request = getHttpGetRequest(serviceUrl);
                result.ResponseBody = request.body();
                result.StatusCode = request.code();
                result.Success = (request.code() < 400);
                return result;
            } catch (HttpRequestException e) {
                //Log.e("AsyncOperationFailed", e.getMessage());
                result.ResponseBody = e.getMessage();
                e.printStackTrace();
            }
            return result;
        }

        protected void onPostExecute(ApiRequestResult result) {

            if (result.Success) {
                String jsonString = result.ResponseBody;
                ArrayList<Group> groups = new ArrayList<Group>();
                try {
                    JSONArray jGroups = new JSONArray(jsonString);
                    for (int g = 0; g < jGroups.length(); g++) {
                        JSONObject jg = (JSONObject) jGroups.get(g);
                        Group group = new Group();
                        group.Name = jg.getString("Name");
                        JSONArray jgmodules = jg.getJSONArray("Modules");
                        for (int m = 0; m < jgmodules.length(); m++) {
                            JSONObject jmp = (JSONObject) jgmodules.get(m);
                            Module mod = new Module();
                            mod.Domain = jmp.getString("Domain");
                            mod.Address = jmp.getString("Address");
                            group.Modules.add(mod);
                        }
                        groups.add(group);
                    }
                    if (callback != null)
                        callback.onRequestSuccess(groups);
                } catch (JSONException e) {
                    e.printStackTrace();
                    if (callback != null)
                        callback.onRequestError(new ApiRequestResult());
                }
            } else {
                callback.onRequestError(result);
            }
        }
    }


    public static class GetGroupModulesRequest extends AsyncTask<String, Boolean, ApiRequestResult> {

        private String serviceUrl;
        //
        private GroupModulesRequestCallback callback;

        public GetGroupModulesRequest(String groupName, GroupModulesRequestCallback callback) {
            if (groupName.equals("")) {
                this.serviceUrl = _protocol + _hg_address + "/api/HomeAutomation.HomeGenie/Config/Modules.List/";
            } else {
                this.serviceUrl = _protocol + _hg_address + "/api/HomeAutomation.HomeGenie/Config/Groups.ModulesList/" + Uri.encode(groupName);
            }
            this.callback = callback;
        }

        @Override
        protected ApiRequestResult doInBackground(String... params) {
            ApiRequestResult result = new ApiRequestResult();
            //execute the request
            try {
                HttpRequest request = getHttpGetRequest(serviceUrl);
                result.ResponseBody = request.body();
                result.StatusCode = request.code();
                result.Success = (request.code() < 400);
                return result;
            } catch (HttpRequestException e) {
                //Log.e("AsyncOperationFailed", e.getMessage());
                result.ResponseBody = e.getMessage();
                e.printStackTrace();
            }
            return result;
        }

        protected void onPostExecute(ApiRequestResult result) {
            if (result.Success) {
                String jsonString = result.ResponseBody;

                if (jsonString == null || jsonString.equals("")) return;

                ArrayList<Module> moduleList = new ArrayList<Module>();
                try {
                    JSONArray groupModules = new JSONArray(jsonString);
                    for (int m = 0; m < groupModules.length(); m++) {
                        JSONObject jm = (JSONObject) groupModules.get(m);
                        Module module = new Module();
                        module.Domain = jm.getString("Domain");
                        module.Address = jm.getString("Address");
                        module.DeviceType = jm.getString("DeviceType");
                        module.Name = jm.getString("Name");
                        module.Description = jm.getString("Description");
                        module.RoutingNode = jm.getString("RoutingNode");
                        //
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                        //
                        JSONArray jmProperties = jm.getJSONArray("Properties");
                        for (int p = 0; p < jmProperties.length(); p++) {
                            JSONObject jmp = (JSONObject) jmProperties.get(p);
                            ModuleParameter param = new ModuleParameter(jmp.getString("Name"), jmp.getString("Value"));
                            param.Description = jmp.getString("Description");
                            try {
                                param.UpdateTime = dateFormat.parse(jmp.getString("UpdateTime"));
                            } catch (Exception e) {
                            }
                            module.Properties.add(param);
                        }
                        moduleList.add(module);
                    }
                    if (callback != null)
                        callback.onRequestSuccess(moduleList);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    if (callback != null)
                        callback.onRequestError(new ApiRequestResult());
                }
            } else {
                callback.onRequestError(result);
            }
        }
    }


    //
    // Server Sent Events Handling
    //
    public static void onSseConnect() {
        if (_listener != null) {
            _listener.onSseConnect();
        }
    }

    public static void onSseEvent(Event event) {

        Module module = getModule(event.Domain, event.Source);
        if (module != null) {
            module.setParameter(event.Property, event.Value, event.Timestamp);
        }

        if (_listener != null) {
            _listener.onSseEvent(module, event);
        }
    }

    public static void onSseError(String error) {
        if (_listener != null) {
            _listener.onSseError(error);
        }
    }

    public static void debug(String s) {
        if (enableDebug) {
            System.out.println(s);
        }
    }
}
