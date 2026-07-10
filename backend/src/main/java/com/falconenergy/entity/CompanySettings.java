package com.falconenergy.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "company_settings")
@SQLDelete(sql = "UPDATE company_settings SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "postal_address", length = 255)
    private String postalAddress;

    @Column(name = "office_address", length = 255)
    private String officeAddress;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "logo", length = 255)
    private String logo;

    @Column(name = "signatory_name", length = 150)
    private String signatoryName;

    @Column(name = "signatory_title", length = 150)
    private String signatoryTitle;

    @Column(name = "signatory_signature", length = 255)
    private String signatorySignature;
}
