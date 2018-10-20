package it.dd.spotytoasty.utils;

import java.io.File;
import lombok.extern.log4j.Log4j;

/**
 * Utils Class
 * 
 * @author shamalaya
 *
 */
@Log4j
public class Utils {

	/**
	 * Sleep function
	 * 
	 * @param ms
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {}
	}

	/**
	 * Parse cmd parameter
	 * 
	 * @param args
	 * @return
	 */
	public static String parserParameter(String[] args) {
		
		if( args.length == 1 ) {			
			if( new File(args[0]).exists() )
				return args[0];		
		} else {		
			try {
				return new File( ClassLoader.getSystemResource("conf/config.json").toURI() ).getAbsolutePath();
			} catch(Exception e ) {
				log.error("Error config file not found",e);
			}
		}
		
		usage();
		return null;
	}
	
	/**
	 * Print usage
	 */
	private static void usage() {
		System.out.println("USAGE:");
		System.out.println("SpotyToasty [config_file]");
	}	
}
