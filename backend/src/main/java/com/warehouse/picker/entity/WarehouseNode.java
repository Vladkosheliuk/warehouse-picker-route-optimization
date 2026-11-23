package com.warehouse.picker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "warehouse_nodes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_warehouse_node_code",
                        columnNames = {"warehouse_id", "node_code"}
                )
        }
)
public class WarehouseNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "node_code", nullable = false, length = 50)
    private String nodeCode;

    @Column(name = "node_type", nullable = false, length = 30)
    private String nodeType;

    @Column(name = "x_coordinate", nullable = false,
            precision = 10, scale = 2)
    private BigDecimal xCoordinate;

    @Column(name = "y_coordinate", nullable = false,
            precision = 10, scale = 2)
    private BigDecimal yCoordinate;

    public WarehouseNode(){
    }

    public Long getId() {
        return id;
    }

    public Warehouse getWarehouse(){
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse){
        this.warehouse = warehouse;
    }

    public String getNodeCode(){
        return nodeCode;
    }

    public void setNodeCode(String nodeCode){
        this.nodeCode = nodeCode;
    }

    public String getNodeType(){
        return nodeType;
    }

    public void setNodeType(String nodeType){
        this.nodeType = nodeType;
    }

    public BigDecimal getXCoordinate(){
        return xCoordinate;
    }

    public void setXCoordinate(BigDecimal xCoordinate){
        this.xCoordinate = xCoordinate;
    }

    public BigDecimal getYCoordinate(){
        return yCoordinate;
    }

    public void setYCoordinate(BigDecimal yCoordinate){
        this.yCoordinate = yCoordinate;
    }

}
