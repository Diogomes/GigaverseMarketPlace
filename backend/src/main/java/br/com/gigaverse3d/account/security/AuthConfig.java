package br.com.gigaverse3d.account.security;

import java.time.Clock;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

	/**
	 * Argon2id como padrão (hash prefixado com {argon2}); BCrypt aceito para eventual migração.
	 * Parâmetros recomendados pelo Spring Security 5.8+ (16 MiB de memória, 2 iterações).
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new DelegatingPasswordEncoder("argon2", Map.of("argon2",
				Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(), "bcrypt", new BCryptPasswordEncoder(12)));
	}

	@Bean
	AuthenticationManager authenticationManager(AccountUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
