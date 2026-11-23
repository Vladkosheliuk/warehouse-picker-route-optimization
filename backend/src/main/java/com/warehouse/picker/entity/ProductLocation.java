package com.warehouse.picker.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "product_locations",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"product_id", "node_id"}
                )
        }
)
public class ProductLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private WarehouseNode node;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 0;

    public ProductLocation(){
    }

    public Long getId(){
        return id;
    }

    public Product getProduct(){
        return product;
    }

    public void setProduct(Product product){
        this.product = product;
    }

    public WarehouseNode getNode(){
        return node;
    }

    public void setNode(WarehouseNode node){
        this.node = node;
    }

    public Integer getQuantity(){
        return quantity;
    }

    public void setQuantity(Integer quantity){
        this.quantity = quantity;
    }
}
