package br.com.gigaverse3d.common.web;

/** Exceções de negócio traduzidas em respostas HTTP pelo {@link ApiExceptionHandler}. */
public final class ApiExceptions {

	private ApiExceptions() {
	}

	/** 404 — recurso inexistente (ou não visível para quem pede). */
	public static class NotFoundException extends RuntimeException {

		public NotFoundException(String message) {
			super(message);
		}

	}

	/** 409 — conflito com o estado atual (duplicidade, versão desatualizada, dependências). */
	public static class ConflictException extends RuntimeException {

		public ConflictException(String message) {
			super(message);
		}

	}

	/** 400 — regra de negócio violada num campo específico. */
	public static class FieldValidationException extends RuntimeException {

		private final String field;

		public FieldValidationException(String field, String message) {
			super(message);
			this.field = field;
		}

		public String getField() {
			return field;
		}

	}

	/** 413 — conteúdo enviado acima do limite. */
	public static class PayloadTooLargeException extends RuntimeException {

		public PayloadTooLargeException(String message) {
			super(message);
		}

	}

	/** 401 — credenciais inválidas ou sessão ausente (mensagem genérica). */
	public static class UnauthorizedException extends RuntimeException {

		public UnauthorizedException(String message) {
			super(message);
		}

	}

	/** 429 — limite de tentativas excedido. */
	public static class TooManyRequestsException extends RuntimeException {

		private final long retryAfterSeconds;

		public TooManyRequestsException(long retryAfterSeconds) {
			super("Muitas tentativas. Aguarde alguns minutos e tente novamente.");
			this.retryAfterSeconds = retryAfterSeconds;
		}

		public long getRetryAfterSeconds() {
			return retryAfterSeconds;
		}

	}

}
