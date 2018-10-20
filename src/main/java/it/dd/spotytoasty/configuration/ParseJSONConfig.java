package it.dd.spotytoasty.configuration;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j;

/**
 * Handle JSON Configuration
 * 
 * @author shamalaya
 *
 */
@Log4j
public class ParseJSONConfig {
	
	private String configFile;
	private Configuration conf;
	
	public ParseJSONConfig(String configFile) {
		this.configFile = configFile;
	}

	public void parseConfiguration() throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
	    conf = mapper.readValue( new File( configFile ) , Configuration.class ); 
	    log.info("Configuration parsed from: " + configFile);
	}
	
	public void writeConfiguration() throws JsonGenerationException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writerWithDefaultPrettyPrinter().writeValue( new File(configFile), conf );
		log.info("Configuration saved to: " + configFile);
	}
	
	public Configuration getConfiguration() {
		return conf;
	}
}
