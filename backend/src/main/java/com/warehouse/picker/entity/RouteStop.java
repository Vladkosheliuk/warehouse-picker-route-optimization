package com.warehouse.picker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
        name = "route_stops",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"route_id", "stop_order"}
                )
        }
)
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private PickingRoute route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private WarehouseNode node;

    @Column(name = "stop_order", nullable = false)
    private Integer stopOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "distance_from_previous_m",
            nullable = false, precision = 12, scale = 2)
    private BigDecimal distanceFromPreviousM;

    @Column(name = "carried_weight_after_kg",
            nullable = false, precision = 12, scale = 3)
    private BigDecimal carriedWeightAfterKg;

    public RouteStop() {
    }

    public Long getId() {
        return id;
    }

    public PickingRoute getRoute() {
        return route;
    }

    public void setRoute(PickingRoute route) {
        this.route = route;
    }

    public WarehouseNode getNode() {
        return node;
    }

    public void setNode(WarehouseNode node) {
        this.node = node;
    }

    public Integer getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(Integer stopOrder) {
        this.stopOrder = stopOrder;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getDistanceFromPreviousM() {
        return distanceFromPreviousM;
    }

    public void setDistanceFromPreviousM(BigDecimal distanceFromPreviousM) {
        this.distanceFromPreviousM = distanceFromPreviousM;
    }

    public BigDecimal getCarriedWeightAfterKg() {
        return carriedWeightAfterKg;
    }

    public void setCarriedWeightAfterKg(BigDecimal carriedWeightAfterKg){
        this.carriedWeightAfterKg = carriedWeightAfterKg;
    }
}