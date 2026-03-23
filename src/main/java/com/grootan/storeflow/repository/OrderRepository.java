package com.grootan.storeflow.repository;

import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByReferenceNumber(String referenceNumber);
    List<Order> findByCustomer(User customer);
}