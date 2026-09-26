package br.com.gestaocondominio.api.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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

    /** O pedido esbarra no estado atual do registro (ex.: inativar condomínio que ainda tem unidades). */
    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErrorResponse> handleConflitoException(ConflitoException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), path), HttpStatus.CONFLICT);
    }

    /** Restrição do banco (registro duplicado, ou em uso por outro): 409 em vez de 500, sem o texto técnico do banco. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                      WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        String mensagem = "Não foi possível gravar: o registro repete um que já existe ou está em uso por outro cadastro.";
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.CONFLICT, mensagem, path), HttpStatus.CONFLICT);
    }

    /**
     * Quem lança {@code AccessDeniedException} com uma explicação própria tem a mensagem repassada; a recusa genérica
     * do Spring ("Access Denied", do {@code @PreAuthorize}) sai com o texto padrão.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        String customMessage = ex.getMessage() != null && !ex.getMessage().isBlank()
                && !"Access Denied".equals(ex.getMessage())
                ? ex.getMessage()
                : "Acesso negado. Você não tem permissão para executar esta ação.";
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

    /**
     * Mensagem em português para cada caso. Só o texto da {@code ResponseStatusException}, que é escrito pelo próprio
     * sistema, é repassado; o das demais exceções do Spring vem em inglês e com detalhe técnico.
     */
    private static String mensagemDe(Exception ex, HttpStatus status) {
        if (ex instanceof MethodArgumentNotValidException invalido) {
            return invalido.getBindingResult().getAllErrors().stream()
                    .map(erro -> erro.getDefaultMessage())
                    .distinct()
                    .reduce((a, b) -> a + " " + b)
                    .orElse("Dados inválidos.");
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return "O arquivo passa do limite de 10 MB.";
        }
        if (ex instanceof MissingServletRequestPartException faltando) {
            return "Envie a parte \"" + faltando.getRequestPartName() + "\" do formulário.";
        }
        if (ex instanceof MissingServletRequestParameterException faltando) {
            return "Informe o parâmetro \"" + faltando.getParameterName() + "\".";
        }
        if (ex instanceof TypeMismatchException invalido && invalido.getPropertyName() != null) {
            return "Valor inválido para \"" + invalido.getPropertyName() + "\".";
        }
        if (ex instanceof HttpMessageNotReadableException ilegivel) {
            if (ilegivel.getCause() instanceof InvalidFormatException formato && !formato.getPath().isEmpty()
                    && formato.getPath().get(formato.getPath().size() - 1).getFieldName() != null) {
                return "Valor inválido para \"" + formato.getPath().get(formato.getPath().size() - 1).getFieldName()
                        + "\".";
            }
            return "O conteúdo enviado não pôde ser lido. Confira o formato dos dados.";
        }
        if (ex instanceof org.springframework.web.server.ResponseStatusException resposta
                && resposta.getReason() != null && status != HttpStatus.INTERNAL_SERVER_ERROR) {
            return resposta.getReason();
        }
        return switch (status) {
            case BAD_REQUEST -> "Dados inválidos.";
            case NOT_FOUND -> "Registro não encontrado.";
            case UNSUPPORTED_MEDIA_TYPE -> "Formato de envio não aceito neste endereço.";
            case METHOD_NOT_ALLOWED -> "Operação não permitida neste endereço.";
            default -> "Ocorreu um erro inesperado no servidor. Por favor, tente novamente mais tarde.";
        };
    }
}
