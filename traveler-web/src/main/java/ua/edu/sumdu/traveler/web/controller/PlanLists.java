package ua.edu.sumdu.traveler.web.controller;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.Scope;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;
import ua.edu.sumdu.traveler.web.form.PlanFilterForm;

/** One template (plans/list) for the catalogue, "my plans" and the administrator's list. */
final class PlanLists {

  private PlanLists() {}

  static String render(TravelPlanService plans, Scope scope, String path, PlanFilterForm filter,
      BindingResult binding, Actor actor, Model model) {
    model.addAttribute("scope", scope.name());
    model.addAttribute("listPath", path);
    PageModel.filterOptions(model, filter.getCurrency());
    if (binding.hasErrors()) {
      model.addAttribute("filterError", "Перевірте значення фільтрів: числа й дати мають бути коректними.");
      return "plans/list";
    }
    try {
      model.addAttribute("page", plans.search(actor, filter.toCriteria(scope)));
    } catch (ValidationException e) {
      model.addAttribute("filterError", e.getMessage());
    }
    return "plans/list";
  }
}
