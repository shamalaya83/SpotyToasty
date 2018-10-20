# SpotyToasty ![alt text](https://github.com/dandag/SpotyToasty/blob/master/src/main/resources/img/spotytoasty.png "SpotyToasty")
Simple and Customizable Spotify Overlay

![alt text](https://github.com/dandag/SpotyToasty/blob/master/spotytoasty_sample.png "preview")

### Features:
* Display overlay while gaming (windowed fullscreen)
* Control Buttons: prev, play/pause, next, add/remove bookmark
* Customizable position, colors, border, font, controls, icons, opacity ecc.
* Two fetch "current song" mode: Pooling or Wait end
* Two view mode: "always on/off" or "autohide" 
* Mute ads (mute only spotify program)
* Small memory footprint
* Simple json configuration


### Steps to run SpotyToasty:

#### 0) Install Java JRE 8

#### 1) Download and unzip SpotyToasty-0.0.1.zip

#### 2) Go to Spotify Developer web page (https://developer.spotify.com/dashboard/) and register new application:

* Application name (Es. SpotyToasty)
* Website (Es. http://127.0.0.1:9999/)
* Redirect URIs (Es.http://127.0.0.1:9999/spotytoast-redirect)
* copy your Client ID and Client Secret

#### 3) Setup application in config.json:

```javascript
{
  "server": {
    "server_ip": "127.0.0.1",
    "server_port": 9999,
    "server_path": "spotytoast-redirect"
  },
  "auth": {
    "auth_clientId": "Your Client ID string",
    "auth_clientSecret": "Your Secret Key string",
  ...
```
  
#### 4) Execute SpotyToasty.bat

___

