package com.timemachine.contact_service.service;


import com.timemachine.contact_service.dto.ContactRequest;
import com.timemachine.contact_service.dto.ContactResponse;

import java.util.List;


public interface ContactService {
    ContactResponse processContact(ContactRequest request);
    List<ContactResponse> getAllContacts();

}