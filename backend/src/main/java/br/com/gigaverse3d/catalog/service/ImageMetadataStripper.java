package br.com.gigaverse3d.catalog.service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

import br.com.gigaverse3d.catalog.service.ImageValidator.ImageType;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;

/**
 * Remove metadados das imagens enviadas (EXIF com GPS/câmera/data, XMP, comentários) sem
 * recomprimir os pixels: copia só os blocos necessários para exibir a imagem.
 *
 * - JPEG: descarta segmentos APP1–APP15 (exceto o perfil de cor ICC em APP2) e COM;
 * - PNG: descarta os chunks de texto/metadados (tEXt, zTXt, iTXt, eXIf, tIME);
 * - WebP: descarta os chunks EXIF e XMP e limpa os respectivos bits do cabeçalho VP8X.
 *
 * Estrutura inválida é recusada (a assinatura já foi conferida pelo {@link ImageValidator}).
 */
public final class ImageMetadataStripper {

	private static final Set<String> PNG_DROP = Set.of("tEXt", "zTXt", "iTXt", "eXIf", "tIME");

	private static final String INVALID = "Imagem corrompida ou em formato não suportado.";

	private ImageMetadataStripper() {
	}

	public static byte[] strip(byte[] bytes, ImageType type) {
		try {
			return switch (type) {
				case JPEG -> jpeg(bytes);
				case PNG -> png(bytes);
				case WEBP -> webp(bytes);
			};
		}
		catch (IndexOutOfBoundsException | IllegalArgumentException e) {
			throw new FieldValidationException("file", INVALID);
		}
	}

	static byte[] jpeg(byte[] in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(in.length);
		out.write(in, 0, 2); // SOI
		int pos = 2;
		while (pos < in.length) {
			if ((in[pos] & 0xFF) != 0xFF) {
				throw new IllegalArgumentException("marcador JPEG esperado");
			}
			int marker = in[pos + 1] & 0xFF;
			if (marker == 0xFF) { // preenchimento
				pos++;
				continue;
			}
			if (marker == 0xDA || marker == 0xD9) { // SOS: dados comprimidos até o fim; EOI
				out.write(in, pos, in.length - pos);
				return out.toByteArray();
			}
			if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) { // marcadores sem tamanho
				out.write(in, pos, 2);
				pos += 2;
				continue;
			}
			int length = ((in[pos + 2] & 0xFF) << 8) | (in[pos + 3] & 0xFF);
			if (length < 2 || pos + 2 + length > in.length) {
				throw new IllegalArgumentException("segmento JPEG inválido");
			}
			boolean app = marker >= 0xE1 && marker <= 0xEF;
			boolean icc = marker == 0xE2 && startsWith(in, pos + 4, "ICC_PROFILE");
			if (!((app && !icc) || marker == 0xFE)) {
				out.write(in, pos, 2 + length);
			}
			pos += 2 + length;
		}
		throw new IllegalArgumentException("JPEG sem dados de imagem");
	}

	static byte[] png(byte[] in) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(in.length);
		out.write(in, 0, 8); // assinatura
		int pos = 8;
		while (pos < in.length) {
			int length = ByteBuffer.wrap(in, pos, 4).getInt();
			if (length < 0 || pos + 12 + length > in.length) {
				throw new IllegalArgumentException("chunk PNG inválido");
			}
			String chunk = new String(in, pos + 4, 4, StandardCharsets.US_ASCII);
			int total = 12 + length; // tamanho + tipo + dados + CRC
			if (!PNG_DROP.contains(chunk)) {
				out.write(in, pos, total);
			}
			pos += total;
			if (chunk.equals("IEND")) {
				return out.toByteArray();
			}
		}
		throw new IllegalArgumentException("PNG sem IEND");
	}

	static byte[] webp(byte[] in) {
		ByteArrayOutputStream body = new ByteArrayOutputStream(in.length);
		body.write(in, 8, 4); // "WEBP"
		int pos = 12;
		while (pos + 8 <= in.length) {
			String chunk = new String(in, pos, 4, StandardCharsets.US_ASCII);
			int size = ByteBuffer.wrap(in, pos + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
			int padded = size + (size & 1);
			if (size < 0 || pos + 8 + padded > in.length) {
				throw new IllegalArgumentException("chunk WebP inválido");
			}
			if (chunk.equals("VP8X")) {
				byte[] copy = Arrays.copyOfRange(in, pos, pos + 8 + padded);
				copy[8] = (byte) (copy[8] & ~0x0C); // limpa os bits "tem EXIF" (0x08) e "tem XMP" (0x04)
				body.write(copy, 0, copy.length);
			}
			else if (!chunk.equals("EXIF") && !chunk.equals("XMP ")) {
				body.write(in, pos, 8 + padded);
			}
			pos += 8 + padded;
		}
		byte[] payload = body.toByteArray();
		ByteBuffer out = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
		out.put(in, 0, 4).putInt(payload.length).put(payload);
		return out.array();
	}

	private static boolean startsWith(byte[] in, int offset, String ascii) {
		byte[] prefix = ascii.getBytes(StandardCharsets.US_ASCII);
		if (offset + prefix.length > in.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (in[offset + i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

}
