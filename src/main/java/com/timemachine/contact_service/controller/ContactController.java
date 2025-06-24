package com.timemachine.contact_service.controller;


import com.timemachine.contact_service.dto.ContactRequest;
import com.timemachine.contact_service.dto.ContactResponse;
import com.timemachine.contact_service.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/identify")
    public ResponseEntity<ContactResponse> identifyContact(@Valid @RequestBody ContactRequest request) {
        ContactResponse response = contactService.processContact(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get all consolidated contacts")
    public ResponseEntity<List<ContactResponse>> getAllContacts() {
        return ResponseEntity.ok(contactService.getAllContacts());
    }
}