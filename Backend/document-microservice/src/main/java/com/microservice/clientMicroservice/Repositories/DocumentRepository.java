package com.microservice.clientMicroservice.Repositories;

import com.microservice.clientMicroservice.Entities.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
}
