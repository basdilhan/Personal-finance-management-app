package com.example.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "forecasts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "forecast_month"})
})
public class ForecastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "forecast_month", nullable = false)
    private String forecastMonth; // Format: "2026-07"

    @Column(name = "predicted_expense", precision = 12, scale = 2)
    private BigDecimal predictedExpense = BigDecimal.ZERO;

    @Column(name = "predicted_income", precision = 12, scale = 2)
    private BigDecimal predictedIncome = BigDecimal.ZERO;

    @Column(name = "predicted_bills", precision = 12, scale = 2)
    private BigDecimal predictedBills = BigDecimal.ZERO;

    @Column(name = "net_cash_flow", precision = 12, scale = 2)
    private BigDecimal netCashFlow = BigDecimal.ZERO;

    @Column(name = "category_breakdown", columnDefinition = "jsonb")
    private String categoryBreakdown = "{}";

    @Column(name = "model_version")
    private String modelVersion = "chronos-t5-small";

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getForecastMonth() { return forecastMonth; }
    public void setForecastMonth(String forecastMonth) { this.forecastMonth = forecastMonth; }

    public BigDecimal getPredictedExpense() { return predictedExpense; }
    public void setPredictedExpense(BigDecimal predictedExpense) { this.predictedExpense = predictedExpense; }

    public BigDecimal getPredictedIncome() { return predictedIncome; }
    public void setPredictedIncome(BigDecimal predictedIncome) { this.predictedIncome = predictedIncome; }

    public BigDecimal getPredictedBills() { return predictedBills; }
    public void setPredictedBills(BigDecimal predictedBills) { this.predictedBills = predictedBills; }

    public BigDecimal getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(BigDecimal netCashFlow) { this.netCashFlow = netCashFlow; }

    public String getCategoryBreakdown() { return categoryBreakdown; }
    public void setCategoryBreakdown(String categoryBreakdown) { this.categoryBreakdown = categoryBreakdown; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
}
