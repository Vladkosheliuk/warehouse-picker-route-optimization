package com.warehouse.picker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false,
            unique = true, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "weight_kg", nullable = false,
            precision = 10, scale = 3)
    private BigDecimal weightKg;

    public Product(){
    }

    public Long getId(){
        return id;
    }

    public String getSku(){
        return sku;
    }

    public void setSku(String sku){
        this.sku = sku;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public BigDecimal getWeightKg(){
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg){
        this.weightKg = weightKg;
    }
}
