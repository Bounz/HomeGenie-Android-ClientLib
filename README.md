HomeGenie-Android-ClientLib
===========================

Android Client Library for HomeGenie Service


It enables a client application to connect a *HomeGenie* server.

After the connection is estabilished the client can access to *Groups and Modules* data structures that are real-time updated via *Server Sent Events* (http://en.wikipedia.org/wiki/Server-sent_events).

For receiving real-time events from HG server a class implementing **EventSourceListener** interface must be provided as the second parameter of the **Control.connect** method.

###Examples

Estabilishing a connection to HG from a class implementing EventSourceListener :

        public void homegenieConnect() {
        
            Control.setServer(
                    "192.168.1.10",
                    "admin",
                    "thepassword"
            );
            
            Control.connect(new Control.UpdateGroupsAndModulesCallback() {  // <-- This is a very looong callback name, isn't it? =)
                @Override
                public void groupsAndModulesUpdated(boolean success) {
                    if (success) {
                    
                       // Connection succesful
                    
                    } else {
                    
                       // Connection failed
                    
                    }
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
    Control.callServiceApi("HomeAutomation.HomeGenie/Config/Groups.List", new ServiceCallCallback(){
        @Override
        public void serviceCallCompleted(String response)
        {
            // handle response here...
        }
    });

For further informations about data fields and methods see:

https://github.com/genielabs/HomeGenie-Android-ClientLib/tree/master/src/com/glabs/homegenie/client/data

For a working example app using this library see:

https://github.com/genielabs/HomeGenie-Android







