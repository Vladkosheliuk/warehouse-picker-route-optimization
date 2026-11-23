package com.warehouse.picker.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "picking_tasks")
public class PickingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_picker_id")
    private User assignedPicker;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "NEW";

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public PickingTask(){
    }

    public Long getId(){
        return id;
    }

    public Warehouse getWarehouse(){
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse){
        this.warehouse = warehouse;
    }

    public User getCreatedBy(){
        return createdBy;
    }

    public void setCreatedBy(User createdBy){
        this.createdBy = createdBy;
    }

    public User getAssignedPicker(){
        return assignedPicker;
    }

    public void setAssignedPicker(User assignedPicker){
        this.assignedPicker = assignedPicker;
    }

    public String getStatus(){
        return status;
    }

    public void setStatus(String status){
        this.status = status;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }

    public OffsetDateTime getStartedAt(){
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt){
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt(){
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt){
        this.completedAt = completedAt;
    }


}
