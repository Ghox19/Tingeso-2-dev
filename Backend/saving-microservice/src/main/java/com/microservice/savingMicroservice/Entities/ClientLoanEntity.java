package com.microservice.savingMicroservice.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientLoanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private long id;

    private String loanName;
    private Integer years;
    private Integer propertyValue;
    private Float interest;
    private Integer loanAmount;
    private Float loanRatio;

    private Integer mensualPay;
    private Double cuotaIncome;
    private Double debtCuota;
    private String fase;
    private String message;
    private Integer fireInsurance;
    private Double deduction;
    private Integer totalCost;

    private Long savingId;

    private Long clientId;

    @ElementCollection
    @CollectionTable(
            name = "client_loan_document_ids",
            joinColumns = @JoinColumn(name = "client_loan_id")
    )
    @Column(name = "document_id")
    private List<Long> documentsId;
}
