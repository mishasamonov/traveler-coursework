package ua.edu.sumdu.traveler.web.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.ForbiddenOperationException;
import ua.edu.sumdu.traveler.core.error.NotFoundException;
import ua.edu.sumdu.traveler.core.error.ValidationException;

/**
 * Turns exceptions that controllers did not handle themselves into error pages with the proper
 * HTTP status. Expected business errors are logged as WARN, unexpected ones as ERROR with an
 * incident number that the user can report.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  public ModelAndView notFound(NotFoundException e, HttpServletRequest request) {
    log.warn("404 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
    return page("error/404", HttpStatus.NOT_FOUND, e.getMessage());
  }

  /** A guest is sent to the login page, a signed-in user gets "access denied". */
  @ExceptionHandler(ForbiddenOperationException.class)
  public ModelAndView forbidden(ForbiddenOperationException e, HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth instanceof AnonymousAuthenticationToken) {
      return new ModelAndView("redirect:/login");
    }
    log.warn("403 {} {} user={}: {}", request.getMethod(), request.getRequestURI(),
        auth.getName(), e.getMessage());
    return page("error/403", HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler({ConflictException.class, OptimisticLockingFailureException.class})
  public ModelAndView conflict(RuntimeException e, HttpServletRequest request) {
    log.warn("409 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
    String message = e instanceof ConflictException ? e.getMessage()
        : "Запис уже змінено іншим користувачем. Оновіть сторінку й повторіть дію.";
    return page("error/409", HttpStatus.CONFLICT, message);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ModelAndView integrity(DataIntegrityViolationException e, HttpServletRequest request) {
    log.warn("409 {} {}: database constraint {}", request.getMethod(), request.getRequestURI(),
        e.getMostSpecificCause().getMessage());
    return page("error/409", HttpStatus.CONFLICT,
        "Дані порушують обмеження бази даних (наприклад, унікальність позиції в маршруті).");
  }

  @ExceptionHandler({ValidationException.class, MethodArgumentTypeMismatchException.class,
      MissingServletRequestParameterException.class})
  public ModelAndView badRequest(Exception e, HttpServletRequest request) {
    log.warn("400 {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
    String message = e instanceof ValidationException ? e.getMessage()
        : "Некоректні параметри запиту.";
    return page("error/400", HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView unexpected(Exception e, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    // Security exceptions must reach the Spring Security filters (login redirect, 403).
    if (e instanceof org.springframework.security.access.AccessDeniedException
        || e instanceof org.springframework.security.core.AuthenticationException
        || e instanceof org.springframework.web.servlet.resource.NoResourceFoundException
        || e instanceof org.springframework.web.HttpRequestMethodNotSupportedException) {
      throw e;
    }
    String incident = UUID.randomUUID().toString().substring(0, 8);
    log.error("500 {} {} incident={}", request.getMethod(), request.getRequestURI(), incident, e);
    ModelAndView mav = page("error/500", HttpStatus.INTERNAL_SERVER_ERROR,
        "Під час обробки запиту сталася внутрішня помилка.");
    mav.addObject("incident", incident);
    return mav;
  }

  private static ModelAndView page(String view, HttpStatus status, String message) {
    ModelAndView mav = new ModelAndView(view);
    mav.setStatus(status);
    mav.addObject("status", status.value());
    mav.addObject("message", message);
    return mav;
  }
}
