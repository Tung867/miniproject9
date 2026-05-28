package com.spaceres.repository;

import com.spaceres.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByRefreshToken(String refreshToken);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.refreshToken = :token WHERE u.email = :email")
    void updateRefreshToken(@Param("email") String email, @Param("token") String token);
}
