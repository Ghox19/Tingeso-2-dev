package com.microservice.documentMicroservice.DTOS;

import lombok.Data;

@Data
public class DocumentSafeForm {
    private Long id;
    private String name;
    private String type;
    private Boolean approved;

}