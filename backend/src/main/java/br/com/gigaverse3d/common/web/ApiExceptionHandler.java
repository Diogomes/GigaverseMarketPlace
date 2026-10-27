package br.com.gigaverse3d.common.web;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.gigaverse3d.common.web.ApiExceptions.ConflictException;
import br.com.gigaverse3d.common.web.ApiExceptions.FieldValidationException;
import br.com.gigaverse3d.common.web.ApiExceptions.NotFoundException;
import br.com.gigaverse3d.common.web.ApiExceptions.PayloadTooLargeException;
import br.com.gigaverse3d.common.web.ApiExceptions.TooManyRequestsException;
import br.com.gigaverse3d.common.web.ApiExceptions.UnauthorizedException;
import br.com.gigaverse3d.inventory.domain.InsufficientStockException;

/**
 * Respostas de erro no formato RFC 9457 (application/problem+json), sem stack trace nem
 * detalhes internos. Erros de validação trazem a lista {@code errors: [{field, message}]}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private static ProblemDetail problem(HttpStatus status, String detail) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(status.getReasonPhrase());
		return pd;
	}

	private static ProblemDetail validationProblem(List<Map<String, String>> errors) {
		ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "Dados inválidos.");
		pd.setProperty("errors", errors);
		return pd;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
			.toList();
		return ResponseEntity.badRequest().body(validationProblem(errors));
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> errors = ex.getParameterValidationResults()
			.stream()
			.flatMap(r -> r.getResolvableErrors()
				.stream()
				.map(e -> Map.of("field",
						e instanceof FieldError fe ? fe.getField()
								: String.valueOf(r.getMethodParameter().getParameterName()),
						"message", String.valueOf(e.getDefaultMessage()))))
			.toList();
		return ResponseEntity.badRequest().body(validationProblem(errors));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest()
			.body(problem(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou com tipos incorretos."));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException ex) {
		List<Map<String, String>> errors = ex.getConstraintViolations()
			.stream()
			.map(v -> Map.of("field", String.valueOf(v.getPropertyPath()), "message", v.getMessage()))
			.toList();
		return ResponseEntity.badRequest().body(validationProblem(errors));
	}

	@ExceptionHandler(FieldValidationException.class)
	ResponseEntity<ProblemDetail> fieldValidation(FieldValidationException ex) {
		return ResponseEntity.badRequest()
			.body(validationProblem(List.of(Map.of("field", ex.getField(), "message", ex.getMessage()))));
	}

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<ProblemDetail> notFound(NotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(HttpStatus.NOT_FOUND, ex.getMessage()));
	}

	@ExceptionHandler({ ConflictException.class, InsufficientStockException.class })
	ResponseEntity<ProblemDetail> conflict(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(HttpStatus.CONFLICT, ex.getMessage()));
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<ProblemDetail> optimisticLock(ObjectOptimisticLockingFailureException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(problem(HttpStatus.CONFLICT, "O registro foi alterado por outra pessoa. Recarregue e tente novamente."));
	}

	/** Última defesa para unicidade/constraints que escaparam das checagens do serviço. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ProblemDetail> dataIntegrity(DataIntegrityViolationException ex) {
		log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(problem(HttpStatus.CONFLICT, "A operação viola uma regra de integridade dos dados."));
	}

	@ExceptionHandler(UnauthorizedException.class)
	ResponseEntity<ProblemDetail> unauthorized(UnauthorizedException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem(HttpStatus.UNAUTHORIZED, ex.getMessage()));
	}

	@ExceptionHandler(TooManyRequestsException.class)
	ResponseEntity<ProblemDetail> tooManyRequests(TooManyRequestsException ex) {
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
			.body(problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
	}

	@ExceptionHandler(PayloadTooLargeException.class)
	ResponseEntity<ProblemDetail> tooLarge(PayloadTooLargeException ex) {
		return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
			.body(problem(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage()));
	}

}
