package ua.edu.sumdu.traveler.web.controller;

import static ua.edu.sumdu.traveler.web.security.AppUserDetails.actorOf;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.Scope;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;
import ua.edu.sumdu.traveler.core.service.UserService;
import ua.edu.sumdu.traveler.web.form.PlanFilterForm;
import ua.edu.sumdu.traveler.web.monitoring.MonitoringOverview;
import ua.edu.sumdu.traveler.web.security.AppUserDetails;
import ua.edu.sumdu.traveler.web.security.SessionTerminator;

/**
 * Administrator: dashboard with statistics and system state, all plans (including private and
 * ownerless ones) and user accounts. Protected twice: by the URL rule /admin/** and here.
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final TravelPlanService plans;
  private final UserService users;
  private final MonitoringOverview monitoring;
  private final SessionTerminator sessions;

  public AdminController(TravelPlanService plans, UserService users, MonitoringOverview monitoring,
      SessionTerminator sessions) {
    this.plans = plans;
    this.users = users;
    this.monitoring = monitoring;
    this.sessions = sessions;
  }

  @GetMapping
  public String dashboard(@AuthenticationPrincipal AppUserDetails user, Model model) {
    model.addAttribute("stats", plans.statistics(actorOf(user)));
    model.addAttribute("system", monitoring.snapshot());
    return "admin/dashboard";
  }

  @GetMapping("/plans")
  public String allPlans(@ModelAttribute("filter") PlanFilterForm filter, BindingResult binding,
      @AuthenticationPrincipal AppUserDetails user, Model model) {
    return PlanLists.render(plans, Scope.ALL, "/admin/plans", filter, binding, actorOf(user), model);
  }

  @GetMapping("/users")
  public String users(@AuthenticationPrincipal AppUserDetails user, Model model) {
    model.addAttribute("users", users.listUsers(actorOf(user)));
    model.addAttribute("roles", Role.values());
    return "admin/users";
  }

  @PostMapping("/users/{id}/role")
  public String changeRole(@PathVariable UUID id, @RequestParam Role role,
      @AuthenticationPrincipal AppUserDetails user, RedirectAttributes redirect) {
    try {
      AppUser changed = users.changeRole(actorOf(user), id, role);
      int ended = sessions.expireSessionsOf(id);
      redirect.addFlashAttribute("success", "Роль користувача " + changed.email() + ": "
          + changed.role().label() + (ended > 0 ? ". Активні сесії завершено." : "."));
    } catch (ValidationException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/users";
  }

  @PostMapping("/users/{id}/enabled")
  public String setEnabled(@PathVariable UUID id, @RequestParam boolean enabled,
      @AuthenticationPrincipal AppUserDetails user, RedirectAttributes redirect) {
    try {
      AppUser changed = users.setEnabled(actorOf(user), id, enabled);
      if (!enabled) sessions.expireSessionsOf(id);
      redirect.addFlashAttribute("success", (enabled ? "Розблоковано: " : "Заблоковано: ")
          + changed.email());
    } catch (ValidationException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/admin/users";
  }
}
