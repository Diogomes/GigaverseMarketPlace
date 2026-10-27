package br.com.gigaverse3d.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de dinheiro: BigDecimal com 2 casas, arredondamento bancário. Nunca double/float. */
public final class Money {

	public static final String CURRENCY = "BRL";

	public static final int SCALE = 2;

	private Money() {
	}

	public static BigDecimal of(String value) {
		return normalize(new BigDecimal(value));
	}

	public static BigDecimal normalize(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_EVEN);
	}

}
