package it.dd.spotytoasty;

import it.dd.spotytoasty.calls.SpotyWeb;
import it.dd.spotytoasty.configuration.ParseJSONConfig;
import it.dd.spotytoasty.server.SpotyToastyServer;
import it.dd.spotytoasty.toast.Toast;
import it.dd.spotytoasty.utils.Utils;

/**
 * Simple Spotify Toast
 * 
 * @author shamalaya
 *
 */
public class SpotyToasty {
	
	public static void main(String[] args) throws Exception {

		// parse parameter
		final String config_file = Utils.parserParameter(args);
		if( config_file == null ) 
			return;

		// parse configuration
		final ParseJSONConfig conf = new ParseJSONConfig( config_file );
		conf.parseConfiguration();

		// initialize spotyweb
		final SpotyWeb spotyweb = SpotyWeb.initialize( conf );

		// authorize spotyweb process
		if( conf.getConfiguration().getAuth().getAuth_refreshToken() == null ) {

			// server
			final SpotyToastyServer server = new SpotyToastyServer(conf.getConfiguration());

			// start the server
			server.serverStart();
	
			// get URI
			spotyweb.authorizationCodeUri();

			// waiting authorization	
			SpotyWeb.authorized.await();

			// stop server
			server.serverStop();

		} else
			spotyweb.authorizationCodeRefresh();

		// Create Toast
		final Toast toast = new Toast( conf.getConfiguration(), spotyweb );
		
		// Draw Toast
		toast.draw();
		
		// Start Worker
		toast.executeWorker();
	}
}