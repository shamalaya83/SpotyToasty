package it.dd.spotytoasty.server;

import it.dd.spotytoasty.configuration.Configuration;
import lombok.extern.log4j.Log4j;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * Simple jetty server needed to retrieve code from Spotify authorization process
 * 
 * @author shamalaya
 *
 */
@Log4j
public class SpotyToastyServer {

	private Server server;
	private ServletHandler handler;
	private Configuration conf;

	public SpotyToastyServer(Configuration conf) {
		this.conf = conf;
	}

	/**
	 * Initialize and start the server
	 * 
	 * @throws Exception
	 */
	public void serverStart() {
		if( server==null ) {

			try {
				
				// new server
				server = new Server();

				// server address
				ServerConnector connector=new ServerConnector(server);
				connector.setHost( conf.getServer().getServer_ip() );
				connector.setPort( conf.getServer().getServer_port() );		    
				server.setConnectors(new Connector[]{connector});

				// server handler
				handler = new ServletHandler();
				server.setHandler(handler);	
				handler.addServletWithMapping( SpotyToastyHandler.class, "/" + conf.getServer().getServer_path() + "/*" );

				// start server
				server.start();

				log.info("Server started on: http://" + conf.getServer().getServer_ip() + ":" + 
						conf.getServer().getServer_port() + "/" +
						conf.getServer().getServer_path());
			
			} catch(Exception e) {
				log.error("Error while starting server", e);
				System.exit(-1);
			}
		}
	}

	/**
	 * Stop the server
	 * 
	 * @throws Exception
	 */
	public void serverStop() throws Exception {
		if(server!=null && handler!=null) {
			handler.stop();
			server.stop();
			log.info("Server stopped.");
		}
	}
}
