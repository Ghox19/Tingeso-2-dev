package com.microservice.clientMicroservice.Services;

import com.microservice.clientMicroservice.DTOS.DocumentForm;
import com.microservice.clientMicroservice.DTOS.RegisterForm;
import com.microservice.clientMicroservice.Entities.ClientEntity;
import com.microservice.clientMicroservice.Repositories.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClientService {
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private RestTemplate restTemplate;
    private static final String DOCUMENT_SERVICE_URL = "http://localhost:8080/document-microservice/document";


    public ResponseEntity<Object> addClient(RegisterForm clientForm) {
        if (clientRepository.findByRut(clientForm.getRut()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("Ya existe un cliente con el RUT especificado");
        }

        ClientEntity newClient = createClientFromForm(clientForm);
        newClient = this.clientRepository.save(newClient);

        try {
            List<Long> documentIds = processDocuments(clientForm.getDocuments(), newClient.getId());
            newClient.setDocumentsId(documentIds);
            clientRepository.save(newClient);
        } catch (RestClientException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar los documentos");
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Se ingresó correctamente el Usuario");
    }

    private ClientEntity createClientFromForm(RegisterForm form) {
        ClientEntity client = new ClientEntity();
        client.setName(form.getName());
        client.setLastName(form.getLastName());
        client.setRut(form.getRut());
        client.setEmail(form.getEmail());
        client.setYears(form.getYears());
        client.setContact(form.getContact());
        client.setJobType(form.getJobType());
        client.setMensualIncome(form.getMensualIncome());
        client.setJobYears(form.getJobYears());
        client.setTotalDebt(form.getTotalDebt());
        client.setLoansId(new ArrayList<>());
        return client;
    }

    private List<Long> processDocuments(List<DocumentForm> documentForms, Long clientId) {
        List<Long> documentIds = new ArrayList<>();

        for (DocumentForm docForm : documentForms) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            docForm.setClientId(clientId);

            HttpEntity<DocumentForm> requestEntity = new HttpEntity<>(docForm, headers);

            Long documentId = restTemplate.postForObject(
                    DOCUMENT_SERVICE_URL,
                    requestEntity,
                    Long.class
            );

            documentIds.add(documentId);
        }

        return documentIds;
    }
}
