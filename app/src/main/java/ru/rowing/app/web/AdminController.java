package ru.rowing.app.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.rowing.app.domain.Role;
import ru.rowing.app.service.AdminService;
import ru.rowing.app.service.AdminViewService;
import ru.rowing.app.service.PlanCatalog;
import ru.rowing.seeding.SeedingException;

import java.security.Principal;

/** Админ-программист (раздел 7.4 ТЗ). Доступ ограничен ролью ADMIN в {@code SecurityConfig}. */
@Controller
public class AdminController {

    private final AdminService admin;
    private final AdminViewService view;
    private final PlanCatalog catalog;

    public AdminController(AdminService admin, AdminViewService view, PlanCatalog catalog) {
        this.admin = admin;
        this.view = view;
        this.catalog = catalog;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("overview", view.overview());
        model.addAttribute("competitions", view.competitions());
        return "admin/dashboard";
    }

    @PostMapping("/admin/competitions/{id}/delete")
    public String deleteCompetition(@PathVariable long id, RedirectAttributes ra) {
        run(ra, () -> admin.deleteCompetition(id));
        return "redirect:/admin";
    }

    // --- пользователи ---

    @GetMapping("/admin/users")
    public String users(Model model) {
        model.addAttribute("users", view.users());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/admin/users")
    public String createUser(@RequestParam String login, @RequestParam String password,
                             @RequestParam Role role, @RequestParam(required = false) String displayName,
                             RedirectAttributes ra) {
        run(ra, () -> admin.createUser(login, password, role, displayName));
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}")
    public String updateUser(@PathVariable long id, @RequestParam Role role,
                             @RequestParam(required = false) String displayName,
                             @RequestParam(defaultValue = "false") boolean active, RedirectAttributes ra) {
        run(ra, () -> admin.updateUser(id, role, displayName, active));
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/password")
    public String resetPassword(@PathVariable long id, @RequestParam String password, RedirectAttributes ra) {
        run(ra, () -> admin.resetPassword(id, password));
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable long id, Principal principal, RedirectAttributes ra) {
        run(ra, () -> admin.deleteUser(id, principal.getName()));
        return "redirect:/admin/users";
    }

    // --- тренеры ---

    @GetMapping("/admin/coaches")
    public String coaches(Model model) {
        model.addAttribute("coaches", view.coaches());
        model.addAttribute("userOptions", view.userOptions());
        return "admin/coaches";
    }

    @PostMapping("/admin/coaches")
    public String createCoach(@RequestParam String fullName, @RequestParam(required = false) Long userId,
                              RedirectAttributes ra) {
        run(ra, () -> admin.createCoach(fullName, userId));
        return "redirect:/admin/coaches";
    }

    @PostMapping("/admin/coaches/{id}")
    public String updateCoach(@PathVariable long id, @RequestParam String fullName,
                              @RequestParam(required = false) Long userId, RedirectAttributes ra) {
        run(ra, () -> admin.updateCoach(id, fullName, userId));
        return "redirect:/admin/coaches";
    }

    @PostMapping("/admin/coaches/{id}/delete")
    public String deleteCoach(@PathVariable long id, RedirectAttributes ra) {
        run(ra, () -> admin.deleteCoach(id));
        return "redirect:/admin/coaches";
    }

    // --- спортсмены ---

    @GetMapping("/admin/athletes")
    public String athletes(Model model) {
        model.addAttribute("athletes", view.athletes());
        model.addAttribute("coachOptions", view.coachOptions());
        return "admin/athletes";
    }

    @PostMapping("/admin/athletes/{id}")
    public String updateAthlete(@PathVariable long id, @RequestParam String fullName,
                               @RequestParam int birthYear, @RequestParam(required = false) String rank,
                               @RequestParam(required = false) String region,
                               @RequestParam(required = false) String school,
                               @RequestParam(required = false) Long coachId, RedirectAttributes ra) {
        run(ra, () -> admin.updateAthlete(id, fullName, birthYear, rank, region, school, coachId));
        return "redirect:/admin/athletes";
    }

    @PostMapping("/admin/athletes/{id}/delete")
    public String deleteAthlete(@PathVariable long id, RedirectAttributes ra) {
        run(ra, () -> admin.deleteAthlete(id));
        return "redirect:/admin/athletes";
    }

    // --- файлы сеток ---

    @GetMapping("/admin/plans")
    public String plans(Model model) {
        model.addAttribute("p", view.plans());
        model.addAttribute("letters", PlanCatalog.LETTERS);
        return "admin/plans";
    }

    @PostMapping("/admin/plans")
    public String uploadPlan(@RequestParam String letter, @RequestParam String json, RedirectAttributes ra) {
        try {
            catalog.replace(letter, json);
            ra.addFlashAttribute("ok", "План " + letter + " загружен и прошёл валидацию.");
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/plans";
    }

    private void run(RedirectAttributes ra, Runnable action) {
        try {
            action.run();
            ra.addFlashAttribute("ok", "Готово.");
        } catch (SeedingException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
    }
}
