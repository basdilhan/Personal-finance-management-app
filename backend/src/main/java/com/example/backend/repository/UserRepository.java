package com.example.backend.repository;

import com.example.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM UserEntity u WHERE u.fcmToken IS NOT NULL AND u.fcmToken != ''")
    java.util.List<UserEntity> findUsersWithFcmTokens();
}
