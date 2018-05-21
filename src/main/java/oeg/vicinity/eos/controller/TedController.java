package oeg.vicinity.eos.controller;


import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sun.misc.IOUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


@Controller
public class TedController {

    public static Logger log = Logger.getLogger(TedController.class.getName());
    private static String TED_ENDPOINT;
    private static String AGORA_ENDPOINT;

    /**
     * Reads the setup from the configuration file
     */
    public static void initializeFromFile(String file){
        log.info("Reading server setup from file ");
        File f = new File(file);
        try {
            // Check if file exists
            if (f.exists()) {
                // Read file
                String content = new String(Files.readAllBytes(Paths.get(file)));
                JSONObject config = new JSONObject(content);
                AGORA_ENDPOINT = config.getString("AGORA_ENDPOINT");
            } else {
                log.severe("Provided config file does not exists: "+file);
            }
        }catch(Exception e){
            log.severe(e.toString());
        }
        // Check that read enpoint is actually correct
        if(AGORA_ENDPOINT!= null && !AGORA_ENDPOINT.isEmpty()) {
            log.info("\tAgora endpoint: " + AGORA_ENDPOINT);
        }else{
            log.warning("\tERROR, NO AGORA ENDPOINT WAS SPECIFIED IN THE CONFIG FILE UNDER KEY 'AGORA_ENDPOINT'");
        }
    }

    public static void initializeFromEnvironmentVariables(){
        log.info("Reading server setup from environment variables: ");

        // reads from environment variables the entrypoints
        AGORA_ENDPOINT = System.getenv("AGORA_ENDPOINT");
        if(AGORA_ENDPOINT!=null && !AGORA_ENDPOINT.isEmpty()) {
            log.info("\tAgora endpoint: " + AGORA_ENDPOINT);
        }else{
            log.warning("\tERROR, NO AGORA ENDPOINT WAS SPECIFIED AS ENVIRONMENT VARIABLE UNDER 'AGORA_ENDPOINT'");
        }
        initEndpoints();
    }

    private static void initEndpoints(){
        StringBuffer buffer = new StringBuffer(AGORA_ENDPOINT);
        TED_ENDPOINT = buffer.append("/discover").toString();
    }

    /**
     * This method returns a Thing Ecosystem Description (TED) containing Things relevant to answer a given query
     * @param query A SPARQL query
     * @param response The HTTP Response that the Servlet will respond after this method is invoked
     * @return A JSON-LD document containing Things relevant to the query, i.e., a TED
     */
    @RequestMapping(value ="/ted", method = RequestMethod.POST, produces = "application/ld+json")
    @ResponseBody
    public String getSuitableTed(@RequestBody String query,  HttpServletResponse response) {
        Boolean strict = true;
        Boolean min = false;

        // Preamble
        response.setHeader("Server", "Gateway API Services of VICINITY");
        StringWriter responseBody = new StringWriter();
        log.info("Ted retrieving request for query: \n"+query);
        log.info("\tStrict: "+strict);
        log.info("\tMin: "+min);

        // Retrieve TED
        response.setStatus( HttpServletResponse.SC_BAD_REQUEST ); // by default response code is BAD
        String jsonTed = retrieveTED(query, strict, min);
        if(!jsonTed.isEmpty()) { // filter Ted based on requester neighbours
            response.setStatus( HttpServletResponse.SC_OK );
        }

        // Returning TED
        return jsonTed;
    }

    /**
     * This method retrieves from an Agora Server the TED relevant for a given query
     * @param query A SPARQL query
     * @param strict A parameter to forwards to the Agora Server
     * @param min A parameter to forward to the Agora Server specifying that output should (min=false) or should not (min=true) contain RDF associated to Things found
     * @return A JSON-LD document containing relevant Things for the provided query
     */
    private String retrieveTED(String query, Boolean strict, Boolean min){
        String jsonTed = "";

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/ld+json");

        try {

            if (strict && min) {
                jsonTed = Unirest.post(TED_ENDPOINT+"?strict&min").headers(headers).body(query).asJson().getBody().toString();
            } else if (strict && !min) {
                jsonTed = Unirest.post(TED_ENDPOINT+"?strict").headers(headers).body(query).asJson().getBody().toString();
            } else if (!strict && min) {
                jsonTed = Unirest.post(TED_ENDPOINT+"?min").headers(headers).body(query).asJson().getBody().toString();
            } else if (!strict && !min) {
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
        response.setStatus( HttpServletResponse.SC_BAD_REQUEST ); // by default response code is BAD

        try {
            JSONObject jsonDocument = new JSONObject(document);
            String iri = jsonDocument.getString("resource");
            iri = transformIRI(iri);
            // TODO: in case requested IRI has a domain different from the semantic repository a transformation is required domain1/[things,descriptions]/{ID -> domain2/[thigs,descriptions]/{ID}
            log.info("Retrieving RDF for iri :"+iri);
            resourceRDF = Unirest.get(iri).asJson().getBody().toString();
            if(!resourceRDF.isEmpty()) {
                response.setStatus( HttpServletResponse.SC_OK );
            }
        }catch(Exception e){
            log.severe(e.toString());
        }

        return resourceRDF;
    }

    /**
     * This method transform any IRI to an IRI in the same domain of Agora
     * @param iri
     * @return
     */
    private static String transformIRI(String iri){
        String newIRI = "";
        String subIRI = "";
        if(iri.contains("/things/")){
            subIRI = iri.substring(iri.indexOf("/things/"), iri.length());
        }else if(iri.contains("/descriptions/")){
            subIRI = iri.substring(iri.indexOf("/descriptions/"), iri.length());
        }else{
            log.severe("Malformed IRI :"+iri);
        }

        newIRI = AGORA_ENDPOINT + subIRI;
        return newIRI;
    }

    @RequestMapping(value ="/", method = RequestMethod.GET)
    @ResponseBody
    public String getTest() {
        return "Vicinity Gateway API Services";
    }






}
