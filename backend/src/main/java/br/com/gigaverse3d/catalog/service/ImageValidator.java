package br.com.gigaverse3d.catalog.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Component;

import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.PayloadTooLargeException;

/**
 * Valida uploads de imagem sem confiar no cliente: limite de tamanho, extensão permitida,
 * tipo REAL detectado pelos bytes iniciais (assinatura) e coerência entre os dois.
 * Dimensões são lidas só do cabeçalho (não decodifica a imagem inteira — evita "bombas").
 */
@Component
public class ImageValidator {

	public static final long MAX_BYTES = 5L * 1024 * 1024;

	public static final int MAX_DIMENSION = 8000;

	public enum ImageType {
		JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), WEBP("webp", "image/webp");

		public final String extension;

		public final String contentType;

		ImageType(String extension, String contentType) {
			this.extension = extension;
			this.contentType = contentType;
		}
	}

	public record ValidatedImage(ImageType type, Integer width, Integer height) {
	}

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

	public ValidatedImage validate(String originalFilename, byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			throw new FieldValidationException("file", "Selecione um arquivo de imagem.");
		}
		if (bytes.length > MAX_BYTES) {
			throw new PayloadTooLargeException("A imagem deve ter no máximo 5 MB.");
		}
		String ext = extension(originalFilename);
		if (!ALLOWED_EXTENSIONS.contains(ext)) {
			throw new FieldValidationException("file", "Formatos aceitos: JPG, PNG ou WebP.");
		}
		ImageType detected = detect(bytes);
		if (detected == null) {
			throw new FieldValidationException("file", "O conteúdo do arquivo não é uma imagem JPG, PNG ou WebP válida.");
		}
		boolean extensionMatches = switch (detected) {
			case JPEG -> ext.equals("jpg") || ext.equals("jpeg");
			case PNG -> ext.equals("png");
			case WEBP -> ext.equals("webp");
		};
		if (!extensionMatches) {
			throw new FieldValidationException("file", "A extensão do arquivo não corresponde ao seu conteúdo.");
		}
		Integer[] size = detected == ImageType.WEBP ? new Integer[] { null, null } : readDimensions(bytes);
		if (size[0] != null && (size[0] > MAX_DIMENSION || size[1] > MAX_DIMENSION)) {
			throw new FieldValidationException("file", "Imagem muito grande (máximo de 8000 px por lado).");
		}
		return new ValidatedImage(detected, size[0], size[1]);
	}

	static String extension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
	}

	/** Assinaturas: JPEG FF D8 FF; PNG 89 50 4E 47 0D 0A 1A 0A; WebP "RIFF"????"WEBP". */
	static ImageType detect(byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return ImageType.JPEG;
		}
		if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' && b[4] == 0x0D
				&& b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
			return ImageType.PNG;
		}
		if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E'
				&& b[10] == 'B' && b[11] == 'P') {
			return ImageType.WEBP;
		}
		return null;
	}

	private static Integer[] readDimensions(byte[] bytes) {
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) {
				throw new FieldValidationException("file", "Imagem corrompida ou ilegível.");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(in, true, true);
				return new Integer[] { reader.getWidth(0), reader.getHeight(0) };
			}
			finally {
				reader.dispose();
			}
		}
		catch (IOException e) {
			throw new FieldValidationException("file", "Imagem corrompida ou ilegível.");
		}
	}

}
