package ua.marketplace.web.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import ua.marketplace.common.error.ErrorCode;

/** Українські повідомлення про помилки: ключ error.<CODE> у messages.properties. */
@Component
public class UiErrors {

    private static final Locale UK = Locale.forLanguageTag("uk");

    private final MessageSource messages;

    public UiErrors(MessageSource messages) {
        this.messages = messages;
    }

    public String message(ErrorCode code) {
        return text("error." + code.name());
    }

    /** Текст за ключем; якщо ключа немає, повертається сам ключ. */
    public String text(String key, Object... args) {
        return messages.getMessage(key, args, key, UK);
    }

    /** Усі повідомлення BindingResult, розв’язані через messages.properties або текст анотації. */
    public List<String> messages(BindingResult result) {
        List<String> out = new ArrayList<>();
        for (ObjectError error : result.getAllErrors()) {
            out.add(messages.getMessage(error, UK));
        }
        return out;
    }
}
