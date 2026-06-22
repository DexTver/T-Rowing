package ru.rowing.app.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import ru.rowing.app.captcha.CaptchaService;
import ru.rowing.app.service.PublicViewService;

import java.security.Principal;

/** Публичная часть для зрителей/участников (раздел 7.1 ТЗ). Чтение без авторизации. */
@Controller
public class PublicController {

    private final PublicViewService view;
    private final CaptchaService captcha;

    public PublicController(PublicViewService view, CaptchaService captcha) {
        this.view = view;
        this.captcha = captcha;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("competitions", view.listCompetitions());
        return "home";
    }

    @GetMapping("/competitions/{id}")
    public String competition(@PathVariable long id, Model model) {
        var comp = view.getCompetition(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено"));
        model.addAttribute("competition", comp);
        return "competition";
    }

    @GetMapping("/heats/{id}")
    public String heat(@PathVariable long id, Model model) {
        var heat = view.getHeat(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заезд не найден или протокол не сформирован"));
        model.addAttribute("heat", heat);
        return "heat";
    }

    @GetMapping("/results")
    public String results(@RequestParam(required = false) String athlete,
                          @RequestParam(required = false) String coach,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String region,
                          @RequestParam(required = false) String school,
                          @RequestParam(required = false, name = "captcha") String captchaToken,
                          Principal principal,
                          Model model) {
        boolean searching = anyPresent(athlete, coach, category, region, school);
        boolean anonymous = principal == null;

        model.addAttribute("captchaEnabled", captcha.isEnabled());
        model.addAttribute("filter", new String[]{athlete, coach, category, region, school});
        // Значения по умолчанию, чтобы шаблон не обращался к null (страница без поиска).
        model.addAttribute("searched", false);
        model.addAttribute("rows", java.util.List.of());

        // Капча требуется анонимам на формах поиска (раздел 3).
        if (searching && anonymous && captcha.isEnabled() && !captcha.verify(captchaToken)) {
            model.addAttribute("captchaError", true);
            return "results";
        }

        if (searching) {
            model.addAttribute("rows", view.search(athlete, coach, category, region, school));
            model.addAttribute("searched", true);
        }
        return "results";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private static boolean anyPresent(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return true;
            }
        }
        return false;
    }
}
