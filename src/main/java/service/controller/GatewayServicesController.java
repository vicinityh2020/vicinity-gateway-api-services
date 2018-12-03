package service.controller;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.mashape.unirest.http.Unirest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


@Controller
public class GatewayServicesController {

	// -- Attributes
	
    private static Logger log = Logger.getLogger(GatewayServicesController.class.getName());
    private static String tedEndpoint;
    private static String planEndpoint;
    private static String prefixesEndpoint;
    private static String agoraEndpoint;

    private static String dataDomain = "http://vicinity.eu/data";
    private static String repositoryEndpoint;
    private static final String HEADER_ACCEPT_KEY = "Accept";
    private final String queryHead = "?name=&infer=true&sameAs=true&query=";

    	public static Map<String,Model> cache = new ConcurrentHashMap<String, Model>();
    
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
                if(config.has("AGORA_ENDPOINT") && config.has("DATA_DOMAIN") && config.has("SEMANTIC_REPOSITORY_ENDPOINT")) {
                		agoraEndpoint = config.getString("AGORA_ENDPOINT");
                		dataDomain = config.getString("DATA_DOMAIN");
                		repositoryEndpoint = config.getString("SEMANTIC_REPOSITORY_ENDPOINT");
                		
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
        if(agoraEndpoint!= null && !agoraEndpoint.isEmpty() && dataDomain!=null && !dataDomain.isEmpty()) {
            tedEndpoint = agoraEndpoint.concat("/discover");
            prefixesEndpoint = agoraEndpoint.concat("/prefixes");
            planEndpoint = agoraEndpoint.concat("/plan");
            log.info("Agora endpoints: ");
            log.log(Level.INFO, () -> "\t>"+agoraEndpoint);
            log.log(Level.INFO, () -> "\t>"+prefixesEndpoint);
            log.log(Level.INFO, () -> "\t>"+tedEndpoint);
            log.log(Level.INFO, () -> "\t>"+planEndpoint);
            log.log(Level.INFO, () -> "Local data domain is "+dataDomain);
            log.log(Level.INFO, () -> "\t>"+repositoryEndpoint);
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
    		Unirest.setTimeouts(0, 0);
    	 	JSONObject jsonPrefixes = new JSONObject();
    		// 1. Prepare response & request headers
    		prepareResonse(response);
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_ACCEPT_KEY, "application/json");
        	// 2. Request prefixes to Agora
        try {
			String jsonPrefixesString = Unirest.get(prefixesEndpoint).headers(headers).asJson().getBody().toString();
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
        if(jsonTed!=null) {
            response.setStatus( HttpServletResponse.SC_OK );
        }else {
        		response.setStatus( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        }
        // 4. Change domain of IRIs from Agora's to Local
        jsonTed =  jsonTed.replace(agoraEndpoint, dataDomain);
        log.info("Returning ted");
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
    		Unirest.setTimeouts(0, 0);
        String jsonTed = null;
        // 1. Set request headers
        Map<String, String> headers = new HashMap<>();
        headers.put(HEADER_ACCEPT_KEY, "application/ld+json");
        try {
        		// 2. Send POST request to Agora
            if (strict && min) {
                jsonTed = Unirest.post(tedEndpoint+"?strict&min").headers(headers).body(query).asJson().getBody().toString();
            } else if (strict) {
                jsonTed = Unirest.post(tedEndpoint+"?strict").headers(headers).body(query).asJson().getBody().toString();
            } else if (min) {
                jsonTed = Unirest.post(tedEndpoint+"?min").headers(headers).body(query).asJson().getBody().toString();
            } else {
                jsonTed = Unirest.post(tedEndpoint).headers(headers).body(query).asJson().getBody().toString();
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
    		Unirest.setTimeouts(0, 0);
        String resourceRDF = "";
        // 1. Prepare response
        prepareResonse(response);
        try {
        		// 2. Retrieve from provided JSON Document the value under key 'resource'
            JSONObject jsonDocument = new JSONObject(document);
            String iri = jsonDocument.getString("resource");
            // 3. Transform the value of key 'resource' into an IRI in the namespace of Agora
            iri = translateIRIToNamespace(iri, agoraEndpoint); // this could be actually a simple regex
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
        resourceRDF =  resourceRDF.replace(agoraEndpoint, dataDomain);
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
    @RequestMapping(value ="/plan", method = RequestMethod.POST, produces = "text/turtle")
    @ResponseBody
    public String getPlan(@RequestBody String query,  HttpServletResponse response) {
    		Unirest.setTimeouts(0, 0);
        String plan = "";
    		// 2. Prepare response
        prepareResonse(response);
        log.info("Plan retrieving request for query: \n");
        log.info(query);
        // 3. Retrieve TED from Agora and set response code
        try {
        		// 3.1 Prepare headers 
        		Map<String, String> headers = new HashMap<>();
            headers.put(HEADER_ACCEPT_KEY, "text/turtle");
            headers.put("Content-Type", "application/json");
            // 3.2 Prepare body
            plan = Unirest.post(planEndpoint).headers(headers).body(query).asString().getBody();
        }catch(Exception e){
            log.severe(e.toString());
        }
        if(!plan.isEmpty()) {
            response.setStatus( HttpServletResponse.SC_OK );
        }else {
        		response.setStatus( HttpServletResponse.SC_NO_CONTENT );
        }
        // 4. Change domain of IRIs from Agora's to Local
        plan =  plan.replace(agoraEndpoint, dataDomain);
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
    
    /**
     * This method returns a Thing Ecosystem Description (TED) containing Things relevant to answer a given query
     * @param query A SPARQL query
     * @param response The HTTP Response that the Servlet will respond after this method is invoked
     * @return A JSON-LD document containing Things relevant to the query, i.e., a TED
     */
    @RequestMapping(value ="/advanced-discovery", method = RequestMethod.POST, produces = "application/ld+json")
    @ResponseBody
    public String getDiscovery(@RequestBody String query, @RequestParam String neighbors, HttpServletResponse response) {
    		prepareResonse(response);
    		Unirest.setTimeouts(0, 0);
    		String ted = "{}";
    		String endpoint = repositoryEndpoint;
    		// 1. Following variables are Agora bespoke setup, we set them with such values since fit better for VICINITY requirements
    		if(!query.isEmpty()) {
    			try {
	    				log.info("Discovery query received");
	    				// Build TED
	    	    			ted = buildTED(endpoint, neighbors);
					response.setStatus( HttpServletResponse.SC_OK );
					log.info("TED answered");
			} catch (Exception e) {
				log.severe(e.toString());
			}
    		}
    		cache.clear();
    		return ted;
    }



	private String buildTED(String endpoint, String ted) {
		Model tedFiltered = parseRDF("<http://vicinity.eu/data/ted> a <http://iot.linkeddata.es/def/core#ThingEcosystemDescription>;\n   <http://iot.linkeddata.es/def/core#describes> <http://bnodes/N9e711c303f3e40f7872d87ccb66cc225> .\n \n <http://bnodes/N9e711c303f3e40f7872d87ccb66cc225>  a <http://iot.linkeddata.es/def/core#Ecosystem>.", "TURTLE");
		Property hasComponentPredicate = ResourceFactory.createProperty("http://iot.linkeddata.es/def/core#hasComponent");
		String[] oids = ted.split(",");
		int maxIndex = oids.length;
		int index = 0;
		while(index < maxIndex) {
			String oid = oids[index].trim();
			String thing = GatewayServicesController.dataDomain+"/things/"+oid;
			if(!cache.containsKey(thing)) {
				// retrieve all the RDF of the IRI in 'line'
				this.visitedIRIs = new ArrayList<>();
				Model thingRDF = retrieveThingRDF(endpoint, thing);
				if(!thingRDF.isEmpty()) {
					// add thing to ted
					tedFiltered.add(ResourceFactory.createResource("http://bnodes/N9e711c303f3e40f7872d87ccb66cc225"), hasComponentPredicate, ResourceFactory.createResource(thing));
					tedFiltered.add(thingRDF);
					cache.put(thing, thingRDF);
				}
			}else {
				tedFiltered.add(ResourceFactory.createResource("http://bnodes/N9e711c303f3e40f7872d87ccb66cc225"), hasComponentPredicate, ResourceFactory.createResource(thing));
				tedFiltered.add(cache.get(thing));
			}
			
			index++;
		}
		// TODO: IMPROVE THIS CACHE
		
		
		return toString(tedFiltered,"JSONLD");
	}
	
	private List<String> visitedIRIs;
	private Model retrieveThingRDF(String endpoint, String thingIri) {
		Model tedFiltered = ModelFactory.createDefaultModel();

		try {
			if(!visitedIRIs.contains(thingIri)) {
				visitedIRIs.add(thingIri);
				String thingRequest = endpoint+this.queryHead.replace("query=", "query=")+URLEncoder.encode("select distinct * where { <"+thingIri+"> ?p ?o . }","UTF-8")+"&execute=";
				String thingRDF = Unirest.get(thingRequest).asString().getBody();
				String[] thingLines = thingRDF.split("\n");
				int maxIndex = thingLines.length;
				int index = 1;
				while(index < maxIndex) {
					String thingLine = thingLines[index];
					int commaIndex = thingLine.indexOf(',');
					if(commaIndex>-1) {
						Property predicate = ResourceFactory.createProperty(thingLine.substring(0, commaIndex).trim());
						String object =  thingLine.substring(commaIndex+1, thingLine.length()).trim();
						Model thingRDFRetrieved = processThingRDF(endpoint, predicate, object, thingIri);
						tedFiltered.add(thingRDFRetrieved);
						index++;
					}else {
						break;
					}
				}
			}
		} catch (Exception e) {
			log.severe(e.toString());
		}
		
		return tedFiltered;
	}
	
	private Model processThingRDF(String endpoint, Property predicate, String object, String thingIri) {
		Model tedFiltered = ModelFactory.createDefaultModel();
			if(object.startsWith("http")) {
				Resource resource = ResourceFactory.createResource(object);
				tedFiltered.add(ResourceFactory.createResource(thingIri),predicate, resource);
				Model auxiliarRDF = ModelFactory.createDefaultModel();
				if(object.contains("/things") || object.contains("/descriptions") || ( object.startsWith("http://bnode") && !predicate.toString().contains("http://iot.linkeddata.es/def/core#isComponentOf") )) { 
					auxiliarRDF = retrieveThingRDF(endpoint, object);
				}
				tedFiltered.add(auxiliarRDF);
			}else if(object.startsWith("_:")){
				// tedFiltered.add(ResourceFactory.createResource(thingIri),predicate,ResourceFactory.createResource());
			}else {
				Literal literal = ResourceFactory.createPlainLiteral(object);
				tedFiltered.add(ResourceFactory.createResource(thingIri),predicate, literal);
			}
		return tedFiltered;
	}

    /**
     * This method transforms a String variable with RDF content into a jena {@link Model}
     * @param strRDF A String variable containing RDF in "JSON-LD" format
     * @return a jena {@link Model}
     */
    private  Model parseRDF(String strRDF, String format) {
    		Model parsedModel = ModelFactory.createDefaultModel();
    		try {
			 InputStream is = new ByteArrayInputStream( strRDF.getBytes() );
			 parsedModel.read(is, null, format);
    		}catch(Exception e) {
    			String message = ("Something went wrong parsing RDF\n").concat(e.toString());
    			log.severe(message);
    		}
		return parsedModel;
	}
    
    /**
     * This method transforms the RDF within a {@link Model} into a String variable
     * @param model a jena {@link Model} 
     * @return a String variable with the same RDF of the input {@link Model} in "TURTLE" format
     */
    private String toString(Model model, String format) {
		Writer output = new StringWriter();
		model.write(output, format);
		return output.toString();
	}


    
}
