package br.com.gigaverse3d.storage;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;

/**
 * Serve imagens do storage local (só existe no modo local; em produção o S3/CDN serve direto).
 * Os parâmetros de rota aceitam apenas IDs numéricos e nomes UUID gerados pelo servidor.
 */
@RestController
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class MediaController {

	private final LocalStorageService storage;

	public MediaController(LocalStorageService storage) {
		this.storage = storage;
	}

	@GetMapping("/media/products/{productId:[0-9]{1,19}}/{file:[0-9a-f-]{36}\\.(?:jpg|png|webp)}")
	ResponseEntity<Resource> productImage(@PathVariable String productId, @PathVariable String file) {
		String key = "products/" + productId + "/" + file;
		Path path = storage.find(key).orElseThrow(() -> new NotFoundException("Imagem não encontrada."));
		MediaType type = switch (file.substring(file.lastIndexOf('.') + 1)) {
			case "png" -> MediaType.IMAGE_PNG;
			case "webp" -> MediaType.parseMediaType("image/webp");
			default -> MediaType.IMAGE_JPEG;
		};
		return ResponseEntity.ok()
			.contentType(type)
			.cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
			.header("X-Content-Type-Options", "nosniff")
			.header("Content-Security-Policy", "default-src 'none'; sandbox")
			.header("Content-Disposition", "inline")
			.body(new FileSystemResource(path));
	}

}
