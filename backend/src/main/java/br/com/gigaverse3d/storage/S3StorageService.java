package br.com.gigaverse3d.storage;

import java.io.InputStream;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Adaptador S3-compatível (AWS S3, Cloudflare R2, Backblaze B2, MinIO…).
 * Testado contra MinIO (Testcontainers); a escolha do provedor real é a decisão D-P04.
 */
public class S3StorageService implements StorageService {

	private final S3Client client;

	private final String bucket;

	private final String publicBaseUrl;

	public S3StorageService(S3Client client, String bucket, String publicBaseUrl) {
		this.client = client;
		this.bucket = bucket;
		this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
				: publicBaseUrl;
	}

	@Override
	public void store(String key, InputStream content, long size, String contentType) {
		client.putObject(PutObjectRequest.builder()
			.bucket(bucket)
			.key(StorageService.requireValidKey(key))
			.contentType(contentType)
			.cacheControl("public, max-age=31536000, immutable")
			.build(), RequestBody.fromInputStream(content, size));
	}

	@Override
	public void delete(String key) {
		client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(StorageService.requireValidKey(key)).build());
	}

	@Override
	public String publicUrl(String key) {
		return publicBaseUrl + "/" + StorageService.requireValidKey(key);
	}

}
