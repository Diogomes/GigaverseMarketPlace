package br.com.gigaverse3d.storage;

import java.net.URI;
import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

	@Bean
	@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
	LocalStorageService localStorageService(StorageProperties properties) {
		String root = properties.local() != null && StringUtils.hasText(properties.local().rootDir())
				? properties.local().rootDir() : "./storage";
		return new LocalStorageService(Path.of(root));
	}

	@Bean
	@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
	S3StorageService s3StorageService(StorageProperties properties) {
		StorageProperties.S3 s3 = properties.s3();
		if (s3 == null || !StringUtils.hasText(s3.bucket()) || !StringUtils.hasText(s3.region())
				|| !StringUtils.hasText(s3.publicBaseUrl())) {
			throw new IllegalStateException(
					"app.storage.type=s3 exige app.storage.s3.bucket, region e public-base-url (ver ENVIRONMENT_VARIABLES.md)");
		}
		S3ClientBuilder builder = S3Client.builder()
			.region(Region.of(s3.region()))
			.forcePathStyle(s3.pathStyleAccess());
		if (StringUtils.hasText(s3.endpoint())) {
			builder.endpointOverride(URI.create(s3.endpoint()));
		}
		return new S3StorageService(builder.build(), s3.bucket(), s3.publicBaseUrl());
	}

}
