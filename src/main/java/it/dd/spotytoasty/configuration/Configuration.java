package it.dd.spotytoasty.configuration;

import lombok.Data;

@Data
public class Configuration {

	// server port
	private Server server;
	
	// authorization
	private Auth auth;
	
	// overlay
	private Overlay overlay;
	
	// image
	private Cover cover;
	
	// controls
	private Controls controls;
	
	// info
	private Info artist;
	private Info song;
	private Info album;
	
	// automute
	private boolean automute;
	
	// autohide
	private Autohide autohide;
}