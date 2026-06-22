package ru.rowing.app.captcha;

/**
 * Абстракция капчи для анонимных форм поиска/фильтрации (раздел 3 ТЗ).
 * Конкретный провайдер (hCaptcha / Turnstile / SmartCaptcha) подключается отдельной реализацией;
 * по умолчанию активна {@link NoopCaptchaService} (капча отключена).
 */
public interface CaptchaService {

    /** Включена ли проверка капчи (если нет — формы работают без токена). */
    boolean isEnabled();

    /** Проверяет токен капчи, присланный формой. */
    boolean verify(String token);
}
