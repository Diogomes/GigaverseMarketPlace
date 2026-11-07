package br.com.gigaverse3d.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração de armazenamento ({@code app.storage.*}).
 * Credenciais S3 NÃO ficam aqui: vêm da cadeia padrão do AWS SDK (variáveis
 * AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY, perfil ou papel da instância).
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String type, Local local, S3 s3) {

	public record Local(String rootDir) {
	}

	public record S3(String bucket, String region, String endpoint, boolean pathStyleAccess, String publicBaseUrl) {
	}

}
