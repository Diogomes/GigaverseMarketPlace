package br.com.gigaverse3d.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.net.URI;

@Testcontainers
class StorageTest {

	/** S3Mock (Adobe): servidor S3-compatível local; imagens oficiais do MinIO não estão mais no Docker Hub. */
	@Container
	static GenericContainer<?> s3mock = new GenericContainer<>("adobe/s3mock:3.12.0").withExposedPorts(9090);

	@TempDir
	Path tmp;

	private static final String KEY = "products/42/0b6c7a5e-1111-4222-8333-444455556666.png";

	@Test
	void localStorageWritesInsideRootAndRejectsTraversal() throws Exception {
		LocalStorageService local = new LocalStorageService(tmp);
		local.store(KEY, new ByteArrayInputStream(new byte[] { 1, 2, 3 }), 3, "image/png");
		assertThat(Files.readAllBytes(tmp.resolve(KEY))).containsExactly(1, 2, 3);
		assertThat(local.publicUrl(KEY)).isEqualTo("/media/" + KEY);

		assertThatThrownBy(() -> local.store("../outside.png", new ByteArrayInputStream(new byte[0]), 0, "image/png"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> local.store("products/1/../../x.png", new ByteArrayInputStream(new byte[0]), 0,
				"image/png"))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> local.store("products/1/shell.jsp", new ByteArrayInputStream(new byte[0]), 0,
				"image/png"))
			.isInstanceOf(IllegalArgumentException.class);

		local.delete(KEY);
		assertThat(local.find(KEY)).isEmpty();
	}

	/** Adaptador S3 contra um servidor S3-compatível real em container (sem credenciais de nuvem). */
	@Test
	void s3AdapterStoresAndDeletesObjects() {
		S3Client client = S3Client.builder()
			.endpointOverride(URI.create("http://" + s3mock.getHost() + ":" + s3mock.getMappedPort(9090)))
			.region(Region.US_EAST_1)
			.forcePathStyle(true)
			.credentialsProvider(StaticCredentialsProvider
				.create(AwsBasicCredentials.create("teste", "teste")))
			.build();
		client.createBucket(b -> b.bucket("gigaverse3d-test"));
		S3StorageService s3 = new S3StorageService(client, "gigaverse3d-test", "https://cdn.exemplo.com/");

		byte[] data = "conteudo".getBytes(StandardCharsets.UTF_8);
		s3.store(KEY, new ByteArrayInputStream(data), data.length, "image/png");
		var object = client.getObject(r -> r.bucket("gigaverse3d-test").key(KEY), ResponseTransformer.toBytes());
		assertThat(object.asByteArray()).isEqualTo(data);
		assertThat(object.response().contentType()).isEqualTo("image/png");
		assertThat(s3.publicUrl(KEY)).isEqualTo("https://cdn.exemplo.com/" + KEY);

		s3.delete(KEY);
		assertThatThrownBy(() -> client.getObject(r -> r.bucket("gigaverse3d-test").key(KEY)))
			.isInstanceOf(NoSuchKeyException.class);
	}

}
