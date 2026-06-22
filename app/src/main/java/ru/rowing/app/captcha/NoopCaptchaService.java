package ru.rowing.app.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Заглушка капчи по умолчанию: проверка отключена, любой запрос проходит.
 * Активна, пока не задано {@code app.captcha.enabled=true} и не подключён реальный провайдер.
 */
@Service
@ConditionalOnProperty(name = "app.captcha.enabled", havingValue = "false", matchIfMissing = true)
public class NoopCaptchaService implements CaptchaService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean verify(String token) {
        return true;
    }
}
