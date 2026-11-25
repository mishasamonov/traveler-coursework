package ua.edu.sumdu.traveler.web.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.error.ValidationException;
import ua.edu.sumdu.traveler.core.service.UserService;
import ua.edu.sumdu.traveler.web.form.RegistrationForm;
import ua.edu.sumdu.traveler.web.security.AppUserDetails;

/** Login page (the form is processed by Spring Security) and registration of travellers. */
@Controller
public class AuthController {

  private final UserService users;

  public AuthController(UserService users) {
    this.users = users;
  }

  @GetMapping("/login")
  public String login(@AuthenticationPrincipal AppUserDetails user) {
    return user != null ? "redirect:/my/plans" : "auth/login";
  }

  @GetMapping("/register")
  public String registerForm(@ModelAttribute("form") RegistrationForm form,
      @AuthenticationPrincipal AppUserDetails user) {
    return user != null ? "redirect:/my/plans" : "auth/register";
  }

  @PostMapping("/register")
  public String register(@Valid @ModelAttribute("form") RegistrationForm form,
      BindingResult binding, RedirectAttributes redirect) {
    if (binding.hasErrors()) return "auth/register";
    try {
      AppUser user = users.register(form.toCommand());
      redirect.addFlashAttribute("success",
          "Обліковий запис створено. Увійдіть з адресою " + user.email() + ".");
      redirect.addFlashAttribute("email", user.email());
      return "redirect:/login";
    } catch (ValidationException e) {
      PageModel.reject(binding, e);
      return "auth/register";
    }
  }
}
