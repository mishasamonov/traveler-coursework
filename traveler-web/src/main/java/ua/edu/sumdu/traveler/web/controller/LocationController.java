package ua.edu.sumdu.traveler.web.controller;

import static ua.edu.sumdu.traveler.web.security.AppUserDetails.actorOf;

import jakarta.validation.Valid;
import java.time.ZoneId;
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
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.error.ConflictException;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.service.LocationService;
import ua.edu.sumdu.traveler.core.service.LocationService.Direction;
import ua.edu.sumdu.traveler.core.service.LocationService.LocationEdit;
import ua.edu.sumdu.traveler.web.form.LocationForm;
import ua.edu.sumdu.traveler.web.security.AppUserDetails;

/** Route of a plan: adding, editing, moving and deleting locations. */
@Controller
public class LocationController {

  private final LocationService locations;
  private final ZoneId zone;

  public LocationController(LocationService locations, ZoneId travelerZone) {
    this.locations = locations;
    this.zone = travelerZone;
  }

  @GetMapping("/plans/{planId}/locations/new")
  public String newLocation(@PathVariable UUID planId, @AuthenticationPrincipal AppUserDetails user,
      Model model) {
    TravelPlan plan = locations.planForNewLocation(actorOf(user), planId);
    return form(model, new LocationForm(), plan, null, 0);
  }

  @PostMapping("/plans/{planId}/locations")
  public String add(@PathVariable UUID planId, @Valid @ModelAttribute("form") LocationForm form,
      BindingResult binding, @AuthenticationPrincipal AppUserDetails user, Model model,
      RedirectAttributes redirect) {
    Actor actor = actorOf(user);
    TravelPlan plan = locations.planForNewLocation(actor, planId);
    form.setVisitOrder(null); // the position of a new location is assigned by the system
    if (binding.hasErrors()) return form(model, form, plan, null, 0);
    try {
      Location saved = locations.add(actor, planId, form.toCommand(zone));
      redirect.addFlashAttribute("success", "Локацію «" + saved.name()
          + "» додано до маршруту на позицію " + saved.visitOrder() + ".");
      return "redirect:/plans/" + planId + "#route";
    } catch (ValidationException e) {
      PageModel.reject(binding, e);
      return form(model, form, plan, null, 0);
    }
  }

  @GetMapping("/locations/{id}/edit")
  public String edit(@PathVariable UUID id, @AuthenticationPrincipal AppUserDetails user,
      Model model) {
    LocationEdit edit = locations.forEdit(actorOf(user), id);
    return form(model, LocationForm.of(edit.location(), zone), edit.plan(), edit.location(),
        edit.routeSize());
  }

  @PostMapping("/locations/{id}")
  public String update(@PathVariable UUID id, @Valid @ModelAttribute("form") LocationForm form,
      BindingResult binding, @AuthenticationPrincipal AppUserDetails user, Model model,
      RedirectAttributes redirect) {
    Actor actor = actorOf(user);
    LocationEdit edit = locations.forEdit(actor, id);
    if (binding.hasErrors()) {
      return form(model, form, edit.plan(), edit.location(), edit.routeSize());
    }
    try {
      Location saved = locations.update(actor, id, form.toCommand(zone));
      redirect.addFlashAttribute("success", "Локацію «" + saved.name() + "» збережено.");
      return "redirect:/plans/" + saved.travelPlanId() + "#route";
    } catch (ValidationException e) {
      PageModel.reject(binding, e);
    } catch (ConflictException e) {
      binding.reject("conflict", e.getMessage());
      model.addAttribute("conflict", true);
    }
    return form(model, form, edit.plan(), edit.location(), edit.routeSize());
  }

  @PostMapping("/locations/{id}/move")
  public String move(@PathVariable UUID id, @RequestParam Direction direction,
      @RequestParam int version, @RequestParam UUID planId,
      @AuthenticationPrincipal AppUserDetails user, RedirectAttributes redirect) {
    try {
      locations.move(actorOf(user), id, direction, version);
      redirect.addFlashAttribute("success", "Порядок маршруту змінено.");
    } catch (ValidationException | ConflictException e) {
      redirect.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/plans/" + planId + "#route";
  }

  @PostMapping("/locations/{id}/delete")
  public String delete(@PathVariable UUID id, @RequestParam UUID planId,
      @AuthenticationPrincipal AppUserDetails user, RedirectAttributes redirect) {
    locations.delete(actorOf(user), id);
    redirect.addFlashAttribute("success", "Локацію видалено, нумерацію маршруту оновлено.");
    return "redirect:/plans/" + planId + "#route";
  }

  private String form(Model model, LocationForm form, TravelPlan plan, Location location,
      int routeSize) {
    model.addAttribute("form", form);
    model.addAttribute("plan", plan);
    model.addAttribute("location", location);
    model.addAttribute("routeSize", routeSize);
    return "locations/form";
  }
}
