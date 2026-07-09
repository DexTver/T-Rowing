package ru.rowing.app.web;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.rowing.app.domain.BoatClass;
import ru.rowing.app.domain.Gender;
import ru.rowing.app.service.JudgeService;
import ru.rowing.app.service.JudgeViewService;
import ru.rowing.app.web.view.JudgeViews.HeatEntryView;
import ru.rowing.seeding.SeedingException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

/** Судейские действия (раздел 7.3 ТЗ). Доступ ограничен ролью JUDGE/ADMIN в {@code SecurityConfig}. */
@Controller
public class JudgeController {

    private final JudgeService judge;
    private final JudgeViewService view;
    private final ru.rowing.app.service.protocol.ProtocolImportService importer;

    public JudgeController(JudgeService judge, JudgeViewService view,
                           ru.rowing.app.service.protocol.ProtocolImportService importer) {
        this.judge = judge;
        this.view = view;
        this.importer = importer;
    }

    @GetMapping("/judge")
    public String dashboard(Model model) {
        model.addAttribute("competitions", view.listCompetitions());
        return "judge/dashboard";
    }

    @GetMapping("/judge/competitions/new")
    public String newCompetition() {
        return "judge/competition-new";
    }

    /** Импорт соревнования из стартового протокола Excel (раздел 7.3: судьи умеют так составлять). */
    @PostMapping("/judge/import")
    public String importProtocol(@org.springframework.web.bind.annotation.RequestParam("file")
                                 org.springframework.web.multipart.MultipartFile file,
                                 @RequestParam(required = false) String name,
                                 RedirectAttributes ra) {
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Файл не выбран.");
            return "redirect:/judge";
        }
        try (var in = file.getInputStream()) {
            var res = importer.importFrom(in,
                    (name != null && !name.isBlank()) ? name : file.getOriginalFilename());
            ra.addFlashAttribute("ok", "Импортировано: дней " + res.days() + ", категорий " + res.categories()
                    + ", заездов " + res.heats() + ", спортсменов " + res.athletes()
                    + " (из базы " + res.baseAthletes() + ")"
                    + (res.warnings().isEmpty() ? "." : ". Предупреждений: " + res.warnings().size() + "."));
            return "redirect:/judge/competitions/" + res.competitionId();
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/judge";
        } catch (java.io.IOException e) {
            ra.addFlashAttribute("error", "Ошибка чтения файла: " + e.getMessage());
            return "redirect:/judge";
        }
    }

    @PostMapping("/judge/competitions")
    public String createCompetition(@RequestParam String name,
                                    @RequestParam(required = false) String description) {
        var comp = judge.createCompetition(name, description);
        return "redirect:/judge/competitions/" + comp.getId();
    }

    @GetMapping("/judge/competitions/{id}")
    public String manage(@PathVariable long id, Model model) {
        model.addAttribute("c", view.manage(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено")));
        return "judge/competition";
    }

    @PostMapping("/judge/competitions/{id}/days")
    public String addDay(@PathVariable long id,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                         RedirectAttributes ra) {
        run(ra, () -> judge.addDay(id, date, startTime));
        return "redirect:/judge/competitions/" + id;
    }

    @PostMapping("/judge/competitions/{id}/categories")
    public String addCategory(@PathVariable long id,
                              @RequestParam String name,
                              @RequestParam(required = false) Long dayId,
                              @RequestParam BoatClass boatClass,
                              @RequestParam Gender gender,
                              @RequestParam int distanceM,
                              @RequestParam(required = false) Integer birthFrom,
                              @RequestParam(required = false) Integer birthTo,
                              @RequestParam(defaultValue = "false") boolean finalB,
                              @RequestParam(defaultValue = "false") boolean finalC,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime regOpens,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime regCloses,
                              RedirectAttributes ra) {
        run(ra, () -> judge.addCategory(id, dayId, name, boatClass, gender, distanceM, birthFrom, birthTo,
                finalB, finalC, toInstant(regOpens), toInstant(regCloses)));
        return "redirect:/judge/competitions/" + id;
    }

    @GetMapping("/judge/categories/{id}")
    public String category(@PathVariable long id, Model model) {
        model.addAttribute("cat", view.category(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Категория не найдена")));
        return "judge/category";
    }

    @PostMapping("/judge/categories/{id}/open")
    public String openRegistration(@PathVariable long id, RedirectAttributes ra) {
        run(ra, () -> judge.openRegistration(id));
        return "redirect:/judge/categories/" + id;
    }

    @PostMapping("/judge/categories/{id}/participants")
    public String addParticipant(@PathVariable long id,
                                 @RequestParam String fullName,
                                 @RequestParam int birthYear,
                                 @RequestParam(required = false) String region,
                                 @RequestParam(required = false) String school,
                                 RedirectAttributes ra) {
        run(ra, () -> judge.addParticipant(id, fullName, birthYear, region, school));
        return "redirect:/judge/categories/" + id;
    }

    @PostMapping("/judge/categories/{id}/close")
    public String close(@PathVariable long id, RedirectAttributes ra) {
        run(ra, () -> judge.closeRegistrationAndFormProtocol(id));
        return "redirect:/judge/categories/" + id;
    }

    @PostMapping("/judge/categories/{id}/plan")
    public String setPlan(@PathVariable long id, @RequestParam String plan, RedirectAttributes ra) {
        run(ra, () -> judge.setPlan(id, plan));
        return "redirect:/judge/categories/" + id;
    }

    @PostMapping("/judge/categories/{id}/variant")
    public String setVariant(@PathVariable long id, @RequestParam String variant, RedirectAttributes ra) {
        run(ra, () -> judge.setVariant(id, variant));
        return "redirect:/judge/categories/" + id;
    }

    @PostMapping("/judge/categories/{id}/next")
    public String formNext(@PathVariable long id, RedirectAttributes ra) {
        run(ra, () -> judge.formNextStage(id));
        return "redirect:/judge/categories/" + id;
    }

    @GetMapping("/judge/heats/{id}")
    public String heat(@PathVariable long id, Model model) {
        HeatEntryView heat = view.heatEntry(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заезд не найден"));
        model.addAttribute("heat", heat);
        model.addAttribute("prefill", heat.lanes.stream()
                .map(l -> l.lane + " " + l.value)
                .collect(Collectors.joining("\n")));
        return "judge/heat-results";
    }

    @PostMapping("/judge/heats/{id}/results")
    public String saveResults(@PathVariable long id,
                              @RequestParam("input") String input,
                              @RequestParam long categoryId,
                              RedirectAttributes ra) {
        try {
            var errors = judge.saveResults(id, input);
            if (!errors.isEmpty()) {
                ra.addFlashAttribute("error", String.join("; ", errors));
            } else {
                ra.addFlashAttribute("ok", "Результаты сохранены.");
            }
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/judge/categories/" + categoryId;
    }

    @GetMapping("/judge/competitions/{id}/print")
    public String print(@PathVariable long id,
                        @RequestParam(required = false) Integer from,
                        @RequestParam(required = false) Integer to,
                        Model model) {
        model.addAttribute("competitionName", view.competitionName(id));
        model.addAttribute("heats", view.printRange(id, from, to));
        return "judge/print";
    }

    // --- вспомогательное ---

    private void run(RedirectAttributes ra, Runnable action) {
        try {
            action.run();
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
    }

    private static Instant toInstant(LocalDateTime dt) {
        return dt == null ? null : dt.atZone(ZoneId.systemDefault()).toInstant();
    }
}
