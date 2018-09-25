# VICINITY Gateway API Services

### About

The VICINITY Gateway API Services provides semantic interoperability in the VICINITY cloud, its main goals are:
* Generate ecosystems of things (Things Ecosystem Description - TED) containing relevant semantic resources for discovery requests
* Generate discovery and consumption plans (query plans) for queries issued from VICINITY Nodes.
* Retrieve static RDF of semantic resources




### Installation 

Download the [latest](https://github.com/vicinityh2020/vicinity-gateway-api-services/releases) release of the VICINITY Gateway API Services. Then, create a configuration file as explained in the **Configuration** section, and finally, follow the isntructions provided in the **Deploying VICINITY Gateway API Services** section of this document.

##### Build an executable The VICINITY Gateway API Services jar
Download this project, then move to its directory and compile the project with maven. As output a target folder will be created containing the *jar* file. 

```
#!shell
git clone https://github.com/vicinityh2020/vicinity-gateway-api-services.git
cd vicinity-gateway-api-services
mvn clean package
```
The compiled *jar* allows to start the service as we explain in the **Deploying VICINITY Gateway API Services** section.

### Configuration 

In order to deploy the VICINITY Gateway API Services we need to define a configuration file, and run an [Agora service](https://github.com/fserena/agora-cli). The configuration file is a JSON document containing the Agora service address and the domain under which we want to publish data. To shed some light over this configuration file and its content check the snippet below containing the pointer to a local Agora service; notice that we established *http://vicinity.eu/data* as domain to publish the RDF resources.

```
{
	"AGORA_ENDPOINT" : "http://gateway-services.vicinity.linkeddata.es",
	"DATA_DOMAIN" : "http://vicinity.eu/data"
}
```

### Deploying VICINITY Gateway API Services

To start the VICINITY Gateway API Services we need to type:
```
java -jar gateway-api-services.jar --config configFile.json
```
After that the service will start on port *8081*, the log will be displayed on the same terminal used to start the process.

### Service Usage
Once the Gateway API Services is running several services are available

| URL        | Type |                  Headers                 | Body Content                                                                                   |
|------------|------|:----------------------------------------:|------------------------------------------------------------------------------------------------|
| /prefixes  | GET  | None                                     | -                                                                                              |
| /discovery | POST | None                                     | A SPARQL Query                                                                                 |
| /plan      | POST | None                                     | A SPARQL Query                                                                                 |
| /resource  | POST | {  "Content-Type" : "application/json" } | A JSON document containing the IRI of a resource as value:   {     "resource" : "http://..." } |

Some examples of the Gateway API Services usage Interface can be found [here](https://documenter.getpostman.com/view/3240053/vicinity-gateway-api-services/RVu1Hr6o)
