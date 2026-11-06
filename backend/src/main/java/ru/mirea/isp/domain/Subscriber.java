package ru.mirea.isp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Абонент, на которую ссылаются карточки заявок. */
@Entity
@Table(name = "subscriber")
public class Subscriber extends AuditedVersionedEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String name;

    @Column(name = "registration_year")
    private Integer registrationYear;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 32) private String phone;
    @Column(length = 254) private String email;
    @Column(length = 500) private String address;
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }

    protected Subscriber() {
    }

    public Subscriber(UUID id, String code, String name, Integer registrationYear) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.registrationYear = registrationYear;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getRegistrationYear() { return registrationYear; }
    public void setRegistrationYear(Integer registrationYear) { this.registrationYear = registrationYear; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
