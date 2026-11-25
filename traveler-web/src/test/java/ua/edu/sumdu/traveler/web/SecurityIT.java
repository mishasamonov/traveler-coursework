package ua.edu.sumdu.traveler.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.sumdu.traveler.web.IntegrationTest.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.Role;
import ua.edu.sumdu.traveler.core.service.UserService;

/** Authentication and access rules of the three user categories. */
@IntegrationTest
class SecurityIT {

  @Autowired MockMvc mvc;
  @Autowired UserService users;

  @Test
  @DisplayName("Гість бачить каталог і публічний план, але не приватний")
  void guestReadsPublicCatalog() throws Exception {
    mvc.perform(get("/plans")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Карпати: Яремче")));
    mvc.perform(get("/plans/" + CARPATHIANS)).andExpect(status().isOk());
    mvc.perform(get("/plans/" + ITALY)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Гостя перенаправляє на вхід зі сторінок, що потребують облікового запису")
  void guestIsRedirectedToLogin() throws Exception {
    mvc.perform(get("/my/plans")).andExpect(redirectedUrlPattern("**/login"));
    mvc.perform(get("/plans/new")).andExpect(redirectedUrlPattern("**/login"));
    mvc.perform(get("/admin")).andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  @DisplayName("Вхід формою: правильний пароль — до «Моїх подорожей», неправильний — помилка")
  void formLogin_() throws Exception {
    mvc.perform(formLogin("/login").userParameter("email").user(OLENA).password("olena123"))
        .andExpect(redirectedUrl("/my/plans")).andExpect(authenticated().withRoles("USER"));
    mvc.perform(formLogin("/login").userParameter("email").user(OLENA).password("wrong"))
        .andExpect(redirectedUrl("/login?error")).andExpect(unauthenticated());
  }

  @Test
  @Transactional
  @DisplayName("Заблокований користувач не може увійти")
  void blockedUserCannotLogIn() throws Exception {
    var admin = users.findByEmail(ADMIN).orElseThrow();
    var andrii = users.findByEmail(ANDRII).orElseThrow();
    users.setEnabled(new Actor(admin.id(), admin.email(), Role.ADMIN), andrii.id(), false);
    mvc.perform(formLogin("/login").userParameter("email").user(ANDRII).password("andrii123"))
        .andExpect(redirectedUrl("/login?blocked"));
  }

  @Test
  @WithUserDetails(OLENA)
  @DisplayName("Мандрівник не має доступу до адміністрування та закритих вузлів Actuator")
  void userIsNotAdmin() throws Exception {
    mvc.perform(get("/admin")).andExpect(status().isForbidden());
    mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    mvc.perform(get("/actuator/metrics")).andExpect(status().isForbidden());
    mvc.perform(get("/my/plans")).andExpect(status().isOk());
  }

  @Test
  @WithUserDetails(OLENA)
  @DisplayName("POST без CSRF-токена відхиляється")
  void csrfIsRequired() throws Exception {
    mvc.perform(post("/plans").param("title", "Без токена").param("currency", "UAH"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/plans").with(csrf()).param("title", "").param("currency", "UAH"))
        .andExpect(status().isOk()); // form with validation errors
  }

  @Test
  @DisplayName("Actuator: health та info публічні без деталей, решта — лише для ADMIN")
  void actuatorForGuest() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
    mvc.perform(get("/actuator/info")).andExpect(status().isOk())
        .andExpect(jsonPath("$.app.name").value("Traveler"));
    mvc.perform(get("/actuator/travel")).andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  @WithUserDetails(ADMIN)
  @DisplayName("Адміністратор бачить компоненти health і всі вузли Actuator")
  void actuatorForAdmin() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk())
        .andExpect(jsonPath("$.components.db.status").value("UP"))
        .andExpect(jsonPath("$.components.travelData.details.routeOrderConsistent").value(true));
    mvc.perform(get("/actuator/travel")).andExpect(status().isOk())
        .andExpect(jsonPath("$.locations").isNumber());
    mvc.perform(get("/actuator/flyway")).andExpect(status().isOk())
        .andExpect(content().string(containsString("traveler api schema")));
    mvc.perform(get("/admin")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Стан системи")));
  }
}
