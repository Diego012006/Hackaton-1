package com.example.demo.entity;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int units;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String branch;

    @Column(nullable = false)
    private LocalDateTime soldAt;

    @Column(nullable = false)
    private String createdBy;
}
