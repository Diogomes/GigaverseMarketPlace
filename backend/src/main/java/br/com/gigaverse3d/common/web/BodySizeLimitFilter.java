package br.com.gigaverse3d.common.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita o corpo das requisições JSON (uploads multipart têm limite próprio de 5 MB). Nenhum
 * formulário da loja chega perto de 256 KB; corpos maiores são abuso ou erro e são recusados
 * antes de chegar ao parser JSON.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class BodySizeLimitFilter extends OncePerRequestFilter {

	public static final long MAX_JSON_BYTES = 256 * 1024;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String type = request.getContentType();
		boolean multipart = type != null && type.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
		if (!multipart && request.getContentLengthLong() > MAX_JSON_BYTES) {
			response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
			response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.getWriter()
				.write("{\"type\":\"about:blank\",\"title\":\"Payload Too Large\",\"status\":413,"
						+ "\"detail\":\"Conteúdo da requisição acima do limite.\"}");
			return;
		}
		chain.doFilter(request, response);
	}

}
