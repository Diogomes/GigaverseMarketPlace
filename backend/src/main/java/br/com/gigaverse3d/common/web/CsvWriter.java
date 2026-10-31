package br.com.gigaverse3d.common.web;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import br.com.gigaverse3d.common.domain.BusinessTime;

/**
 * CSV para planilhas em pt-BR (separador ";", decimais com vírgula, UTF-8 com BOM).
 * Células que começam com =, +, -, @, tab ou CR recebem um apóstrofo: evita injeção de
 * fórmulas quando o arquivo é aberto no Excel/LibreOffice.
 */
public final class CsvWriter {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
		.withZone(BusinessTime.ZONE);

	private final StringBuilder out = new StringBuilder("\uFEFF");

	private int rows;

	public CsvWriter row(List<?> cells) {
		for (int i = 0; i < cells.size(); i++) {
			if (i > 0) {
				out.append(';');
			}
			out.append(cell(cells.get(i)));
		}
		out.append("\r\n");
		rows++;
		return this;
	}

	/** Linhas escritas, incluindo o cabeçalho. */
	public int rows() {
		return rows;
	}

	static String cell(Object value) {
		String text = switch (value) {
			case null -> "";
			case BigDecimal d -> d.toPlainString().replace('.', ',');
			case Instant t -> DATE_TIME.format(t);
			default -> value.toString();
		};
		if (!text.isEmpty() && "=+-@\t\r".indexOf(text.charAt(0)) >= 0 && !(value instanceof Number)) {
			text = "'" + text;
		}
		if (text.contains(";") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
			text = "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}

	/** Resposta de download: anexo, sem cache (dados pessoais). */
	public ResponseEntity<byte[]> toResponse(String filename) {
		return ResponseEntity.ok()
			.contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
			.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
			.header(HttpHeaders.CACHE_CONTROL, "no-store")
			.body(out.toString().getBytes(StandardCharsets.UTF_8));
	}

}
