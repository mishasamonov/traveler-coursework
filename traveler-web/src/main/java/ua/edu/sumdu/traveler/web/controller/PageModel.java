package ua.edu.sumdu.traveler.web.controller;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria.SortOrder;
import ua.edu.sumdu.traveler.core.domain.PlanStatus;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;

/** Helpers shared by the controllers. */
final class PageModel {

  private PageModel() {}

  /** Puts a business-rule error from the core next to the form field it concerns. */
  static void reject(BindingResult binding, ValidationException e) {
    if (e.getField() != null && binding.getTarget() != null
        && org.springframework.beans.BeanUtils.getPropertyDescriptor(binding.getTarget().getClass(),
            e.getField()) != null) {
      binding.rejectValue(e.getField(), "business", e.getMessage());
    } else {
      binding.reject("business", e.getMessage());
    }
  }

  /** Currency options of a select; the current value stays available even if it is unusual. */
  static List<String> currencies(String current) {
    Set<String> result = new LinkedHashSet<>(TravelPlanService.COMMON_CURRENCIES);
    if (current != null && !current.isBlank()) result.add(current);
    return List.copyOf(result);
  }

  static void filterOptions(Model model, String currentCurrency) {
    model.addAttribute("currencies", currencies(currentCurrency));
    model.addAttribute("statuses", Arrays.asList(PlanStatus.values()));
    model.addAttribute("sortOrders", Arrays.asList(SortOrder.values()));
  }
}
