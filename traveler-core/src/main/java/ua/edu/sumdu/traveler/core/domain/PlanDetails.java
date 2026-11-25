package ua.edu.sumdu.traveler.core.domain;

import java.util.List;

/**
 * Everything the plan page shows: the plan, its ordered route, route analytics and what the
 * current actor is allowed to do with it.
 */
public record PlanDetails(
    TravelPlan plan,
    String ownerName,
    List<Location> locations,
    RouteSummary summary,
    PlanStatus status,
    boolean editable,
    boolean copyable) {}
