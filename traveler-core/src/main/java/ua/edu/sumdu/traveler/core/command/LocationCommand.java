package ua.edu.sumdu.traveler.core.command;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Data of a route location entered by the user. visitOrder is used only when an existing location
 * is edited (a new location always goes to the end of the route); version is null for a new one.
 */
public record LocationCommand(
    String name,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    OffsetDateTime arrivalDate,
    OffsetDateTime departureDate,
    BigDecimal budget,
    String notes,
    Integer visitOrder,
    Integer version) {}
