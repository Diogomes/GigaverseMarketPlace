package br.com.gigaverse3d.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import br.com.gigaverse3d.catalog.service.ImageMetadataStripper;
import br.com.gigaverse3d.catalog.service.ImageValidator.ImageType;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;

class ImageMetadataStripperTest {

	private static final String GPS = "GPS-LATITUDE-8.05S";

	private static byte[] encode(String format) throws IOException {
		BufferedImage img = new BufferedImage(12, 7, BufferedImage.TYPE_INT_RGB);
		img.setRGB(3, 3, 0x2a78d6);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, format, out);
		return out.toByteArray();
	}

	private static String ascii(byte[] b) {
		return new String(b, StandardCharsets.ISO_8859_1);
	}

	@Test
	void jpegLosesExifAndCommentButKeepsPixelsAndIcc() throws IOException {
		byte[] jpeg = encode("jpg");
		byte[] exif = ("Exif\0\0" + GPS).getBytes(StandardCharsets.ISO_8859_1);
		byte[] icc = "ICC_PROFILE\0perfil".getBytes(StandardCharsets.ISO_8859_1);
		byte[] comment = "camera do celular".getBytes(StandardCharsets.ISO_8859_1);
		ByteArrayOutputStream withMeta = new ByteArrayOutputStream();
		withMeta.write(jpeg, 0, 2);
		segment(withMeta, 0xE1, exif);
		segment(withMeta, 0xE2, icc);
		segment(withMeta, 0xFE, comment);
		withMeta.write(jpeg, 2, jpeg.length - 2);

		byte[] clean = ImageMetadataStripper.strip(withMeta.toByteArray(), ImageType.JPEG);
		assertThat(ascii(clean)).doesNotContain(GPS).doesNotContain("camera do celular").contains("ICC_PROFILE");

		// sem o ICC fictício (que não é um perfil válido), a imagem limpa decodifica com as mesmas dimensões
		ByteArrayOutputStream exifOnly = new ByteArrayOutputStream();
		exifOnly.write(jpeg, 0, 2);
		segment(exifOnly, 0xE1, exif);
		segment(exifOnly, 0xFE, comment);
		exifOnly.write(jpeg, 2, jpeg.length - 2);
		byte[] cleanExifOnly = ImageMetadataStripper.strip(exifOnly.toByteArray(), ImageType.JPEG);
		assertThat(cleanExifOnly).isEqualTo(jpeg);
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(cleanExifOnly));
		assertThat(decoded.getWidth()).isEqualTo(12);
		assertThat(decoded.getHeight()).isEqualTo(7);
	}

	@Test
	void pngLosesTextChunksAndStillDecodes() throws IOException {
		byte[] png = encode("png");
		// insere tEXt logo após o IHDR (8 + 25 bytes)
		ByteArrayOutputStream withMeta = new ByteArrayOutputStream();
		withMeta.write(png, 0, 33);
		pngChunk(withMeta, "tEXt", ("Location\0" + GPS).getBytes(StandardCharsets.ISO_8859_1));
		withMeta.write(png, 33, png.length - 33);

		byte[] clean = ImageMetadataStripper.strip(withMeta.toByteArray(), ImageType.PNG);

		assertThat(ascii(clean)).doesNotContain(GPS).doesNotContain("tEXt");
		assertThat(clean).isEqualTo(png);
		assertThat(ImageIO.read(new ByteArrayInputStream(clean)).getWidth()).isEqualTo(12);
	}

	@Test
	void webpLosesExifAndXmpChunksAndFlags() {
		byte[] vp8x = new byte[10];
		vp8x[0] = 0x0C | 0x10; // EXIF + XMP + alfa
		byte[] image = "dados-da-imagem".getBytes(StandardCharsets.ISO_8859_1); // tamanho ímpar → preenchimento
		ByteArrayOutputStream chunks = new ByteArrayOutputStream();
		webpChunk(chunks, "VP8X", vp8x);
		webpChunk(chunks, "VP8L", image);
		webpChunk(chunks, "EXIF", GPS.getBytes(StandardCharsets.ISO_8859_1));
		webpChunk(chunks, "XMP ", "<x:xmpmeta/>".getBytes(StandardCharsets.ISO_8859_1));
		byte[] riff = riff(chunks.toByteArray());

		byte[] clean = ImageMetadataStripper.strip(riff, ImageType.WEBP);

		assertThat(ascii(clean)).doesNotContain(GPS).doesNotContain("XMP ").contains("VP8L").contains("dados-da-imagem");
		assertThat(clean[20]).isEqualTo((byte) 0x10); // só o bit de alfa permanece
		int declared = ByteBuffer.wrap(clean, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
		assertThat(declared).isEqualTo(clean.length - 8);
	}

	@Test
	void truncatedFilesAreRejected() throws IOException {
		byte[] jpeg = encode("jpg");
		byte[] truncated = java.util.Arrays.copyOf(jpeg, 30);
		assertThatThrownBy(() -> ImageMetadataStripper.strip(truncated, ImageType.JPEG))
			.isInstanceOf(FieldValidationException.class);
		byte[] png = java.util.Arrays.copyOf(encode("png"), 40);
		assertThatThrownBy(() -> ImageMetadataStripper.strip(png, ImageType.PNG))
			.isInstanceOf(FieldValidationException.class);
	}

	private static void segment(ByteArrayOutputStream out, int marker, byte[] data) {
		int len = data.length + 2;
		out.write(0xFF);
		out.write(marker);
		out.write(len >> 8);
		out.write(len & 0xFF);
		out.writeBytes(data);
	}

	private static void pngChunk(ByteArrayOutputStream out, String type, byte[] data) {
		byte[] t = type.getBytes(StandardCharsets.US_ASCII);
		out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
		out.writeBytes(t);
		out.writeBytes(data);
		CRC32 crc = new CRC32();
		crc.update(t);
		crc.update(data);
		out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
	}

	private static void webpChunk(ByteArrayOutputStream out, String type, byte[] data) {
		out.writeBytes(type.getBytes(StandardCharsets.US_ASCII));
		out.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data.length).array());
		out.writeBytes(data);
		if ((data.length & 1) == 1) {
			out.write(0);
		}
	}

	private static byte[] riff(byte[] chunks) {
		ByteBuffer b = ByteBuffer.allocate(12 + chunks.length).order(ByteOrder.LITTLE_ENDIAN);
		b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(4 + chunks.length);
		b.put("WEBP".getBytes(StandardCharsets.US_ASCII)).put(chunks);
		return b.array();
	}

}
