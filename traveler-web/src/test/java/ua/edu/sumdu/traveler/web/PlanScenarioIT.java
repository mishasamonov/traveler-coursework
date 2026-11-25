package ua.edu.sumdu.traveler.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.edu.sumdu.traveler.web.IntegrationTest.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;
import ua.edu.sumdu.traveler.core.port.UserPort;

/**
 * The designed use-case scenarios through the web layer. Each test runs in a transaction that is
 * rolled back, so the demo data stay unchanged.
 */
@IntegrationTest
@Transactional
class PlanScenarioIT {

  @Autowired MockMvc mvc;
  @Autowired TravelPlanPort plans;
  @Autowired LocationPort locations;
  @Autowired UserPort users;

  @Test
  @WithUserDetails(OLENA)
  @DisplayName("Сценарій мандрівника: план → три локації → перестановка → видалення")
  void travellerScenario() throws Exception {
    MvcResult created = mvc.perform(post("/plans").with(csrf())
            .param("title", "Чернігів на вихідні").param("startDate", "2027-08-01")
            .param("endDate", "2027-08-02").param("budget", "3000").param("currency", "UAH")
            .param("publicPlan", "true"))
        .andExpect(redirectedUrlPattern("/plans/*")).andReturn();
    UUID planId = UUID.fromString(created.getResponse().getRedirectedUrl().substring(7));
    TravelPlan plan = plans.findById(planId).orElseThrow();
    assertThat(plan.ownerId()).isEqualTo(users.findByEmail(OLENA).orElseThrow().id());
    assertThat(plan.version()).isEqualTo(1);

    for (String name : List.of("Вал", "Антонієві печери", "Єлецький монастир")) {
      mvc.perform(post("/plans/" + planId + "/locations").with(csrf()).param("name", name)
              .param("latitude", "51.49").param("longitude", "31.30")
              .param("arrivalDate", "2027-08-01T10:00").param("departureDate", "2027-08-01T11:00")
              .param("budget", "100"))
          .andExpect(redirectedUrl("/plans/" + planId + "#route"));
    }
    assertThat(route(planId)).containsExactly("1 Вал", "2 Антонієві печери", "3 Єлецький монастир");

    Location last = locations.findByPlanId(planId).get(2);
    mvc.perform(post("/locations/" + last.id() + "/move").with(csrf()).param("direction", "UP")
            .param("version", String.valueOf(last.version())).param("planId", planId.toString()))
        .andExpect(redirectedUrl("/plans/" + planId + "#route"));
    assertThat(route(planId)).containsExactly("1 Вал", "2 Єлецький монастир", "3 Антонієві печери");

    Location first = locations.findByPlanId(planId).get(0);
    mvc.perform(post("/locations/" + first.id() + "/delete").with(csrf())
            .param("planId", planId.toString()))
        .andExpect(redirectedUrl("/plans/" + planId + "#route"));
    assertThat(route(planId)).containsExactly("1 Єлецький монастир", "2 Антонієві печери");

    mvc.perform(get("/plans/" + planId)).andExpect(status().isOk())
        .andExpect(content().string(containsString("Єлецький монастир")))
        .andExpect(content().string(containsString("Підсумок маршруту")));
  }

  @Test
  @WithUserDetails(OLENA)
  @DisplayName("Валідація форми плану: порожня назва, кінець раніше початку, від'ємний бюджет")
  void planFormValidation() throws Exception {
    mvc.perform(post("/plans").with(csrf()).param("title", " ").param("startDate", "2027-08-05")
            .param("endDate", "2027-08-01").param("budget", "-5").param("currency", "UAH"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "title", "endDate", "budget"))
        .andExpect(content().string(containsString("Дата завершення не може передувати")));
  }

