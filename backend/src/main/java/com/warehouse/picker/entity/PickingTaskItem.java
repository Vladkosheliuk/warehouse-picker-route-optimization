package com.warehouse.picker.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "picking_task_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"task_id", "product_id"}
                )
        }
)
public class PickingTaskItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private PickingTask task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "required_quantity", nullable = false)
    private Integer requiredQuantity;

    @Column(name = "collected_quantity", nullable = false)
    private Integer collectedQuantity = 0;

    public PickingTaskItem(){
    }

    public Long getId(){
        return id;
    }

    public PickingTask getTask(){
        return task;
    }

    public void setTask(PickingTask task){
        this.task = task;
    }

    public Product getProduct(){
        return product;
    }

    public void setProduct(Product product){
        this.product = product;
    }

    public Integer getRequiredQuantity(){
        return requiredQuantity;
    }

    public void setRequiredQuantity(Integer requiredQuantity){
        this.requiredQuantity = requiredQuantity;
    }

    public Integer getCollectedQuantity(){
        return collectedQuantity;
    }

    public void setCollectedQuantity(Integer collectedQuantity){
        this.collectedQuantity = collectedQuantity;
    }

}
