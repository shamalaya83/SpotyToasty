package it.dd.spotytoasty.configuration;

import lombok.Data;

@Data
public class Auth {
	private String auth_clientId;
	private String auth_clientSecret;
	private String auth_refreshToken;
}
