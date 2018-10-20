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

#### 5) Allow Spotify app (only first time)
___

### Customize SpotyToasty

SpotyToasty can be customized using the config.json:

```javascript
{
  // server option for Spotify authorization process
	"server": {
		"server_ip": "127.0.0.1",
		"server_port": 9999,
		"server_path": "spotytoast-redirect"
	},
  // application keys to interacting with Spotify API
	"auth": {
		"auth_clientId": "your key",
		"auth_clientSecret": "your sec key",
		"auth_refreshToken": null
	},
  // main overlay section
	"overlay": {
		"show": true,                                   // Set always on or hide mode
		"refresh_ms": 1000,                             // default think time (es. in pooling mode)
		"pooling": false,                               // enable pooling mode
		"opacity": 0.8,                         
		"coords": "5,5",                                // x, y coordinate from left-high corner
		"max_width": 500,                               // set overlay max width, <0 for no limit
		"overlay_background": "#7d1896",                // external rectangle option
		"overlay_border": "5,5,5,5",
		"overlay_cover_background": "#272727",          // cover rectangle option
		"overlay_cover_border": "5,0,0,0",
		"overlay_control_background": "#272727",        // controls rectangle option
		"overlay_control_border":"0,0,0,0",
		"overlay_info_background": "#404040",           // info rectangle option
		"overlay_info_border": "5,10,5,10"
	},
  // cover size in px
	"cover": {
		"image_width": 70,
		"image_height": 70
	},
  // controls section
	"controls": {
		"show": true,                                   // show controls
		"icons_px": 16,                                 // incons px 
		"show_prev": true,                              // show/hide specific button
		"show_pp": true,
		"show_next": true,
		"show_love": true
	},
  // info section
	"artist": {
		"show": true,                                  // show artist info
		"position": "Center",                          // set position: North, Center or South
		"color": "#d3d3d3",                            // set color in HTML
		"font": "Verdana",                             // set font (must be already installed)
		"weight": 1,                                   // font weight: 0 normal, 1 bold, 2 cursive
		"size": 16                                     // font size
	},
	"song": {
		"show": true,                                  // show song info
		"position": "North",
		"color": "#FFFFFF",
		"font": "Verdana",
		"weight": 1,
		"size": 24
	},
	"album": {
		"show": true,                                  // show album info
		"position": "South",
		"color": "#d6d6d6",
		"font": "Verdana",
		"weight": 2,
		"size": 14
	},
	"automute": true,                                // mute spotify ads 
	// autohide section
  "autohide": {                   
		"autohide": true,                              // hide toast after ms
		"hide_after_ms": 8000                          
	}
}
```
