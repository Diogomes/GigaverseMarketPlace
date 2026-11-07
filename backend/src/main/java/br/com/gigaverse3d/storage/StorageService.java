package br.com.gigaverse3d.storage;

import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Armazenamento de arquivos públicos (imagens de produto). Implementações: disco local
 * (desenvolvimento) e S3-compatível (produção). As chaves são sempre geradas pelo servidor.
 */
public interface StorageService {

	/** Formato aceito para chaves: evita path traversal e nomes controlados pelo usuário. */
	Pattern KEY_PATTERN = Pattern.compile("^products/[0-9]{1,19}/[0-9a-f-]{36}\\.(jpg|png|webp)$");

	void store(String key, InputStream content, long size, String contentType);

	void delete(String key);

	/** URL pública para o navegador (relativa à API no modo local; CDN/bucket no S3). */
	String publicUrl(String key);

	static String requireValidKey(String key) {
		if (key == null || !KEY_PATTERN.matcher(key).matches()) {
			throw new IllegalArgumentException("Chave de armazenamento inválida");
		}
		return key;
	}

}
