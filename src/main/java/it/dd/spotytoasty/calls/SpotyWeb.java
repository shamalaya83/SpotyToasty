package it.dd.spotytoasty.calls;

import it.dd.spotytoasty.configuration.ParseJSONConfig;
import lombok.extern.log4j.Log4j;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.library.RemoveUsersSavedTracksRequest;
import com.wrapper.spotify.requests.data.library.SaveTracksForUserRequest;
import com.wrapper.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;

/**
 * 
 * Talk with Spotify using the officla API
 * 
 * @author shamalaya
 *
 */
@Log4j
public class SpotyWeb {

	/**
	 * Authorization sync between threads 
	 */
	public static final CountDownLatch authorized = new CountDownLatch(1); 	

	/**
	 * Keep track of expire time
	 */
	private long expires = 0L;

	/**
	 * Spotify Application secrets and redirect url
	 */
	private String clientId;
	private String clientSecret;
	private URI redirectUri;

	/**
	 * SpotifyApi 
	 */
	private SpotifyApi spotifyApi;

	/**
	 * Configuration
	 */
	private ParseJSONConfig conf;

	/**
	 * Single instance
	 */
	private static SpotyWeb spotyweb;

	/**
	 * Spotify Scopers
	 */
	private final static String SCOPES = 	
		"user-read-currently-playing, " +
		"user-read-playback-state, " +
		"user-library-modify";

	// INITIALIZE
	private SpotyWeb(ParseJSONConfig conf) {

		// set configuration
		this.conf = conf;

		// set secrets
		clientId = conf.getConfiguration().getAuth().getAuth_clientId();
		clientSecret = conf.getConfiguration().getAuth().getAuth_clientSecret();
		redirectUri = SpotifyHttpManager.makeUri( 	"http://" + conf.getConfiguration().getServer().getServer_ip() + ":" + 
				conf.getConfiguration().getServer().getServer_port() + "/" + 
				conf.getConfiguration().getServer().getServer_path() );

		// set api
		spotifyApi = new SpotifyApi.Builder()
				.setClientId(clientId)
				.setClientSecret(clientSecret)
				.setRedirectUri(redirectUri)
				.build();		
	}

	public static final SpotyWeb initialize(ParseJSONConfig conf) {
		if( spotyweb==null ) {
			spotyweb = new SpotyWeb(conf);
		}

		return spotyweb;
	}

	public static final SpotyWeb getInstance() {
		return spotyweb;
	}

	// GET URI
	public void authorizationCodeUri() throws Exception {
		final AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
				.state("x4xkmn9pu3j6ukrs8n")
				.scope( SCOPES )
				.show_dialog(true)
				.build();

		final URI uri = authorizationCodeUriRequest.execute();

		if (Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(new URI(uri.toString()));
		} else {
			log.info("Please manualy open this url: " + uri.toString());
		}
	}

	// AUTHORIZE
	public void authorizationCode(String code) throws SpotifyWebApiException, IOException {

		final AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
		final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();

		// Set access and refresh token for further "spotifyApi" object usage
		spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
		spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
		log.info("Token expires in: " + authorizationCodeCredentials.getExpiresIn());
		log.info("Refresh Token: " + authorizationCodeCredentials.getRefreshToken());

		// set expire time
		expires = System.currentTimeMillis() + ( authorizationCodeCredentials.getExpiresIn() * 1000 );

		// save refresh token in configuration
		if( authorizationCodeCredentials.getRefreshToken() != null ) {
			conf.getConfiguration().getAuth().setAuth_refreshToken( authorizationCodeCredentials.getRefreshToken() );
			conf.writeConfiguration();
		}
	}

	// REFRESH
	public void authorizationCodeRefresh() throws JsonGenerationException, JsonMappingException, IOException, SpotifyWebApiException {

		// reload refresh token
		if( spotifyApi.getRefreshToken() == null ) {
			spotifyApi.setRefreshToken(conf.getConfiguration().getAuth().getAuth_refreshToken());
		}

		final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh().build();
		final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

		spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
		spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
		log.info("Renew Expires in: " + authorizationCodeCredentials.getExpiresIn());
		log.info("Renew Refresh Token: " + authorizationCodeCredentials.getRefreshToken());

		// set expire time
		expires = System.currentTimeMillis() + ( authorizationCodeCredentials.getExpiresIn() * 1000 );

		// save refresh token in configuration
		if( authorizationCodeCredentials.getRefreshToken() != null ) {
			conf.getConfiguration().getAuth().setAuth_refreshToken( authorizationCodeCredentials.getRefreshToken() );
			conf.writeConfiguration();
		}
	}

	// CHECK EXPIRED
	private void checkExpired() throws JsonGenerationException, JsonMappingException, SpotifyWebApiException, IOException {
		if( System.currentTimeMillis() >= expires ) {
			authorizationCodeRefresh();
		}
	}

	/****************
	 * ACTION CALLS *
	 ****************/
	/**
	 * Current song
	 * 
	 * @return
	 * @throws Exception
	 * @throws SpotifyWebApiException
	 */
	public CurrentlyPlayingContext getInformationAboutUsersCurrentPlayback() throws Exception, SpotifyWebApiException {
		long start = System.currentTimeMillis();

		checkExpired();

		GetInformationAboutUsersCurrentPlaybackRequest getInformationAboutUsersCurrentPlaybackRequest =
				spotifyApi.getInformationAboutUsersCurrentPlayback()
				.market(CountryCode.IT)
				.build();
		CurrentlyPlayingContext ris = getInformationAboutUsersCurrentPlaybackRequest.execute();

		log.info("getInformationAboutUsersCurrentPlayback(): info retrieved in " + (System.currentTimeMillis()-start) + " ms" );
		return ris;
	}

	/**
	 * Save track in bookmark
	 * 
	 * @param id
	 * @throws SpotifyWebApiException
	 * @throws IOException
	 */
	public void saveTracksForUser(String id) throws SpotifyWebApiException, IOException {		  

		checkExpired();

		SaveTracksForUserRequest saveTracksForUserRequest = spotifyApi.saveTracksForUser( new String[] { id } ).build();
		String ris = saveTracksForUserRequest.execute();
		log.info("saveTracksForUser(): " + ris);
	}

	/**
	 * Remove track from bookmark
	 * 
	 * @param id
	 * @throws SpotifyWebApiException
	 * @throws IOException
	 */
	public void removeTracksForUser(String id) throws SpotifyWebApiException, IOException {		  

		checkExpired();

		RemoveUsersSavedTracksRequest saveTracksForUserRequest = spotifyApi.removeUsersSavedTracks( new String[] { id } ).build();
		String ris = saveTracksForUserRequest.execute();
		log.info("removeTracksForUser(): " + ris);
	}
}