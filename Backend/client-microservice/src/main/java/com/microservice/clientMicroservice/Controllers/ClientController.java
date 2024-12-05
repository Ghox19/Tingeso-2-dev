package com.microservice.clientMicroservice.Controllers;

import com.microservice.clientMicroservice.DTOS.RegisterForm;
import com.microservice.clientMicroservice.Services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/client")
public class ClientController {
    @Autowired
    private ClientService clientService;

    //Function to add a user
    @PostMapping
    public ResponseEntity<Object> addClient(@RequestBody RegisterForm client) {
        return this.clientService.addClient(client);
    }
}
