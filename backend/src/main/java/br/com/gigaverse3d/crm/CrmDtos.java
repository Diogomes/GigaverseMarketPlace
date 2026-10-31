package br.com.gigaverse3d.crm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.gigaverse3d.account.domain.User;

/**
 * DTOs do CRM. Dados mínimos: nada de endereço, telefone ou consentimentos na listagem; nenhum
 * atributo inferido (gênero, idade, renda…). Origem do cadastro não é exibida porque não é coletada.
 */
public final class CrmDtos {

	private CrmDtos() {
	}

	/** Segmentos calculados a partir dos pedidos (sem perfilamento sensível). */
	public enum Segment {

		@Schema(description = "Nenhum pedido pago") NO_PURCHASE,
		@Schema(description = "Dois ou mais pedidos pagos") REPEAT,
		@Schema(description = "Já comprou, mas não nos últimos 90 dias") LAPSED

	}

	public enum CustomerSort {
		LAST_PURCHASE, TOTAL_SPENT, CREATED_AT, NAME
	}

	public record TagView(Long id, String name) {
	}

	public record TagWithCount(Long id, String name, long customers) {
	}

	public record CustomerListItem(Long id, String name, String email, User.Status status, Instant createdAt,
			@Schema(description = "Pedidos pagos que não foram cancelados") long paidOrders,
			@Schema(description = "Σ total dos pedidos pagos que não foram cancelados") BigDecimal totalSpent,
			@Schema(description = "Data de pagamento do último pedido pago válido") Instant lastPurchaseAt,
			List<TagView> tags) {
	}

	public record CustomerDetail(Long id, String name, String email, String phone, User.Status status,
			Instant createdAt, Instant lastLoginAt,
			@Schema(description = "Aceite atual de e-mails de marketing (pode ser contatado com ofertas?)")
			boolean marketingOptIn,
			@Schema(description = "Todos os pedidos, em qualquer status") long orders, long paidOrders,
			BigDecimal totalSpent, BigDecimal averageTicket, Instant firstPurchaseAt, Instant lastPurchaseAt,
			List<TagView> tags) {
	}

	public record NoteView(Long id, String body, Long authorId, String authorName, Instant createdAt) {
	}

	public record NoteRequest(
			@NotBlank(message = "Escreva a nota.") @Size(max = 2000, message = "Máximo de 2000 caracteres.") String body) {
	}

	public record TagRequest(@NotBlank(message = "Informe o nome da etiqueta.")
	@Size(min = 2, max = 30, message = "Entre 2 e 30 caracteres.") String name) {
	}

	public record AssignTagsRequest(
			@NotNull(message = "Informe as etiquetas.") @Size(max = 20, message = "Máximo de 20 etiquetas por cliente.")
			List<@NotNull Long> tagIds) {
	}

}
