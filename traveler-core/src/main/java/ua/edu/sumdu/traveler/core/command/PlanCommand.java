package ua.edu.sumdu.traveler.core.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data of a plan entered by the user. version is the value the user saw when opening the form; it
 * is null when a plan is created.
 */
public record PlanCommand(
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal budget,
    String currency,
    boolean isPublic,
    Integer version) {}
