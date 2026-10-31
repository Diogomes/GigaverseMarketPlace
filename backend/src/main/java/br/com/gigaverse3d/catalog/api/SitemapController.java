package br.com.gigaverse3d.catalog.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import br.com.gigaverse3d.catalog.repository.CategoryRepository;
import br.com.gigaverse3d.catalog.repository.ProductRepository;

/** sitemap.xml com páginas institucionais, categorias e produtos ATIVOS (para buscadores). */
@RestController
public class SitemapController {

	private static final String[] STATIC_PAGES = { "/", "/produtos", "/colecoes", "/personalizados", "/sobre",
			"/contato", "/perguntas-frequentes", "/politica-comercial", "/privacidade", "/termos" };

	private final ProductRepository products;

	private final CategoryRepository categories;

	private final String baseUrl;

	public SitemapController(ProductRepository products, CategoryRepository categories,
			@Value("${app.auth.frontend-base-url}") String baseUrl) {
		this.products = products;
		this.categories = categories;
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	@GetMapping(path = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
	@Transactional(readOnly = true)
	public String sitemap() {
		StringBuilder xml = new StringBuilder("""
				<?xml version="1.0" encoding="UTF-8"?>
				<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
				""");
		for (String page : STATIC_PAGES) {
			url(xml, page);
		}
		categories.findByActiveTrueOrderBySortOrderAscNameAsc().forEach(c -> url(xml, "/produtos?categoria=" + c.getSlug()));
		products.findActiveSlugs().forEach(slug -> url(xml, "/produtos/" + slug));
		return xml.append("</urlset>\n").toString();
	}

	private void url(StringBuilder xml, String path) {
		xml.append("  <url><loc>").append(HtmlUtils.htmlEscape(baseUrl + path)).append("</loc></url>\n");
	}

}
