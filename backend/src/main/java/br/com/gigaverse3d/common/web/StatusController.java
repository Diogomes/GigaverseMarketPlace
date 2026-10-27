package br.com.gigaverse3d.common.web;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint público mínimo para provar a comunicação frontend ↔ backend. */
@RestController
@RequestMapping("/api/v1/public")
public class StatusController {

	public record StatusResponse(String service, String status, Instant timestamp) {
	}

	@GetMapping("/status")
	public StatusResponse status() {
		return new StatusResponse("gigaverse3d-api", "ok", Instant.now());
	}

}
