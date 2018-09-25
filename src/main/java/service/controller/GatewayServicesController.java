package service.controller;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.mashape.unirest.http.Unirest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@Controller
public class GatewayServicesController {

	// -- Attributes
	
    private static Logger log = Logger.getLogger(GatewayServicesController.class.getName());
    private static String TED_ENDPOINT;
    private static String PREFIXES_ENDPOINT;
    private static String AGORA_ENDPOINT;

    private static String DATA_DOMAIN;
    
    // -- Constructor
    public GatewayServicesController() {
    		// empty
    }
    
    
    // -- Ancillary methods
    
    /**
     * This method initializes Agora endpoints from a configuration file
     * @param file The path of the configuration file
     */
    public static void initializeFromFile(String file){
    		log.info("Initializing server");
        // 1. Read from file the Agora endpoint
    		File f = new File(file);
        try {
            if (f.exists()) {
                String content = new String(Files.readAllBytes(Paths.get(file)));
                JSONObject config = new JSONObject(content);
                if(config.has("AGORA_ENDPOINT") && config.has("DATA_DOMAIN")) {
                		AGORA_ENDPOINT = config.getString("AGORA_ENDPOINT");
                		DATA_DOMAIN = config.getString("DATA_DOMAIN");
                }else {
                	 	log.severe("Provided config lacks of a mandatory key, either 'AGORA_ENDPOINT' or 'DATA_DOMAIN'");
                     System.exit(0);
                }
            } else {
                log.severe("Provided config file does not exists");
                System.exit(0);
            }
        }catch(Exception e){
            log.severe(e.toString());
            System.exit(0);
        }
        // 2. Check that read enpoint is correct
        validateAgoraEndpoints();
    }

    /**
     * This method checks that read Agora endpoint is correct
     */
    private static void validateAgoraEndpoints(){
    		// 2. Check that read enpoint is correct, i.e., not null & not empty
        if(AGORA_ENDPOINT!= null && !AGORA_ENDPOINT.isEmpty() && DATA_DOMAIN!=null && !DATA_DOMAIN.isEmpty()) {
            TED_ENDPOINT = AGORA_ENDPOINT.concat("/discover");
            PREFIXES_ENDPOINT = AGORA_ENDPOINT.concat("/prefixes");
            log.info("Agora endpoints: ");
            log.log(Level.INFO, () -> "\t>"+AGORA_ENDPOINT);
            log.log(Level.INFO, () -> "\t>"+PREFIXES_ENDPOINT);
            log.log(Level.INFO, () -> "\t>"+TED_ENDPOINT);
            log.log(Level.INFO, () -> "Local data domain is "+DATA_DOMAIN);
        }else{
            log.severe("No Agora endpoint was specifiedin the config file under key 'AGORA_ENDPOINT'");
            System.exit(0);
        }
    }

    
    private void prepareResonse(HttpServletResponse response) {
    	 	response.setHeader("Server", "Gateway API Services of VICINITY"); // Avoids clients to know the server we are using
         response.setStatus( HttpServletResponse.SC_BAD_REQUEST ); // by default response code is BAD
    }
    
    // -- Controller methods
    
