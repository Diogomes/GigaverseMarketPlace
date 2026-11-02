package br.com.gigaverse3d.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import br.com.gigaverse3d.common.domain.BusinessTime;
import br.com.gigaverse3d.common.domain.Money;

class MoneyAndTimeTest {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({ "10,10.00", "189.9,189.90", "0.125,0.12", "0.135,0.14", "0.005,0.00", "0.015,0.02", "99.999,100.00" })
	void normalizesToTwoDecimalsWithBankersRounding(String input, String expected) {
		assertThat(Money.of(input)).isEqualByComparingTo(expected);
		assertThat(Money.of(input).scale()).isEqualTo(2);
	}

	@Test
	void normalizeKeepsExactValues() {
		assertThat(Money.normalize(new BigDecimal("49.90"))).isEqualTo(new BigDecimal("49.90"));
		assertThat(Money.CURRENCY).isEqualTo("BRL");
	}

	@Test
	void businessDayUsesRecifeTimeZone() {
		LocalDate day = LocalDate.of(2026, 11, 2);
		// Recife = UTC−3, sem horário de verão
		assertThat(BusinessTime.startOf(day)).isEqualTo(Instant.parse("2026-11-02T03:00:00Z"));
		assertThat(BusinessTime.endOf(day)).isEqualTo(Instant.parse("2026-11-03T03:00:00Z"));
	}

	@Test
	void todayFollowsRecifeNotUtc() {
		// 01:30 UTC de 03/11 ainda é 22:30 de 02/11 em Recife
		Clock clock = Clock.fixed(Instant.parse("2026-11-03T01:30:00Z"), ZoneOffset.UTC);
		assertThat(BusinessTime.today(clock)).isEqualTo(LocalDate.of(2026, 11, 2));
	}

}
