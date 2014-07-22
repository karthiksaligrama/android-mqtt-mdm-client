android-mdm-client
==================

## Introduction

This is a sample app which demostrates the MDM app without using google cloud messaging framework. The app instead uses MQTT to push the data to the device which then interprets the message and implements the poilcy.

This app is purely to study the functionality of MQTT. 

## Setup

To Setup this app you will need a mqtt based server. I Built this project using mosquitto (http://mosquitto.org/), so you can use that. I'll also update the sample project on for sending the message from the server.

To set up the client you can just import it as an android project in eclipse and run the app once the server is up. 

In splash activity update the "QUEUE_END_POINT" variable to point to your hostname or the ip address. This version of the app doesnt use the secure MQTT ports and uses the standard 1883 port for communication. If you are using any other ports please update the "brokerPortNumber" variable in the MQTTService Class.

The following commands are supported as of this moment
```
ADMIN_COMMAND_LOCK_DEVICE = "lock";
ADMIN_COMMAND_REMOTE_WIPE = "remote_wipe";
ADMIN_RESET_PASSWORD = "reset_password";
ADMIN_DISABLE_CAMERA = "disable_camera";
ADMIN_ENABLE_CAMERA = "enable_camera";
ADMIN_ENABLE_ENCRYPTION = "enable_encryption";
ADMIN_DISABLE_ENCRYTION = "disable_encrytion";
ADMIN_ENABLE_PASSWORD_POLICY = "password_policy";
```

send the data in the format from the server
```
{
  "command":"",
  "message":"",
  "password":"",
  "policy":{
    <as per DevicePolicy.java>
  }
}
```