  @Test
  @WithUserDetails(OLENA)
  @DisplayName("Локація поза датами подорожі відхиляється бізнес-правилом")
  void locationOutsideTrip() throws Exception {
    mvc.perform(post("/plans/" + LVIV + "/locations").with(csrf()).param("name", "Рано")
            .param("arrivalDate", "2027-03-01T10:00"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "arrivalDate"))
        .andExpect(content().string(containsString("раніше за початок подорожі")));
  }

  @Test
  @WithUserDetails(OLENA)
  @DisplayName("Застаріла версія форми: зміни іншої вкладки не перезаписуються")
  void staleFormShowsConflict() throws Exception {
    mvc.perform(post("/plans/" + LVIV).with(csrf()).param("title", "Львів — вкладка 1")
            .param("currency", "UAH").param("version", "1").param("publicPlan", "true"))
        .andExpect(redirectedUrl("/plans/" + LVIV));
    mvc.perform(post("/plans/" + LVIV).with(csrf()).param("title", "Львів — вкладка 2")
            .param("currency", "UAH").param("version", "1").param("publicPlan", "true"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("conflict", true));
    assertThat(plans.findById(LVIV).orElseThrow().title()).isEqualTo("Львів — вкладка 1");
  }

  @Test
  @WithUserDetails(ANDRII)
  @DisplayName("Мандрівник не може змінювати чужий план, але може скопіювати публічний")
  void strangerCopiesInsteadOfEditing() throws Exception {
    mvc.perform(get("/plans/" + CARPATHIANS + "/edit")).andExpect(status().isForbidden());
    mvc.perform(post("/plans/" + CARPATHIANS + "/delete").with(csrf())).andExpect(status().isForbidden());
    MvcResult copy = mvc.perform(post("/plans/" + CARPATHIANS + "/copy").with(csrf()))
        .andExpect(redirectedUrlPattern("/plans/*")).andReturn();
    UUID copyId = UUID.fromString(copy.getResponse().getRedirectedUrl().substring(7));
    TravelPlan copied = plans.findById(copyId).orElseThrow();
    assertThat(copied.isPublic()).isFalse();
    assertThat(copied.ownerId()).isEqualTo(users.findByEmail(ANDRII).orElseThrow().id());
    assertThat(locations.findByPlanId(copyId)).hasSize(4);
  }

  @Test
  @WithUserDetails(ADMIN)
  @DisplayName("Сценарій адміністратора: чужий приватний план, усі плани, блокування користувача")
  void adminScenario() throws Exception {
    mvc.perform(get("/plans/" + ITALY + "/edit")).andExpect(status().isOk());
    mvc.perform(get("/admin/plans").param("owner", "олена")).andExpect(status().isOk())
        .andExpect(content().string(containsString("Італія: Рим — Флоренція")));
    UUID andrii = users.findByEmail(ANDRII).orElseThrow().id();
    mvc.perform(post("/admin/users/" + andrii + "/enabled").with(csrf()).param("enabled", "false"))
        .andExpect(redirectedUrl("/admin/users"));
    assertThat(users.findById(andrii).orElseThrow().enabled()).isFalse();
    mvc.perform(post("/plans/" + ODESA + "/delete").with(csrf()))
        .andExpect(redirectedUrl("/admin/plans"));
    assertThat(plans.findById(ODESA)).isEmpty();
  }

  @Test
  @DisplayName("Реєстрація: паролі мають збігатися, після успіху — перехід до входу")
  void registration() throws Exception {
    mvc.perform(post("/register").with(csrf()).param("email", "new@traveler.test")
            .param("displayName", "Новий").param("password", "secret123")
            .param("confirmPassword", "secret124"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));
    mvc.perform(post("/register").with(csrf()).param("email", "New@Traveler.test")
            .param("displayName", "Новий").param("password", "secret123")
            .param("confirmPassword", "secret123"))
        .andExpect(redirectedUrl("/login"));
    assertThat(users.findByEmail("new@traveler.test")).isPresent();
  }

  private List<String> route(UUID planId) {
    return locations.findByPlanId(planId).stream().map(l -> l.visitOrder() + " " + l.name()).toList();
  }
}
