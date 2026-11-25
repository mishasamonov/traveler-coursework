package ua.edu.sumdu.traveler.web.controller;

import static ua.edu.sumdu.traveler.web.security.AppUserDetails.actorOf;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.PlanDetails;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.Scope;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;
import ua.edu.sumdu.traveler.web.form.PlanFilterForm;
import ua.edu.sumdu.traveler.web.form.PlanForm;
import ua.edu.sumdu.traveler.web.security.AppUserDetails;
import ua.edu.sumdu.traveler.web.view.ViewFormat;

/** Travel plans: public catalogue, "my plans", plan page and the plan form. */
@Controller
public class PlanController {

  private final TravelPlanService plans;
  private final ViewFormat fmt;

  public PlanController(TravelPlanService plans, ViewFormat fmt) {
    this.plans = plans;
    this.fmt = fmt;
  }

  // ---------------------------------------------------------------- lists

  @GetMapping("/plans")
  public String catalog(@ModelAttribute("filter") PlanFilterForm filter, BindingResult binding,
      @AuthenticationPrincipal AppUserDetails user, Model model) {
    return PlanLists.render(plans, Scope.PUBLIC, "/plans", filter, binding, actorOf(user), model);
  }

  @GetMapping("/my/plans")
  public String myPlans(@ModelAttribute("filter") PlanFilterForm filter, BindingResult binding,
      @AuthenticationPrincipal AppUserDetails user, Model model) {
    return PlanLists.render(plans, Scope.OWNED, "/my/plans", filter, binding, actorOf(user), model);
  }

  // ---------------------------------------------------------------- plan page

  @GetMapping("/plans/{id}")
  public String details(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails user,
      Model model) {
    PlanDetails details = plans.details(actorOf(user), id);
    model.addAttribute("details", details);
    model.addAttribute("plan", details.plan());
    model.addAttribute("summary", details.summary());
    model.addAttribute("mapPoints", mapPoints(details.locations()));
    return "plans/detail";
  }

  /** Points of the Leaflet map, serialized to JavaScript by Thymeleaf. */
  private List<Map<String, Object>> mapPoints(List<Location> route) {
    List<Map<String, Object>> points = new ArrayList<>();
    for (Location l : route) {
      if (!l.hasCoordinates()) continue;
      Map<String, Object> p = new LinkedHashMap<>();
      p.put("order", l.visitOrder());
      p.put("name", l.name());
      p.put("lat", l.latitude().doubleValue());
      p.put("lng", l.longitude().doubleValue());
      p.put("visit", fmt.visit(l.arrivalDate(), l.departureDate()));
      points.add(p);
    }
    return points;
  }

  // ---------------------------------------------------------------- create

  @GetMapping("/plans/new")
  public String newPlan(Model model) {
    return form(model, new PlanForm(), null);
  }

  @PostMapping("/plans")
  public String create(@Valid @ModelAttribute("form") PlanForm form, BindingResult binding,
      @AuthenticationPrincipal AppUserDetails user, Model model, RedirectAttributes redirect) {
    if (binding.hasErrors()) return form(model, form, null);
    try {
      TravelPlan plan = plans.create(actorOf(user), form.toCommand());
      redirect.addFlashAttribute("success", "План «" + plan.title() + "» створено. Додайте локації маршруту.");
      return "redirect:/plans/" + plan.id();
    } catch (ValidationException e) {
      PageModel.reject(binding, e);
      return form(model, form, null);
    }
  }

  // ---------------------------------------------------------------- edit

  @GetMapping("/plans/{id}/edit")
  public String edit(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails user,
      Model model) {
    TravelPlan plan = plans.forEdit(actorOf(user), id);
    return form(model, PlanForm.of(plan), plan);
  }

  @PostMapping("/plans/{id}")
  public String update(@PathVariable UUID id, @Valid @ModelAttribute("form") PlanForm form,
      BindingResult binding, @AuthenticationPrincipal AppUserDetails user, Model model,
      RedirectAttributes redirect) {
    Actor actor = actorOf(user);
    TravelPlan current = plans.forEdit(actor, id);
    if (binding.hasErrors()) return form(model, form, current);
    try {
      plans.update(actor, id, form.toCommand());
      redirect.addFlashAttribute("success", "Зміни збережено.");
      return "redirect:/plans/" + id;
    } catch (ValidationException e) {
      PageModel.reject(binding, e);
    } catch (ConflictException e) {
      binding.reject("conflict", e.getMessage());
      model.addAttribute("conflict", true);
    }
    return form(model, form, current);
  }

  private String form(Model model, PlanForm form, TravelPlan plan) {
    model.addAttribute("form", form);
    model.addAttribute("plan", plan);
    model.addAttribute("currencies", PageModel.currencies(form.getCurrency()));
    return "plans/form";
  }

  // ---------------------------------------------------------------- actions

  @PostMapping("/plans/{id}/visibility")
  public String visibility(@PathVariable UUID id, @RequestParam boolean publish,
      @RequestParam int version, @AuthenticationPrincipal AppUserDetails user,
      RedirectAttributes redirect) {
    try {
      plans.changeVisibility(actorOf(user), id, publish, version);
      redirect.addFlashAttribute("success", publish
          ? "План опубліковано в каталозі." : "План приховано з каталогу.");
    } catch (ConflictException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/plans/" + id;
  }

  @PostMapping("/plans/{id}/copy")
  public String copy(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails user,
      RedirectAttributes redirect) {
    TravelPlan copy = plans.copy(actorOf(user), id);
    redirect.addFlashAttribute("success",
        "Маршрут скопійовано до ваших подорожей як приватний план. Змініть дати й бюджет під себе.");
    return "redirect:/plans/" + copy.id();
  }

  @PostMapping("/plans/{id}/delete")
  public String delete(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails user,
      RedirectAttributes redirect) {
    Actor actor = actorOf(user);
    TravelPlan plan = plans.forEdit(actor, id);
    plans.delete(actor, id);
    redirect.addFlashAttribute("success", "План «" + plan.title() + "» видалено разом з маршрутом.");
    return actor.isAdmin() && !actor.owns(plan) ? "redirect:/admin/plans" : "redirect:/my/plans";
  }
}
