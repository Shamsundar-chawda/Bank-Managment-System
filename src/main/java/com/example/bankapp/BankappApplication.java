package com.example.bankapp;

import com.example.bankapp.config.DotenvPropertyLoader;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankappApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		SpringApplication application = new SpringApplication(BankappApplication.class);
		application.setDefaultProperties(DotenvPropertyLoader.loadAdminDefaults(dotenv));
		application.run(args);
	}

}