    /**
     * This method returns a Thing Ecosystem Description (TED) containing Things relevant to answer a given query
     * @param query A SPARQL query
     * @param response The HTTP Response that the Servlet will respond after this method is invoked
     * @return A JSON-LD document containing Things relevant to the query, i.e., a TED
     */
    @RequestMapping(value ="/prefixes", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public String getPrefixes(HttpServletResponse response) {
    	 	JSONObject jsonPrefixes = new JSONObject();
    		// 1. Prepare response & request headers
    		prepareResonse(response);
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");
        	// 2. Request prefixes to Agora
        try {
			String jsonPrefixesString = Unirest.get(PREFIXES_ENDPOINT).headers(headers).asJson().getBody().toString();
			jsonPrefixes = new JSONObject(jsonPrefixesString);
		} catch (Exception e) {
			log.severe(e.toString());
		}
        // 3. Check prefixes obtained
        if(jsonPrefixes.keys().hasNext()) {
            response.setStatus( HttpServletResponse.SC_OK );
        }else {
        		response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
        	log.info("Prefixes requested");
        	String prefixesTrace = jsonPrefixes.toString().replace("\",\"", "\",\n\t\"");
        	log.log(Level.INFO, () -> "\t>"+prefixesTrace);
        	return jsonPrefixes.toString();
    }
      
    /**
     * This method returns a Thing Ecosystem Description (TED) containing Things relevant to answer a given query
     * @param query A SPARQL query
     * @param response The HTTP Response that the Servlet will respond after this method is invoked
     * @return A JSON-LD document containing Things relevant to the query, i.e., a TED
     */
    @RequestMapping(value ="/discovery", method = RequestMethod.POST, produces = "application/ld+json")
    @ResponseBody
    public String getSuitableTed(@RequestBody String query,  HttpServletResponse response) {
        // 1. Following variables are Agora bespoke setup, we set them with such values since fit better for VICINITY requirements
    		Boolean strict = true;
        Boolean min = false;
        // 2. Prepare response
        prepareResonse(response);
        log.info("Ted retrieving request for query: \n");
        log.info(query);
        // 3. Retrieve TED from Agora and set response code
        String jsonTed = retrieveTED(query, strict, min);
        if(!jsonTed.isEmpty()) {
            response.setStatus( HttpServletResponse.SC_OK );
        }else {
        		response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
        // 4. Change domain of IRIs from Agora's to Local
        jsonTed =  jsonTed.replace(AGORA_ENDPOINT, DATA_DOMAIN);
        // 5. Return TED
        return jsonTed;
    }

    /**
     * This method retrieves from Agora the TED for a given query
     * @param query A SPARQL query
     * @param strict A bespoke Agora parameter, it specifies whether TED should contain only strict data to answer the query
     * @param min A bespoke Agora parameter, it specifies whether the output should (min=false) or should not (min=true) contain RDF associated to the Things found
     * @return A JSON-LD document containing the TED for the provided query
     */
    private String retrieveTED(String query, Boolean strict, Boolean min){
        String jsonTed = "";
        // 1. Set request headers
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/ld+json");
        try {
        		// 2. Send POST request to Agora
            if (strict && min) {
                jsonTed = Unirest.post(TED_ENDPOINT+"?strict&min").headers(headers).body(query).asJson().getBody().toString();
            } else if (strict) {
                jsonTed = Unirest.post(TED_ENDPOINT+"?strict").headers(headers).body(query).asJson().getBody().toString();
            } else if (min) {
                jsonTed = Unirest.post(TED_ENDPOINT+"?min").headers(headers).body(query).asJson().getBody().toString();
            } else {
                jsonTed = Unirest.post(TED_ENDPOINT).headers(headers).body(query).asJson().getBody().toString();
            }
        }catch(Exception e){
            log.severe(e.toString());
        }
                
        return jsonTed;
    }


    /**
     * This method returns the RDF of a Thing or a Thing Description
     * @param document A JSON document containing an IRI that identifies the resource
     * @param response The HTTP Response that the Servlet will respond after this method is invoked
     * @return A RDF document containing the RDF of a Thing or a Thing Description
     */
    @RequestMapping(value ="/resource", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public String getSemanticResource(@RequestBody String document,  HttpServletResponse response) {
        String resourceRDF = "";
        // 1. Prepare response
        prepareResonse(response);
        try {
        		// 2. Retrieve from provided JSON Document the value under key 'resource'
            JSONObject jsonDocument = new JSONObject(document);
            String iri = jsonDocument.getString("resource");
            // 3. Transform the value of key 'resource' into an IRI in the namespace of Agora
            iri = translateIRIToNamespace(iri, AGORA_ENDPOINT); // this could be actually a simple regex
            if(!iri.isEmpty()) {
	            // 3.A Request Agora the resource RDF
	            resourceRDF = Unirest.get(iri).asJson().getBody().toString();
	            if(!resourceRDF.isEmpty()) {
	                response.setStatus( HttpServletResponse.SC_OK );
	                log.info("Retrieving RDF for resource");
	                log.info(resourceRDF);
	            }else {
	            	response.setStatus( HttpServletResponse.SC_NO_CONTENT );
	            }
            }
        }catch(Exception e){
            log.severe(e.toString());
        }
        
        // 4. Change domain of IRIs from Agora's to Local
        resourceRDF =  resourceRDF.replace(AGORA_ENDPOINT, DATA_DOMAIN);
        // 5. Return resource RDF
        return resourceRDF;
    }

   
    /**
     * This method transforms any IRI in the same namespace of Agora into another IRI in a different namespace.
     * <p>
     * For instance an IRI 'http://domain1.com/things/01' could be changed to 'http://red-domain.eu/things/01'. The IRIs must be Agora like, therefore contain a /things or /descriptions
     * @param iri An IRI in the namespace of Agora
     * @param namespace A namespace to which we want to express the IRI
     * @return An IRI in the new namespace
     */
    private String translateIRIToNamespace(String iri, String namespace){
        String newIRI = "";
        String subIRI = "";
        // 1. Retrieve domain IRI
        if(iri.contains("/things/")){
            subIRI = iri.substring(iri.indexOf("/things/"), iri.length());
        }else if(iri.contains("/descriptions/")){
            subIRI = iri.substring(iri.indexOf("/descriptions/"), iri.length());
        }else{
            log.log(Level.SEVERE, () -> "Malformed IRI :"+iri);
        }
        // Change domain IRI to new namespace
        if(!subIRI.isEmpty())
        		newIRI = namespace.concat(subIRI);
        return newIRI;
    }

    /**
     * This method returns a Search Plan from Agora required to answer a given query
     * @param query A SPARQL query
     * @param response The HTTP Response that the Servlet will respond after this method is invoked
     * @return A JSON-LD document containing the query Plan
     */
    @RequestMapping(value ="/plan", method = RequestMethod.POST, produces = "application/ld+json")
    @ResponseBody
    public String getPlan(@RequestBody String query,  HttpServletResponse response) {
        String plan = "";
    		// 2. Prepare response
        prepareResonse(response);
        log.info("Plan retrieving request for query: \n");
        log.info(query);
        // 3. Retrieve TED from Agora and set response code
        try {
        		// 3.1 Prepare headers 
        		Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/ld+json");
            // 3.2 Prepare body
            String body = transformToAGP(query);
            plan = Unirest.post(TED_ENDPOINT+"?strict&min").headers(headers).body(query).asJson().getBody().toString(); 
        }catch(Exception e){
            log.severe(e.toString());
        }
        if(!plan.isEmpty()) {
            response.setStatus( HttpServletResponse.SC_OK );
        }else {
        		response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
        // 4. Change domain of IRIs from Agora's to Local
        plan =  plan.replace(AGORA_ENDPOINT, DATA_DOMAIN);
        // 5. Return Plan
        return plan;
    }
    
    /**
     * This method transforms a query into a AGP required by Agora
     * @param query A SPARQL query
     * @return An AGP
     */
    private String transformToAGP(String query) {
    		// 1. Retrieve AGP from query	
    		return null;
    }

    
   

}
