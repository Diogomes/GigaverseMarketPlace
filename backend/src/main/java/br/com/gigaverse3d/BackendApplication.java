package br.com.gigaverse3d;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import br.com.gigaverse3d.common.config.ProductionConfigValidator;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(BackendApplication.class);
		// valida a configuração de produção antes de qualquer bean (banco, Flyway) ser criado
		app.addListeners(new ProductionConfigValidator());
		app.run(args);
	}

}
