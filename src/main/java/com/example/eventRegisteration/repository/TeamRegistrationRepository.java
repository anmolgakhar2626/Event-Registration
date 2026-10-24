package com.example.eventRegisteration.repository;

import com.example.eventRegisteration.entity.TeamRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRegistrationRepository extends JpaRepository<TeamRegistration, Long> {

    Optional<TeamRegistration> findByGroupToken(String groupToken);
}
