package ua.marketplace.web.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Спільні атрибути моделі для всіх веб-сторінок. */
@ControllerAdvice(basePackages = "ua.marketplace.web")
public class UiModelAdvice {

    private final boolean devTools;

    public UiModelAdvice(@Value("${app.ui.dev-tools:false}") boolean devTools) {
        this.devTools = devTools;
    }

    @ModelAttribute("devTools")
    public boolean devTools() {
        return devTools;
    }
}
