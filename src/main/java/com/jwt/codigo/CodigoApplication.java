package com.jwt.codigo;

import com.jwt.codigo.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class CodigoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodigoApplication.class, args);
	}

}
