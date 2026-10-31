package br.com.gigaverse3d.common.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Fuso da operação: períodos de relatório ("dia", "mês") são contados no horário de Recife. */
public final class BusinessTime {

	public static final ZoneId ZONE = ZoneId.of("America/Recife");

	private BusinessTime() {
	}

	/** Início do dia (inclusivo). */
	public static Instant startOf(LocalDate day) {
		return day.atStartOfDay(ZONE).toInstant();
	}

	/** Início do dia seguinte (exclusivo): intervalo [startOf(from), endOf(to)). */
	public static Instant endOf(LocalDate day) {
		return day.plusDays(1).atStartOfDay(ZONE).toInstant();
	}

	public static LocalDate today(Clock clock) {
		return LocalDate.now(clock.withZone(ZONE));
	}

}
