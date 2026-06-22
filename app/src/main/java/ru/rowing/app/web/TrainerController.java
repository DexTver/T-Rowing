package ru.rowing.app.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.rowing.app.service.TrainerService;
import ru.rowing.app.service.TrainerViewService;
import ru.rowing.seeding.SeedingException;

import java.security.Principal;
import java.util.List;

/** Действия тренера (раздел 7.2 ТЗ). Доступ ограничен ролью TRAINER/ADMIN в {@code SecurityConfig}. */
@Controller
public class TrainerController {

    private final TrainerService trainer;
    private final TrainerViewService view;

    public TrainerController(TrainerService trainer, TrainerViewService view) {
        this.trainer = trainer;
        this.view = view;
    }

    @GetMapping("/trainer")
    public String dashboard(Model model) {
        model.addAttribute("competitions", view.listCompetitions());
        return "trainer/dashboard";
    }

    @GetMapping("/trainer/competitions/{id}")
    public String competition(@PathVariable long id, Principal principal, Model model) {
        long coachId = trainer.currentCoach(principal.getName()).getId();
        model.addAttribute("c", view.competition(coachId, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Соревнование не найдено")));
        return "trainer/competition";
    }

    @PostMapping("/trainer/competitions/{id}/athletes")
    public String createAthlete(@PathVariable long id,
                                @RequestParam String fullName,
                                @RequestParam int birthYear,
                                @RequestParam(required = false) String rank,
                                @RequestParam(required = false) String region,
                                @RequestParam(required = false) String school,
                                Principal principal, RedirectAttributes ra) {
        try {
            trainer.createAthlete(principal.getName(), fullName, birthYear, rank, region, school);
            ra.addFlashAttribute("ok", "Спортсмен «" + fullName + "» добавлен.");
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trainer/competitions/" + id;
    }

    @PostMapping("/trainer/competitions/{id}/entries")
    public String commit(@PathVariable long id,
                         @RequestParam(name = "athleteId", required = false) List<Long> athleteIds,
                         @RequestParam(name = "categoryId", required = false) List<Long> categoryIds,
                         Principal principal, RedirectAttributes ra) {
        try {
            TrainerService.CommitResult r = trainer.commitEntries(principal.getName(), id, athleteIds, categoryIds);
            ra.addFlashAttribute("ok", "Добавлено заявок: " + r.added() + ".");
            if (!r.messages().isEmpty()) {
                ra.addFlashAttribute("error", String.join("; ", r.messages()));
            }
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trainer/competitions/" + id;
    }

    @PostMapping("/trainer/entries/{entryId}/delete")
    public String deleteEntry(@PathVariable long entryId,
                              @RequestParam long competitionId,
                              Principal principal, RedirectAttributes ra) {
        try {
            trainer.deleteEntry(principal.getName(), entryId);
            ra.addFlashAttribute("ok", "Заявка удалена.");
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trainer/competitions/" + competitionId;
    }
}
