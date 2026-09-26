package br.com.gestaocondominio.api.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), path);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(jakarta.persistence.EntityNotFoundException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), path);
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        String customMessage = "Credenciais inválidas. Por favor, verifique seu e-mail e senha.";
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED, customMessage, path);
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        String customMessage = "Acesso negado. Você não tem permissão para executar esta ação.";
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN, customMessage, path);
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado no servidor. Por favor, tente novamente mais tarde.",
                path
        );

        System.err.println("Erro interno do servidor em " + path + ": " + ex.getMessage());
        ex.printStackTrace();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Os erros que o próprio Spring trata (validação de campos, {@code ResponseStatusException}, corpo ilegível,
     * método não aceito) saem no mesmo formato dos demais, com a mensagem pronta para a tela mostrar.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String path = request instanceof ServletWebRequest servlet
                ? servlet.getRequest().getRequestURI()
                : request.getDescription(false).replace("uri=", "");
        return new ResponseEntity<>(new ErrorResponse(status, mensagemDe(ex, status), path), headers, status);
    }

    private static String mensagemDe(Exception ex, HttpStatus status) {
        if (ex instanceof MethodArgumentNotValidException invalido) {
            return invalido.getBindingResult().getAllErrors().stream()
                    .map(erro -> erro.getDefaultMessage())
                    .distinct()
                    .reduce((a, b) -> a + " " + b)
                    .orElse("Dados inválidos.");
        }
        if (ex instanceof org.springframework.web.ErrorResponse resposta
                && resposta.getBody().getDetail() != null && status != HttpStatus.INTERNAL_SERVER_ERROR) {
            return resposta.getBody().getDetail();
        }
        return switch (status) {
            case BAD_REQUEST -> "Dados inválidos.";
            case NOT_FOUND -> "Registro não encontrado.";
            case METHOD_NOT_ALLOWED -> "Operação não permitida neste endereço.";
            default -> "Ocorreu um erro inesperado no servidor. Por favor, tente novamente mais tarde.";
        };
    }
}
