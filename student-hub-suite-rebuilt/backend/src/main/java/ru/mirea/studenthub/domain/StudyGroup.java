package ru.mirea.studenthub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Учебная группа, на которую ссылаются карточки студентов. */
@Entity
@Table(name = "study_group")
public class StudyGroup extends AuditedVersionedEntity {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 200)
    private String name;

    @Column(name = "admission_year")
    private Integer admissionYear;

    @Column(nullable = false)
    private boolean active = true;

    protected StudyGroup() {
    }

    public StudyGroup(UUID id, String code, String name, Integer admissionYear) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.admissionYear = admissionYear;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAdmissionYear() { return admissionYear; }
    public void setAdmissionYear(Integer admissionYear) { this.admissionYear = admissionYear; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
