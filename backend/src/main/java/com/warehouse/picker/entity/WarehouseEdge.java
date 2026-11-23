package com.warehouse.picker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "warehouse_edges")
public class WarehouseEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", nullable = false)
    private WarehouseNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", nullable = false)
    private WarehouseNode toNode;

    @Column(name = "distance_m", nullable = false,
            precision = 10, scale = 2)
    private BigDecimal distanceM;

    @Column(name = "is_bidirectional", nullable = false)
    private boolean bidirectional = true;

    public WarehouseEdge(){
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

    public WarehouseNode getFromNode(){
        return fromNode;
    }

    public void setFromNode(WarehouseNode fromNode){
        this.fromNode = fromNode;
    }

    public WarehouseNode getToNode(){
        return toNode;
    }

    public void setToNode(WarehouseNode toNode){
        this.toNode = toNode;
    }

    public BigDecimal getDistanceM(){
        return distanceM;
    }

    public void setDistanceM(BigDecimal distanceM){
        this.distanceM = distanceM;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public void setBidirectional(boolean bidirectional){
        this.bidirectional = bidirectional;
    }


}
