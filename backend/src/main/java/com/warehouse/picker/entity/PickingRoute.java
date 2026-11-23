package com.warehouse.picker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "picking_routes")
public class PickingRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private PickingTask task;

    @Column(name = "algorithm", nullable = false, length = 30)
    private String algorithm;

    @Column(name = "total_distance_m", nullable = false,
            precision = 12, scale = 2)
    private BigDecimal totalDistanceM;

    @Column(name = "load_distance_kg_m", nullable = false,
            precision = 16, scale = 3)
    private BigDecimal loadDistanceKgM;

    @Column(name = "calculation_time_ms", nullable = false)
    private Integer calculationTimeMs;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PickingRoute(){
    }

    public Long getId(){
        return id;
    }

    public  PickingTask getTask(){
        return task;
    }

    public void setTask(PickingTask task){
        this.task = task;
    }

    public String getAlgorithm(){
        return algorithm;
    }

    public void setAlgorithm(String algorithm){
        this.algorithm = algorithm;
    }

    public BigDecimal getTotalDistanceM(){
        return totalDistanceM;
    }

    public void setTotalDistanceM(BigDecimal totalDistanceM){
        this.totalDistanceM = totalDistanceM;
    }

    public BigDecimal getLoadDistanceKgM(){
        return loadDistanceKgM;
    }

    public void setLoadDistanceKgM(BigDecimal loadDistanceKgM){
        this.loadDistanceKgM = loadDistanceKgM;
    }

    public Integer getCalculationTimeMs(){
        return calculationTimeMs;
    }

    public void setCalculationTimeMs(Integer calculationTimeMs){
        this.calculationTimeMs = calculationTimeMs;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }

}
