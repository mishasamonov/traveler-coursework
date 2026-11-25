package ua.edu.sumdu.traveler.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ua.edu.sumdu.traveler.core.command.LocationCommand;
import ua.edu.sumdu.traveler.core.command.PlanCommand;
import ua.edu.sumdu.traveler.core.domain.Actor;
import ua.edu.sumdu.traveler.core.domain.AppUser;
import ua.edu.sumdu.traveler.core.domain.Location;
import ua.edu.sumdu.traveler.core.domain.TravelPlan;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.service.LocationService;
import ua.edu.sumdu.traveler.core.service.TravelPlanService;
import ua.edu.sumdu.traveler.core.service.UserService;

/**
 * The same check as the concurrency test of the REST API in the laboratory works: many
 * simultaneous additions to one route never produce equal positions, because each transaction
 * locks the plan row (SELECT ... FOR UPDATE) before it reads MAX(visit_order).
 */
@IntegrationTest
class ConcurrentRouteIT {

  @Autowired TravelPlanService plans;
  @Autowired LocationService locationService;
  @Autowired LocationPort locations;
  @Autowired UserService users;

  @Test
  @DisplayName("12 паралельних додавань локацій отримують позиції 1..12 без дублікатів")
  void parallelAdditions() throws Exception {
    AppUser olena = users.findByEmail(IntegrationTest.OLENA).orElseThrow();
    Actor actor = new Actor(olena.id(), olena.email(), olena.role());
    TravelPlan plan = plans.create(actor, new PlanCommand("Паралельний маршрут", null, null, null,
        BigDecimal.TEN, "UAH", false, null));
    int threads = 12;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Location>> results = new ArrayList<>();
    try {
      for (int i = 1; i <= threads; i++) {
        String name = "Точка " + i;
        Callable<Location> task = () -> {
          start.await();
          return locationService.add(actor, plan.id(),
              new LocationCommand(name, null, null, null, null, null, null, null, null, null));
        };
        results.add(pool.submit(task));
      }
      start.countDown();
      for (Future<Location> f : results) f.get();
    } finally {
      pool.shutdown();
    }
    List<Integer> orders = locations.findByPlanId(plan.id()).stream()
        .map(Location::visitOrder).toList();
    assertThat(orders).hasSize(threads).doesNotHaveDuplicates()
        .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, threads).boxed().toList());
    plans.delete(actor, plan.id());
  }
}
