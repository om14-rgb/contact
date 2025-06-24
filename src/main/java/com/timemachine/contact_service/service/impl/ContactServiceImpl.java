package com.timemachine.contact_service.service.impl;


// Should be:
import com.timemachine.contact_service.dto.ContactRequest;
import com.timemachine.contact_service.dto.ContactResponse;
import com.timemachine.contact_service.model.Contact;
import com.timemachine.contact_service.repository.ContactRepository;
import com.timemachine.contact_service.service.ContactService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContactResponse processContact(ContactRequest request) {
        List<Contact> matchingPrimaries = contactRepository
                .findPrimaryContactsByEmailOrPhoneNumber(request.getEmail(), request.getPhoneNumber());

        if (matchingPrimaries.isEmpty()) {
            return createNewPrimaryContact(request);
        } else if (matchingPrimaries.size() == 1) {
            return handleSingleMatch(matchingPrimaries.get(0), request);
        } else {
            return handleMultipleMatches(matchingPrimaries, request);
        }
    }

    private ContactResponse createNewPrimaryContact(ContactRequest request) {
        Contact newContact = Contact.builder()
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .linkPrecedence(Contact.LinkPrecedence.PRIMARY)
                .build();

        Contact savedContact = contactRepository.save(newContact);
        return buildResponse(savedContact);
    }

    private ContactResponse handleSingleMatch(Contact primaryContact, ContactRequest request) {
        boolean emailExists = request.getEmail() != null &&
                (request.getEmail().equals(primaryContact.getEmail()) ||
                        contactRepository.existsByEmailAndDeletedAtIsNull(request.getEmail()));

        boolean phoneExists = request.getPhoneNumber() != null &&
                (request.getPhoneNumber().equals(primaryContact.getPhoneNumber()) ||
                        contactRepository.existsByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber()));

        if (!emailExists || !phoneExists) {
            Contact secondaryContact = Contact.builder()
                    .email(request.getEmail())
                    .phoneNumber(request.getPhoneNumber())
                    .linkPrecedence(Contact.LinkPrecedence.SECONDARY)
                    .linkedContact(primaryContact)
                    .build();

            contactRepository.save(secondaryContact);
        }

        return buildResponse(primaryContact);
    }
    private ContactResponse handleMultipleMatches(List<Contact> primaries, ContactRequest request) {
        // Safe sorting with null checks
        primaries.sort(Comparator.comparing(
                Contact::getCreatedAt,
                Comparator.nullsFirst(Comparator.naturalOrder())
        ));

        Contact oldestPrimary = primaries.get(0);

        // Convert and relink all secondary contacts
        for (int i = 1; i < primaries.size(); i++) {
            Contact contactToConvert = primaries.get(i);

            // Update the contact to be secondary
            contactToConvert.setLinkPrecedence(Contact.LinkPrecedence.SECONDARY);
            contactToConvert.setLinkedContact(oldestPrimary);
            contactRepository.save(contactToConvert);

            // Find all contacts linked to this one
            List<Contact> linkedContacts = contactRepository
                    .findByLinkedContactIdAndDeletedAtIsNull(contactToConvert.getId());

            // Update their links to point to the new primary
            linkedContacts.forEach(c -> {
                c.setLinkedContact(oldestPrimary);
                contactRepository.save(c);
            });
        }

        // Handle the new contact information
        return handleSingleMatch(oldestPrimary, request);
    }


    private ContactResponse buildResponse(Contact primaryContact) {
        List<Contact> allLinkedContacts = contactRepository.findByLinkedContactIdAndDeletedAtIsNull(primaryContact.getId());
        allLinkedContacts.add(primaryContact);

        Set<String> emails = allLinkedContacts.stream()
                .map(Contact::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> phoneNumbers = allLinkedContacts.stream()
                .map(Contact::getPhoneNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Long> secondaryIds = allLinkedContacts.stream()
                .filter(c -> c.getLinkPrecedence() == Contact.LinkPrecedence.SECONDARY)
                .map(Contact::getId)
                .collect(Collectors.toList());

        return ContactResponse.builder()
                .primaryContactId(primaryContact.getId())
                .emails(new ArrayList<>(emails))
                .phoneNumbers(new ArrayList<>(phoneNumbers))
                .secondaryContactIds(secondaryIds)
                .build();
    }

    @Override
    public List<ContactResponse> getAllContacts() {
        List<Contact> contacts = contactRepository.findAllActiveContacts();

        // Group contacts by their primary contact
        Map<Long, List<Contact>> contactsByPrimary = contacts.stream()
                .collect(Collectors.groupingBy(
                        contact -> contact.getLinkPrecedence() == Contact.LinkPrecedence.PRIMARY
                                ? contact.getId()
                                : contact.getLinkedContact().getId()
                ));

        return contactsByPrimary.entrySet().stream()
                .map(entry -> {
                    Contact primary = contacts.stream()
                            .filter(c -> c.getId().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow();

                    List<Contact> allLinked = entry.getValue();

                    return ContactResponse.builder()
                            .primaryContactId(primary.getId())
                            .emails(allLinked.stream()
                                    .map(Contact::getEmail)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.toList()))
                            .phoneNumbers(allLinked.stream()
                                    .map(Contact::getPhoneNumber)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.toList()))
                            .secondaryContactIds(allLinked.stream()
                                    .filter(c -> c.getLinkPrecedence() == Contact.LinkPrecedence.SECONDARY)
                                    .map(Contact::getId)
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }
}