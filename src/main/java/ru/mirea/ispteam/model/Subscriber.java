package ru.mirea.ispteam.model;

import java.time.LocalDate;

/*
 * Владелец: D (модель). Скелет создан A: только поля/конструкторы/геттеры-сеттеры —
 * необходимый минимум, чтобы компилировался слой репозиториев (A).
 * Поля/типы — эталон из PKS.md разд. 2.1. Менять состав полей только по согласованию.
 *
 * TODO(D): добавить equals/hashCode (по id) и toString — это твоя часть (сдаётся на защите).
 */
public class Subscriber {

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private LocalDate registrationDate;

    public Subscriber() {
    }

    // Конструктор для создания нового абонента (без id — его назначит БД)
    public Subscriber(String fullName, String phone, String email, String address, LocalDate registrationDate) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.registrationDate = registrationDate;
    }

    // Полный конструктор (например, при чтении из БД)
    public Subscriber(Long id, String fullName, String phone, String email, String address, LocalDate registrationDate) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.registrationDate = registrationDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    // TODO(D): equals/hashCode по id + toString.
}
