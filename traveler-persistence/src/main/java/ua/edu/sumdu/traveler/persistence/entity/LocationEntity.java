package ua.edu.sumdu.traveler.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Table locations. The plan is referenced by its id only: deleting a plan removes its locations
 * by ON DELETE CASCADE in the database, so no JPA collection is needed.
 */
@Entity
@Table(name = "locations")
public class LocationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "travel_plan_id", nullable = false, updatable = false)
  private UUID travelPlanId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(columnDefinition = "text")
  private String address;

  @Column(precision = 10, scale = 6)
  private BigDecimal latitude;

  @Column(precision = 11, scale = 6)
  private BigDecimal longitude;

  /** Unique within the plan: UNIQUE (travel_plan_id, visit_order) DEFERRABLE. */
  @Column(name = "visit_order", nullable = false)
  private int visitOrder;

  @Column(name = "arrival_date")
  private OffsetDateTime arrivalDate;

  @Column(name = "departure_date")
  private OffsetDateTime departureDate;

  @Column(precision = 10, scale = 2)
  private BigDecimal budget;

  @Column(columnDefinition = "text")
  private String notes;

  @Version
  @Column(nullable = false)
  private Integer version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
  }

  public UUID getId() { return id; }
  public UUID getTravelPlanId() { return travelPlanId; }
  public void setTravelPlanId(UUID travelPlanId) { this.travelPlanId = travelPlanId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getAddress() { return address; }
  public void setAddress(String address) { this.address = address; }
  public BigDecimal getLatitude() { return latitude; }
  public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
  public BigDecimal getLongitude() { return longitude; }
  public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
  public int getVisitOrder() { return visitOrder; }
  public void setVisitOrder(int visitOrder) { this.visitOrder = visitOrder; }
  public OffsetDateTime getArrivalDate() { return arrivalDate; }
  public void setArrivalDate(OffsetDateTime arrivalDate) { this.arrivalDate = arrivalDate; }
  public OffsetDateTime getDepartureDate() { return departureDate; }
  public void setDepartureDate(OffsetDateTime departureDate) { this.departureDate = departureDate; }
  public BigDecimal getBudget() { return budget; }
  public void setBudget(BigDecimal budget) { this.budget = budget; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public Instant getCreatedAt() { return createdAt; }
}
