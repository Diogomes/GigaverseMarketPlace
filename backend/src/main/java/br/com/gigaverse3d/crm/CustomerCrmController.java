package br.com.gigaverse3d.crm;

import java.time.Clock;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.account.domain.User;
import br.com.gigaverse3d.common.domain.BusinessTime;
import br.com.gigaverse3d.common.web.PageResponse;
import br.com.gigaverse3d.crm.CrmDtos.AssignTagsRequest;
import br.com.gigaverse3d.crm.CrmDtos.CustomerDetail;
import br.com.gigaverse3d.crm.CrmDtos.CustomerListItem;
import br.com.gigaverse3d.crm.CrmDtos.CustomerSort;
import br.com.gigaverse3d.crm.CrmDtos.NoteRequest;
import br.com.gigaverse3d.crm.CrmDtos.NoteView;
import br.com.gigaverse3d.crm.CrmDtos.Segment;
import br.com.gigaverse3d.crm.CrmDtos.TagRequest;
import br.com.gigaverse3d.crm.CrmDtos.TagView;
import br.com.gigaverse3d.crm.CrmDtos.TagWithCount;
import br.com.gigaverse3d.crm.CustomerCrmService.CustomerFilter;

/**
 * CRM da equipe. ADMIN e OPERATOR consultam, anotam e etiquetam; excluir notas/etiquetas e
 * exportar são exclusivos do ADMIN (regras em SecurityConfig).
 */
@Tag(name = "Admin — clientes (CRM)")
@RestController
@RequestMapping("/api/v1/admin")
public class CustomerCrmController {

	private final CustomerCrmService service;

	private final Clock clock;

	public CustomerCrmController(CustomerCrmService service, Clock clock) {
		this.service = service;
		this.clock = clock;
	}

	@Operation(summary = "Lista clientes: busca por nome, e-mail ou id; filtros por status, etiqueta e segmento")
	@GetMapping("/customers")
	public PageResponse<CustomerListItem> list(@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(required = false) User.Status status, @RequestParam(required = false) Long tagId,
			@RequestParam(required = false) Segment segment,
			@RequestParam(defaultValue = "LAST_PURCHASE") CustomerSort sort,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(PageResponse.MAX_PAGE_SIZE) int size) {
		return service.list(new CustomerFilter(q, status, tagId, segment), sort, page, size);
	}

	@Operation(summary = "Exporta clientes em CSV (somente ADMIN; máx. 5000 linhas; auditado)")
	@GetMapping("/customers/export")
	public ResponseEntity<byte[]> export(@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(required = false) User.Status status, @RequestParam(required = false) Long tagId,
			@RequestParam(required = false) Segment segment) {
		return service.export(new CustomerFilter(q, status, tagId, segment))
			.toResponse("clientes-" + BusinessTime.today(clock) + ".csv");
	}

	@Operation(summary = "Perfil do cliente com resumo de compras e etiquetas")
	@GetMapping("/customers/{id}")
	public CustomerDetail detail(@PathVariable Long id) {
		return service.detail(id);
	}

	@Operation(summary = "Define as etiquetas do cliente (lista completa)")
	@PutMapping("/customers/{id}/tags")
	public List<TagView> assignTags(@PathVariable Long id, @Valid @RequestBody AssignTagsRequest req) {
		return service.assignTags(id, req.tagIds());
	}

	@Operation(summary = "Notas internas (nunca visíveis ao cliente)")
	@GetMapping("/customers/{id}/notes")
	public PageResponse<NoteView> notes(@PathVariable Long id, @RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(PageResponse.MAX_PAGE_SIZE) int size) {
		return service.notes(id, page, size);
	}

	@Operation(summary = "Adiciona nota interna")
	@PostMapping("/customers/{id}/notes")
	@ResponseStatus(HttpStatus.CREATED)
	public NoteView addNote(@PathVariable Long id, @Valid @RequestBody NoteRequest req) {
		return service.addNote(id, req.body());
	}

	@Operation(summary = "Remove nota interna (somente ADMIN; exclusão lógica)")
	@DeleteMapping("/customers/{id}/notes/{noteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteNote(@PathVariable Long id, @PathVariable Long noteId) {
		service.deleteNote(id, noteId);
	}

	@Operation(summary = "Etiquetas existentes com quantidade de clientes")
	@GetMapping("/customer-tags")
	public List<TagWithCount> tags() {
		return service.tags();
	}

	@Operation(summary = "Cria etiqueta")
	@PostMapping("/customer-tags")
	@ResponseStatus(HttpStatus.CREATED)
	public TagView createTag(@Valid @RequestBody TagRequest req) {
		return service.createTag(req.name());
	}

	@Operation(summary = "Exclui etiqueta e a remove de todos os clientes (somente ADMIN)")
	@DeleteMapping("/customer-tags/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTag(@PathVariable Long id) {
		service.deleteTag(id);
	}

}
