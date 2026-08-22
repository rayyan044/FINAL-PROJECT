package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET status = 'DELETED', deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false AND status <> 'DELETED'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role roleEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "password_changed", nullable = false)
    @Builder.Default
    private boolean passwordChanged = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", unique = true)
    private Driver driver;

    /** Company represented by this user when their role is CUSTOMER. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Legacy getter helper to return the UserRole enum for backwards compatibility
    public UserRole getRole() {
        if (roleEntity == null) {
            return null;
        }
        try {
            return UserRole.valueOf(roleEntity.getRoleName());
        } catch (Exception e) {
            return null;
        }
    }

    // Helper to assign a legacy role name
    public void setRole(UserRole userRole) {
        // Handled in service layer when mapped to Role entity,
        // but kept here for builder compatibility.
    }
}
