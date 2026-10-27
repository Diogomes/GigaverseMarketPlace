package br.com.gigaverse3d.common.text;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {

	public static final String PATTERN = "^[a-z0-9]+(-[a-z0-9]+)*$";

	private Slugs() {
	}

	/** "Luminária Lua 3D!" -> "luminaria-lua-3d" */
	public static String from(String text) {
		String noAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
		return noAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
	}

}
