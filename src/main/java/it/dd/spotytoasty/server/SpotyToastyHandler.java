package it.dd.spotytoasty.server;

import it.dd.spotytoasty.calls.SpotyWeb;
import lombok.extern.log4j.Log4j;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;

/**
 * Handler for Spotify authorization code
 * 
 * @author shamalaya
 *
 */
@Log4j
public class SpotyToastyHandler extends HttpServlet {
	
	private static final long serialVersionUID = -1104617016318539066L;
	
	@Override
	protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException {
	
		String code = request.getParameter("code");
		
		if( code!=null && !code.isEmpty() ) {
			
			log.info( "Server param Code: " + code );

			try {
				
				// authorize code
				SpotyWeb.getInstance().authorizationCode(code);
				
				response.setContentType("text/html");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("<div style=\"text-align:center\">SpotyToasty connected!</div>");
				response.getWriter().flush();
				
				// wakeup main thread
				SpotyWeb.authorized.countDown();
				
			} catch (SpotifyWebApiException e) {
				response.setContentType("text/html");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println("<div>Sry shit happens :(</div>");
				response.getWriter().println("<div>" + e.getMessage() + "</div>");
				response.getWriter().flush();
				throw new ServletException("Error authorizationCode_Sync()");
			}
			
		} else {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("<div>No code receved!</div>");
			response.getWriter().flush();
		}
	}
}