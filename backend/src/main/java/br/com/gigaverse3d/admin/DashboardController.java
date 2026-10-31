package br.com.gigaverse3d.admin;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import br.com.gigaverse3d.admin.DashboardDtos.DashboardResponse;
import br.com.gigaverse3d.common.domain.BusinessTime;

@Tag(name = "Admin — dashboard")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class DashboardController {

	private final DashboardService service;

	private final Clock clock;

	public DashboardController(DashboardService service, Clock clock) {
		this.service = service;
		this.clock = clock;
	}

	@Operation(summary = "Métricas do período (padrão: últimos 30 dias, fuso America/Recife; máx. 366 dias)")
	@GetMapping
	public DashboardResponse dashboard(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		LocalDate end = to != null ? to : BusinessTime.today(clock);
		LocalDate start = from != null ? from : end.minusDays(29);
		return service.dashboard(start, end);
	}

}
