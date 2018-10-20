package it.dd.spotytoasty.configuration;

import lombok.Data;

@Data
public class Server {
	private String server_ip;
	private int server_port;
	private String server_path;
}
