package ua.marketplace.web.support;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;

/** Помилки HTML-контролерів. REST-контролери обробляє GlobalExceptionHandler. */
@ControllerAdvice(basePackages = "ua.marketplace.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    private final UiErrors uiErrors;

    public WebExceptionHandler(UiErrors uiErrors) {
        this.uiErrors = uiErrors;
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException e, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("errorTitle", "Сторінку не знайдено");
        model.addAttribute("errorMessage", uiErrors.message(e.code()));
        return "error/404";
    }

    @ExceptionHandler(BusinessRuleException.class)
    public String businessRule(BusinessRuleException e, Model model, HttpServletResponse response) {
        int status = e.code().status().value();
        response.setStatus(status);
        model.addAttribute("status", status);
        model.addAttribute("errorTitle", status == 400 ? "Некоректний запит" : "Дію не виконано");
        model.addAttribute("errorMessage", uiErrors.message(e.code()));
        return "error/4xx";
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String locking(RuntimeException e, Model model) {
        model.addAttribute("status", 409);
        model.addAttribute("errorTitle", "Дію не виконано");
        model.addAttribute("errorMessage", uiErrors.message(ErrorCode.CONCURRENT_MODIFICATION));
        return "error/4xx";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(Exception e, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("errorTitle", "Некоректний запит");
        model.addAttribute("errorMessage", "Адреса або параметри запиту некоректні. Перевірте їх і спробуйте ще раз.");
        return "error/4xx";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String unexpected(Exception e, Model model) {
        log.error("Unhandled error in web layer", e);
        model.addAttribute("status", 500);
        model.addAttribute("errorTitle", "Щось пішло не так");
        model.addAttribute("errorMessage", uiErrors.message(ErrorCode.INTERNAL_ERROR));
        return "error/5xx";
    }
}
