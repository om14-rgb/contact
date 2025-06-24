package com.timemachine.contact_service.repository;



import com.timemachine.contact_service.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    @Query("SELECT c FROM Contact c WHERE " +
            "(c.email = :email OR c.phoneNumber = :phoneNumber) " +
            "AND c.linkPrecedence = 'PRIMARY' " +
            "AND c.deletedAt IS NULL")
    List<Contact> findPrimaryContactsByEmailOrPhoneNumber(
            @Param("email") String email,
            @Param("phoneNumber") String phoneNumber
    );
    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneNumberAndDeletedAtIsNull(String phoneNumber);
    @Query("SELECT c FROM Contact c WHERE c.deletedAt IS NULL")
    List<Contact> findAllActiveContacts();
    List<Contact> findByLinkedContactIdAndDeletedAtIsNull(Long linkedId);
}
