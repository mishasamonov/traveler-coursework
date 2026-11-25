package ua.edu.sumdu.traveler.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/** Own Actuator extensions: metrics of the services, the "travel" endpoint, runtime log levels. */
@IntegrationTest
@WithUserDetails(IntegrationTest.ADMIN)
class MonitoringIT {

  @Autowired MockMvc mvc;

  @Test
  @DisplayName("Таймер traveler.service рахує виклики бізнес-сервісів")
  void serviceTimer() throws Exception {
    mvc.perform(get("/plans")).andExpect(status().isOk()); // TravelPlanService.search
    mvc.perform(get("/actuator/metrics/traveler.service").param("tag", "method:search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.measurements[0].statistic").value("COUNT"));
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk())
        .andExpect(content().string(containsString("traveler_service_seconds_count")))
        .andExpect(content().string(containsString("traveler_plans")));
  }

  @Test
  @DisplayName("Рівень логування змінюється під час роботи через /actuator/loggers")
  void changeLogLevel() throws Exception {
    mvc.perform(post("/actuator/loggers/ua.edu.sumdu.traveler.web.monitoring").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{\"configuredLevel\":\"DEBUG\"}"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/actuator/loggers/ua.edu.sumdu.traveler.web.monitoring"))
        .andExpect(jsonPath("$.configuredLevel").value("DEBUG"));
    mvc.perform(post("/actuator/loggers/ua.edu.sumdu.traveler.web.monitoring").with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("info містить дані збірки та розділ travel")
  void info() throws Exception {
    mvc.perform(get("/actuator/info")).andExpect(status().isOk())
        .andExpect(jsonPath("$.build.artifact").value("traveler-web"))
        .andExpect(jsonPath("$.java.version").exists());
  }
}
