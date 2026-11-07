package br.com.gigaverse3d.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/** Armazenamento em disco — somente para desenvolvimento/testes. */
public class LocalStorageService implements StorageService {

	private final Path root;

	public LocalStorageService(Path root) {
		this.root = root.toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.root);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Não foi possível criar o diretório de storage", e);
		}
	}

	/** Resolve a chave garantindo que o caminho final continua dentro da raiz. */
	Path resolve(String key) {
		Path path = root.resolve(StorageService.requireValidKey(key)).normalize();
		if (!path.startsWith(root)) {
			throw new IllegalArgumentException("Chave de armazenamento inválida");
		}
		return path;
	}

	@Override
	public void store(String key, InputStream content, long size, String contentType) {
		Path target = resolve(key);
		try {
			Files.createDirectories(target.getParent());
			Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Falha ao gravar arquivo", e);
		}
	}

	@Override
	public void delete(String key) {
		try {
			Files.deleteIfExists(resolve(key));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Falha ao remover arquivo", e);
		}
	}

	@Override
	public String publicUrl(String key) {
		return "/media/" + StorageService.requireValidKey(key);
	}

	public Optional<Path> find(String key) {
		Path path = resolve(key);
		return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
	}

}
