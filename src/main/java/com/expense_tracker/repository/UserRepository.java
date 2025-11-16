package com.expense_tracker.repository;

import com.expense_tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // User findByEmail(String email);
    Optional<User> findByEmail(String email);

}