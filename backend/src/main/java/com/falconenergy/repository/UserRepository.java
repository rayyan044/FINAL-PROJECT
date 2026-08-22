package com.falconenergy.repository;

import com.falconenergy.entity.User;
import com.falconenergy.entity.UserRole;
import com.falconenergy.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByStatus(UserStatus status);

    @Query("SELECT COUNT(u) FROM User u WHERE u.roleEntity.roleName = :#{#role.name()}")
    long countByRole(@Param("role") UserRole role);

    Optional<User> findByDriverId(Long driverId);
    boolean existsByCustomerId(Long customerId);
    List<User> findByStatus(UserStatus status);
}
