HomeGenie-Android-ClientLib
===========================

Java/Android Client Library for [HomeGenie](https://github.com/genielabs/HomeGenie).

### Usage

To edit/build the library, open it in Android Studio. For building the library *jar* file, use the ```gradlew``` command from terminal:

```bash
./gradlew makeJar
```

the jar file will be generated in the ```homegenieclientlib\build\outputs``` folder.

After adding the ```homegenieclientlib``` as a dependency to your project, your application will be able to connect to *HomeGenie* as shown in the example below.

After the connection is estabilished the client application can access to *Groups and Modules* data structures, that are real-time updated through a [SSE stream](http://en.wikipedia.org/wiki/Server-sent_events).

In order to receive **SSE** events a class implementing ```EventSourceListener``` interface must be provided as the second parameter of the ```Control.connect``` method.

The ```com.glabs.homegenie.client.data.Module``` object extends ```Observable```, so another way to get notification when a module changes is implementing ```Observer```
interface and register using the method ```<module_of_interest>.addObserver(o)```.

### Examples

Estabilishing a connection to HG from a class implementing EventSourceListener :

        private Control hgControl = new Control();

        public void homegenieConnect() {
        
            hgControl.setServer(
                    "192.168.1.10",
                    "admin",
                    "thepassword",
                    false, // Use SSL?
                    false  // Accept all SSL certificates?
            );
            hgControl.setServerEventsListener(this);
            
            hgControl.connect(new Control.DataUpdatedCallback() {
                @Override
                public void onRequestSuccess() {
                    // Connection succesful, groups and modules updated
                    // use Control.getModules() and Control.getGroups()
                    // to get groups and modules list
                }
                @Override
                public void onRequestError(Control.ApiRequestResult apiRequestResult) {
                    // Connection error!
                    // apiRequestResult.StatusCode and apiRequestResult.ResponseBody properties
                    // hold the error data
                }
            }, this);
            
        }
        
        @Override
        public void onSseConnect() {
            // this is fired when the SSE connection is estabilished
        }

        @Override
        public void onSseEvent(Event event) {
            // this is fired every time a new event is received from the server
            
            Log.i("Event received:")
            Log.i("   " + event.Timestamp);
            Log.i("   " + event.Domain);
            Log.i("   " + event.Source);
            Log.i("   " + event.Description);
            Log.i("   " + event.Property);
            Log.i("   " + event.Value);
            
        }

        @Override
        public void onSseError(String error) {
            // this is fired if the SSE connection failed
        }        
     
Disconnecting from server:

    public void homegenieDisconnect() {
        hgControl.disconnect();
    }
    
Accessing Groups and Modules data structures:

    // get the list of all modules
    ArrayList<Module> modules = hgControl.getModules();
    
    // get the list of all groups
    ArrayList<Group> groups = hgControl.getGroups();
    
    // get modules of the group at index 0
    ArrayList<Module> groupModules = groups.get(0).Modules;
    
    // get a module using its domain and address
    Module module = hgControl.getModule("HomeAutomation.X10", "B3");
    
    // making a service API call
    hgControl.apiRequest("HomeAutomation.HomeGenie/Config/Groups.List", new ApiRequestCallback(){
        @Override
        public void onRequestSuccess(ApiRequestResult result) {
            // handle response here, 
            // result.ResponseBody hold the text of the response
        }
        @Override
        public void onRequestError(ApiRequestResult result) {
            // Request error!
            // apiRequestResult.StatusCode and apiRequestResult.ResponseBody properties
            // hold the error data
        }
    });


### Examples for library version <= 1.0.5

Estabilishing a connection to HG from a class implementing EventSourceListener :

        public void homegenieConnect() {
        
            Control.setServer(
                    "192.168.1.10",
                    "admin",
                    "thepassword",
                    false, // Use SSL?
                    false  // Accept all SSL certificates?
            );
            
            Control.connect(new Control.DataUpdatedCallback() {
                @Override
                public void onRequestSuccess() {
                    // Connection succesful, groups and modules updated
                    // use Control.getModules() and Control.getGroups()
                    // to get groups and modules list
                }
                @Override
                public void onRequestError(Control.ApiRequestResult apiRequestResult) {
                    // Connection error!
                    // apiRequestResult.StatusCode and apiRequestResult.ResponseBody properties
                    // hold the error data
                }
            }, this);
            
        }
        
        @Override
        public void onSseConnect() {
            // this is fired when the SSE connection is estabilished
        }

        @Override
        public void onSseEvent(Event event) {
            // this is fired every time a new event is received from the server
            
            Log.i("Event received:")
            Log.i("   " + event.Timestamp);
            Log.i("   " + event.Domain);
            Log.i("   " + event.Source);
            Log.i("   " + event.Description);
            Log.i("   " + event.Property);
            Log.i("   " + event.Value);
            
        }

        @Override
        public void onSseError(String error) {
            // this is fired if the SSE connection failed
        }        
     
Disconnecting from server:

    public void homegenieDisconnect() {
        Control.disconnect();
    }
    
Accessing Groups and Modules data structures:

    // get the list of all modules
    ArrayList<Module> modules = Control.getModules();
    
    // get the list of all groups
    ArrayList<Group> groups = Control.getGroups();
    
    // get modules of the group at index 0
    ArrayList<Module> groupModules = groups.get(0).Modules;
    
    // get a module using its domain and address
    Module module = Control.getModule("HomeAutomation.X10", "B3");
    
    // making a service API call
    Control.apiRequest("HomeAutomation.HomeGenie/Config/Groups.List", new ApiRequestCallback(){
        @Override
        public void onRequestSuccess(ApiRequestResult result) {
            // handle response here, 
            // result.ResponseBody hold the text of the response
        }
        @Override
        public void onRequestError(ApiRequestResult result) {
            // Request error!
            // apiRequestResult.StatusCode and apiRequestResult.ResponseBody properties
            // hold the error data
        }
    });

For further informations about data fields and methods see:

https://github.com/genielabs/HomeGenie-Android-ClientLib/tree/master/homegenieclientlib/src/main/java/com/glabs/homegenie/client/data

For a working example app using this library see:

https://github.com/genielabs/HomeGenie-Android







