# VICINITY Gateway API Services

### About

The VICINITY Gateway API Services provides semantic interoperability in the VICINITY cloud, its main goals are:
* Generate ecosystems of things (Things Ecosystem Description - TED) containing relevant semantic resources for discovery requests
* Generate discovery and consumption plans (query plans) for queries issued from VICINITY Nodes.
* Retrieve static RDF of semantic resources

### Installation 

Download the source code from the repository:
```
#!shell
git clone https://github.com/vicinityh2020/vicinity-gateway-api-services.git
```
Following a jar file can be generated or a Docker image
##### Build an executable jar
Compile the source and build a jar file:
```
#!shell
sudo mvn clean package
```
A target folder will be generated with the gateway-api-services.jar file that allows to start the service as we will explain in next section. 

##### Build a docker image
Generate a Docker image:
```
#!shell
bash mvnw install dockerfile:build # mvnw is provided with the code

```



### Configuration 

The Gateway API Services relies on an [Agora service](https://github.com/fserena/agora-py). The quickest way to deploy a service is downloading from its [repository](https://github.com/fserena/agora-docker) the Docker files and setting up the different parameters according to the requirements. A recommended docker-compose file is the following:
```
version: '2'
services:
  fountain:
    image: fserena/agora-fountain
    environment:
      FOUNTAIN_DB_HOST: redis
  agora-nginx:
    image: fserena/agora-nginx
    ports:
    - 8000:80/tcp
  discovery:
    image: fserena/agora-gw
    environment:
      API_PORT: '8000'
      EXTENSION_BASE: http://localhost:8000/
      FOUNTAIN_HOST: fountain
      FOUNTAIN_PORT: '5000'
      QUERY_CACHE_HOST: redis
      REPOSITORY_BASE: http://localhost:8000/
      SPARQL_HOST: http://localhost:7200/repositories/tds
      UPDATE_HOST: http://localhost:7200/repositories/tds/statements
  redis:
    image: redis
```
Deploy Agora navigating to the directory where this file is located and executing the following commands to manage the service
```
!#shell
docker-compose up -d # starts the service
docker-compose stop  # stops the service
docker-compose down  # erases the service content
```


### Deploying 

Afeter an Agora service is deployed the Gateway API Services can be started from the terminal using a config file typing:
```
!#shell
# Using port 8081
java -Djava.security.egd=file:/dev/./urandom -Dserver.port=8081 -jar gateway-api-services.jar --config config.json
```
On the other hand a docker version can be deployed as follows:
```
docker run -d --name gateway-api-services -p 8081:8081 springio/gateway-api-services
```

Finally we can deploy the Gateway API Services into a Docker image and upload such image to a public repository
```
!#shell
docker run -d --name gateway-api-services -p 8081:8081 springio/gateway-api-services
docker tag springio/gateway-api-services [YOUR DOCKER ACCOUNT]/gateway-api-services 
docker push [YOUR DOCKER ACCOUNT]/gateway-api-services
```

### Service Usage
Once the Gateway API Services is running several services are available

| URL        | Type |                  Headers                 | Body Content                                                                                   |
|------------|------|:----------------------------------------:|------------------------------------------------------------------------------------------------|
| /discovery | POST | None                                     | A SPARQL Query                                                                                 |
| /plan      | POST | None                                     | A SPARQL Query                                                                                 |
| /resource  | POST | {  "Content-Type" : "application/json" } | A JSON document containing the IRI of a resource as value:   {     "resource" : "http://..." } |

Some examples of the Gateway API Services usage Interface can be found [here](https://documenter.getpostman.com/view/3240053/vicinity-gateway-api-services/RVu1Hr6o)
