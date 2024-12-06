package com.microservice.clientLoanMicroservice.Services;


import com.microservice.clientLoanMicroservice.DTOS.ClientGetForm;
import com.microservice.clientLoanMicroservice.DTOS.ClientLoanForm;
import com.microservice.clientLoanMicroservice.DTOS.DocumentForm;
import com.microservice.clientLoanMicroservice.Entities.ClientEntity;
import com.microservice.clientLoanMicroservice.Entities.ClientLoanEntity;
import com.microservice.clientLoanMicroservice.Entities.LoanEntity;
import com.microservice.clientLoanMicroservice.Repositories.ClientLoanRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClientLoanService {

    @Autowired
    private ClientLoanRepository clientLoanRepository;
    @Autowired
    private RestTemplate restTemplate;
    private static final String CLIENT_SERVICE_URL = "http://localhost:8080/client-microservice/client";
    private static final String DOCUMENT_SERVICE_URL = "http://localhost:8080/document-microservice/document";
    private static final String LOAN_SERVICE_URL = "http://localhost:8080/loan-microservice/loan";

    public ResponseEntity<Object> addClientLoan(ClientLoanForm clientLoanForm) {
        ClientEntity client = getClientByRutFromMicroservice(clientLoanForm.getRut());

        if (client == null) {
            return ResponseEntity
                    .badRequest()
                    .body("No se encontró el cliente con el RUT especificado");
        }

        if (clientLoanForm.getRut() == null || clientLoanForm.getYears() == null ||
                clientLoanForm.getInterest() == null || clientLoanForm.getLoanAmount() == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Faltan datos requeridos");
        }

        // Validate monthly payment calculation
        double mensualPay = clientLoanForm.getMensualPay();
        if (mensualPay == 0) { // Assuming 0 represents "Valor obtenido"
            return ResponseEntity
                    .badRequest()
                    .body("Falta calcular el valor de la cuota mensual");
        }

        LoanEntity loantype = getLoanByNameFromMicroservice(clientLoanForm.getLoanName());
        if (loantype == null){
            return ResponseEntity
                    .badRequest()
                    .body("No se conecto correctamente el loan");
        }

        if (clientLoanForm.getInterest() < loantype.getMinInterest() || clientLoanForm.getInterest() > loantype.getMaxInterest()){
            return ResponseEntity
                    .badRequest()
                    .body("El Interes del prestamo esta fuera de los limites");
        }

        if (clientLoanForm.getYears() > loantype.getMaxYears()){
            return ResponseEntity
                    .badRequest()
                    .body("Los años del prestamo esta fuera de los limites");
        }

        float loanRatio = (float) (clientLoanForm.getLoanAmount() * 100) / clientLoanForm.getPropertyValue();
        if (loanRatio > loantype.getMaxAmount()){
            return ResponseEntity
                    .badRequest()
                    .body("La cantidad del prestamo esta fuera de los limites");
        }

        if (clientLoanForm.getDocuments().size() < loantype.getRequirements().size()){
            return ResponseEntity
                    .badRequest()
                    .body("Faltan Documentos");
        }
        // Check monthly payment to income ratio
        double cuotaIncome = (mensualPay / client.getMensualIncome()) * 100;
        if (cuotaIncome > 35) {
            return ResponseEntity
                    .badRequest()
                    .body("La cuota mensual excede el 35% del sueldo");
        }

        // Validate job years
        if (client.getJobYears() < 1) {
            return ResponseEntity
                    .badRequest()
                    .body("El cliente no tiene la antigüedad laboral mínima requerida (1 año)");
        }

        // Calculate and validate total debt ratio
        double totalDebt = client.getTotalDebt() + mensualPay;
        double debtCuota = (totalDebt / client.getMensualIncome()) * 100;
        if (debtCuota > 50) {
            return ResponseEntity
                    .badRequest()
                    .body("La deuda total excede el 50% del sueldo mensual");
        }


        // Validate age limit
        int totalYears = client.getYears() + clientLoanForm.getYears();
        if (totalYears > 70) {
            return ResponseEntity
                    .badRequest()
                    .body("El cliente excede el límite de edad permitido (70 años)");
        }


        ClientLoanEntity clientLoan = createAndSaveClientLoan(clientLoanForm, client.getId(), cuotaIncome, debtCuota, loanRatio);
        List<Long> documents = processDocuments(clientLoanForm.getDocuments(), clientLoan.getId());

        updateClientWithLoan(client, clientLoan, documents);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Se ingresó correctamente el préstamo");
    }

    private ClientEntity getClientByRutFromMicroservice(Integer rut) {
        try {
            ResponseEntity<ClientEntity> response = restTemplate.getForEntity(
                    CLIENT_SERVICE_URL + "/rut/" + rut,
                    ClientEntity.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            return null;
        }
    }

    private LoanEntity getLoanByNameFromMicroservice(String loanName) {
        try {
            ResponseEntity<LoanEntity> response = restTemplate.getForEntity(
                    LOAN_SERVICE_URL + "/" + loanName,
                    LoanEntity.class
            );
            return response.getBody();
        } catch (RestClientException e) {
            return null;
        }
    }

    private ClientLoanEntity createAndSaveClientLoan(ClientLoanForm form, Long clientId,Double cuotaIncome,Double debtCuota,Float loanRatio) {
        ClientLoanEntity clientLoan = new ClientLoanEntity();
        clientLoan.setClientId(clientId);
        setClientLoanFields(clientLoan, form);
        clientLoan.setCuotaIncome(cuotaIncome);
        clientLoan.setDebtCuota(debtCuota);
        clientLoan.setLoanRatio(loanRatio);
        return this.clientLoanRepository.save(clientLoan);
    }

    private void setClientLoanFields(ClientLoanEntity clientLoan, ClientLoanForm form) {
        clientLoan.setLoanAmount(form.getLoanAmount());
        clientLoan.setLoanName(form.getLoanName());
        clientLoan.setInterest(form.getInterest());
        clientLoan.setYears(form.getYears());
        clientLoan.setMensualPay(form.getMensualPay());
        clientLoan.setFase(form.getFase());
        clientLoan.setPropertyValue(form.getPropertyValue());
    }

    private List<Long> processDocuments(List<DocumentForm> documentForms, Long clientLoanId) {
        List<Long> documentIds = new ArrayList<>();

        for (DocumentForm docForm : documentForms) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            docForm.setClienLoanId(clientLoanId);

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

    private void updateClientWithLoan(ClientEntity client, ClientLoanEntity clientLoan,
                                      List<Long> documentsId) {
        try {
            clientLoan.setDocumentsId(documentsId);

            client.getLoansId().add(clientLoan.getId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ClientEntity> requestEntity = new HttpEntity<>(client, headers);

            restTemplate.put(
                    CLIENT_SERVICE_URL + "/update",
                    requestEntity,
                    ClientEntity.class
            );
        } catch (RestClientException e) {
            throw new RuntimeException("Error al actualizar el cliente: " + e.getMessage());
        }
    }
}
