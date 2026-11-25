package ua.edu.sumdu.traveler.web.controller;

import static ua.edu.sumdu.traveler.web.security.AppUserDetails.actorOf;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ua.edu.sumdu.traveler.core.domain.PlanSearchCriteria;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;
import ua.edu.sumdu.traveler.web.security.AppUserDetails;

/** Home page: what the system does and the latest public routes. */
@Controller
public class HomeController {

  private final TravelPlanService plans;

  public HomeController(TravelPlanService plans) {
    this.plans = plans;
  }

  @GetMapping("/")
  public String home(@AuthenticationPrincipal AppUserDetails user, Model model) {
    model.addAttribute("latest", plans.search(actorOf(user), PlanSearchCriteria.publicCatalog()));
    return "index";
  }
}
