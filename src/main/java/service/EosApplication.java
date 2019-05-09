package service;

import service.controller.GatewayServicesController;

import java.util.logging.Logger;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;


@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class ) // This prevents Spring to create default error-handling route /error 
public class EosApplication implements ApplicationRunner{
	
	// -- Attributes
    private Logger log = Logger.getLogger(EosApplication.class.getName());

	
	public static void main(String[] args) {
		SpringApplication.run(EosApplication.class, args);
	}

    @Override
	public void run(ApplicationArguments args) throws Exception {
    		System.out.println("EOS v.2");
        String [] stringArgs = args.getSourceArgs();
        // 1. Check console arguments
        if(stringArgs.length == 2 && stringArgs[0].equals("--config")){
            String file = stringArgs[1];
            if(!file.isEmpty()) {
            		// 1.A.A If a configuration file was provided, then initialize the server
                GatewayServicesController.initializeFromFile(file);
            }else{
            		// 1.A.B Otherwise close everything
            		log.severe("Provided json file for configuration is empty");
                System.exit(0);
            }
        }else{
        		log.severe("VICINITY GATEWAY API SERVICES usage:\n\t - reads from the environment variables the configuration\n\t--config [file.json] - reads the configuration from the specified file\n\t--help - show this message\n\n[CONFIGURATION PARAMETERS]");
            System.exit(0);
        }
    }
}
