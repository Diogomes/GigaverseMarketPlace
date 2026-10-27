package br.com.gigaverse3d.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import br.com.gigaverse3d.catalog.domain.Product;
import br.com.gigaverse3d.persistence.PostgresIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

@WithMockUser(roles = "ADMIN")
class ProductImageApiTest extends PostgresIntegrationTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	CatalogFixtures fixtures;

	@Autowired
	JsonMapper json;

	Product product;

	@BeforeEach
	void setUp() {
		product = fixtures.activeProduct(fixtures.category(CatalogFixtures.unique("img")), "Com imagem", "10.00", 1);
	}

	static byte[] image(String format, int w, int h) throws Exception {
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, format, out);
		return out.toByteArray();
	}

	private ResultActions upload(String filename, String contentType, byte[] bytes) throws Exception {
		return mvc.perform(multipart("/api/v1/admin/products/{id}/images", product.getId())
			.file(new MockMultipartFile("file", filename, contentType, bytes))
			.param("altText", "Foto do produto de teste")
			.with(csrf()));
	}

	@Test
	void uploadsPngStoresUnderServerGeneratedNameAndServesSafely() throws Exception {
		String body = upload("../../etc/passwd.png", "image/png", image("png", 40, 30))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.images[0].contentType").value("image/png"))
			.andExpect(jsonPath("$.images[0].width").value(40))
			.andExpect(jsonPath("$.images[0].height").value(30))
			.andReturn().getResponse().getContentAsString();
		String url = json.readTree(body).get("images").get(0).get("url").asString();
		assertThat(url).matches("/media/products/" + product.getId() + "/[0-9a-f-]{36}\\.png").doesNotContain("passwd");

		mvc.perform(get(url))
			.andExpect(status().isOk())
			.andExpect(content().contentType("image/png"))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andExpect(header().string("Content-Security-Policy", "default-src 'none'; sandbox"));

		// exposto também no catálogo público
		mvc.perform(get("/api/v1/public/products/{slug}", product.getSlug()))
			.andExpect(jsonPath("$.images[0].url").value(url))
			.andExpect(jsonPath("$.images[0].alt").value("Foto do produto de teste"));
	}

	@Test
	void acceptsJpegWithJpegExtension() throws Exception {
		upload("foto.JPEG", "image/jpeg", image("jpg", 10, 10)).andExpect(status().isCreated());
	}

	@Test
	void rejectsTextDisguisedAsImage() throws Exception {
		upload("script.png", "image/png", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message")
				.value("O conteúdo do arquivo não é uma imagem JPG, PNG ou WebP válida."));
	}

	@Test
	void rejectsDisallowedExtensions() throws Exception {
		upload("vetor.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8))
			.andExpect(status().isBadRequest());
		upload("programa.exe", "application/octet-stream", image("png", 5, 5)).andExpect(status().isBadRequest());
	}

	@Test
	void rejectsExtensionThatDoesNotMatchContent() throws Exception {
		upload("foto.jpg", "image/jpeg", image("png", 5, 5))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].message").value("A extensão do arquivo não corresponde ao seu conteúdo."));
	}

	@Test
	void rejectsFilesOverFiveMegabytes() throws Exception {
		byte[] big = new byte[5 * 1024 * 1024 + 1];
		System.arraycopy(image("png", 5, 5), 0, big, 0, 8);
		upload("grande.png", "image/png", big).andExpect(status().isContentTooLarge());
	}

	@Test
	void requiresAltText() throws Exception {
		mvc.perform(multipart("/api/v1/admin/products/{id}/images", product.getId())
			.file(new MockMultipartFile("file", "a.png", "image/png", image("png", 5, 5)))
			.param("altText", " ")
			.with(csrf())).andExpect(status().isBadRequest());
	}

	@Test
	void limitsImagesPerProduct() throws Exception {
		for (int i = 0; i < 8; i++) {
			upload("f" + i + ".png", "image/png", image("png", 4, 4)).andExpect(status().isCreated());
		}
		upload("extra.png", "image/png", image("png", 4, 4)).andExpect(status().isConflict());
	}

	@Test
	void deletesImageAndFile() throws Exception {
		String body = upload("x.png", "image/png", image("png", 4, 4)).andReturn().getResponse().getContentAsString();
		var img = json.readTree(body).get("images").get(0);
		mvc.perform(delete("/api/v1/admin/products/{id}/images/{imageId}", product.getId(), img.get("id").asLong())
			.with(csrf())).andExpect(status().isNoContent());
		mvc.perform(get(img.get("url").asString())).andExpect(status().isNotFound());
	}

	@Test
	void mediaRouteRejectsTraversalAttempts() throws Exception {
		mvc.perform(get("/media/products/1/..%2F..%2Fapplication.yml")).andExpect(status().is4xxClientError());
		mvc.perform(get("/media/products/abc/arquivo.png")).andExpect(status().is4xxClientError());
	}

}
