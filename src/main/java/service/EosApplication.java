package service;

import service.controller.TedController;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EosApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(EosApplication.class, args);
	}


    @Override
    public void run(ApplicationArguments args) throws Exception {
        String [] stringArgs = args.getSourceArgs();
        if(stringArgs.length == 1 && stringArgs[0].equals("--help")){
            System.out.println("VICINITY GATEWAY API SERVICES usage:\n\t - reads from the environment variables the configuration\n\t--config [file.json] - reads the configuration from the specified file\n\t--help - show this message\n\n[CONFIGURATION PARAMETERS]");
            System.exit(0);
        } else if(stringArgs.length == 0) {
            // Reads setup from environment variables and initializes global variables
            TedController.initializeFromEnvironmentVariables();
        }else if(stringArgs.length == 2 && stringArgs[0].equals("--config")){
            // Reads setup from file and initializes global variables
            String file = stringArgs[1];
            if(!file.isEmpty()) {
                TedController.initializeFromFile(file);
            }else{
                TedController.log.severe("Provided json file for configuration is empty");
                System.exit(0);
            }
        }else{
           TedController.log.severe("Incorrect arguments were provided to the GATEWAY API SERVICES");
            System.out.println("VICINITY GATEWAY API SERVICES arguments:\n\t[empty] -> reads from the environment variables the configuration\n\t--config [file.json] -> reads the configuration from the specified file\n\t--help -> show this message\n\n[CONFIGURATION PARAMETERS]");
            System.exit(0);
        }

    }
}
