package ua.edu.sumdu.traveler.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Table travel_plans. The version column is mapped with @Version: Hibernate adds
 * "AND version = ?" to every UPDATE/DELETE and increments it, which is the optimistic locking
 * the database was designed for.
 */
@Entity
@Table(name = "travel_plans")
public class TravelPlanEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** Null for plans created through the REST API of the laboratory works. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private UserEntity owner;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(precision = 10, scale = 2)
  private BigDecimal budget;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "is_public", nullable = false)
  private boolean publicPlan;

  @Version
  @Column(nullable = false)
  private Integer version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Also refreshed by the database trigger touch_plan from the laboratory schema. */
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() { return id; }
  public UserEntity getOwner() { return owner; }
  public void setOwner(UserEntity owner) { this.owner = owner; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public LocalDate getStartDate() { return startDate; }
  public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
  public LocalDate getEndDate() { return endDate; }
  public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
  public BigDecimal getBudget() { return budget; }
  public void setBudget(BigDecimal budget) { this.budget = budget; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public boolean isPublicPlan() { return publicPlan; }
  public void setPublicPlan(boolean publicPlan) { this.publicPlan = publicPlan; }
  public Integer getVersion() { return version; }
  public void setVersion(Integer version) { this.version = version; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
