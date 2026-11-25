package ua.edu.sumdu.traveler.web.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ua.edu.sumdu.traveler.core.error.DomainException;

/**
 * Measures every call of the business services: Micrometer timer traveler.service
 * (tags service, method, outcome) and a DEBUG line with the duration. The DEBUG level can be
 * switched on at runtime through /actuator/loggers without a restart.
 */
@Aspect
@Component
public class ServiceMetricsAspect {

  private static final Logger log = LoggerFactory.getLogger(ServiceMetricsAspect.class);
  private static final long SLOW_MS = 500;

  private final MeterRegistry registry;

  public ServiceMetricsAspect(MeterRegistry registry) {
    this.registry = registry;
  }

  @Around("execution(public * ua.edu.sumdu.traveler.core.service.*Service.*(..))")
  public Object measure(ProceedingJoinPoint call) throws Throwable {
    String service = call.getSignature().getDeclaringType().getSimpleName();
    String method = call.getSignature().getName();
    long start = System.nanoTime();
    String outcome = "success";
    try {
      return call.proceed();
    } catch (DomainException e) {
      outcome = "rejected"; // expected business error: validation, rights, conflict
      throw e;
    } catch (Throwable e) {
      outcome = "error";
      throw e;
    } finally {
      long nanos = System.nanoTime() - start;
      Timer.builder("traveler.service")
          .description("Calls of the business services")
          .tag("service", service).tag("method", method).tag("outcome", outcome)
          .register(registry)
          .record(nanos, java.util.concurrent.TimeUnit.NANOSECONDS);
      long ms = nanos / 1_000_000;
      if (ms > SLOW_MS) {
        log.warn("Slow service call {}.{}: {} ms ({})", service, method, ms, outcome);
      } else {
        log.debug("{}.{} -> {} in {} ms", service, method, outcome, ms);
      }
    }
  }
}
