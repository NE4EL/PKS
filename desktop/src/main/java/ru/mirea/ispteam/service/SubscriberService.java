package ru.mirea.ispteam.service;

import ru.mirea.ispteam.exception.BusinessException;
import ru.mirea.ispteam.exception.EntityNotFoundException;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.repository.ConnectionRequestRepository;
import ru.mirea.ispteam.repository.SubscriberRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Владелец: B. CRUD абонентов и проверки из PKS.md, раздел 2.4. */
public class SubscriberService {
    private final SubscriberRepository subscriberRepository;
    private final ConnectionRequestRepository requestRepository;

    public SubscriberService(SubscriberRepository subscriberRepository,
                             ConnectionRequestRepository requestRepository) {
        this.subscriberRepository = subscriberRepository;
        this.requestRepository = requestRepository;
    }

    public Subscriber create(Subscriber subscriber) {
        Subscriber validated = validate(subscriber);
        LocalDate registrationDate = subscriber.getRegistrationDate() == null
                ? LocalDate.now() : subscriber.getRegistrationDate();
        // КР2: логическая корректность даты — регистрация не может быть в будущем.
        if (registrationDate.isAfter(LocalDate.now())) {
            throw new ValidationException("registrationDate", "Дата регистрации не может быть позже сегодняшней");
        }
        checkUniqueContacts(validated, null);
        validated.setRegistrationDate(registrationDate);
        return subscriberRepository.save(validated);
    }

    public Subscriber getById(Long id) {
        if (id == null || id <= 0) {
            throw new ValidationException("ID абонента должен быть положительным числом");
        }
        return subscriberRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Абонент с id=" + id + " не найден"));
    }

    public List<Subscriber> getAll() {
        return subscriberRepository.findAll();
    }

    public Subscriber update(Long id, Subscriber updated) {
        Subscriber current = getById(id);
        Subscriber validated = validate(updated);
        checkUniqueContacts(validated, id);
        validated.setId(id);
        validated.setRegistrationDate(current.getRegistrationDate());
        return subscriberRepository.update(validated);
    }

    public void delete(Long id) {
        getById(id);
        List<ConnectionRequest> requests = requestRepository.findBySubscriberId(id);
        if (requests.stream().anyMatch(r -> r.getStatus().isActive())) {
            throw new BusinessException("Нельзя удалить абонента с активными заявками");
        }
        // В схеме A внешний ключ без CASCADE: сначала удаляем терминальные заявки.
        for (ConnectionRequest request : requests) {
            requestRepository.deleteById(request.getId());
        }
        subscriberRepository.deleteById(id);
    }

    private Subscriber validate(Subscriber subscriber) {
        if (subscriber == null) {
            throw new ValidationException("Данные абонента обязательны");
        }
        String name = required(subscriber.getFullName(), "fullName", "ФИО", 255);
        // Unicode-буквы позволяют использовать русские и иностранные имена.
        if (!name.matches("\\p{L}+(?:[\\s'’\\-]\\p{L}+)*")) {
            throw new ValidationException("fullName",
                    "ФИО должно содержать буквы, разделённые пробелом, дефисом или апострофом");
        }
        String phone = required(subscriber.getPhone(), "phone", "Телефон", 20);
        if (!phone.matches("\\+7[0-9]{10}")) {
            throw new ValidationException("phone", "Телефон должен иметь формат +7XXXXXXXXXX");
        }
        String address = required(subscriber.getAddress(), "address", "Адрес", 500);
        String email = subscriber.getEmail() == null ? null : subscriber.getEmail().strip();
        if (email != null && email.isEmpty()) email = null;
        if (email != null && (email.length() > 255
                || !email.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+"))) {
            throw new ValidationException("email", "Некорректный email (пример: name@example.com)");
        }
        return new Subscriber(name, phone, email, address, null);
    }

    private void checkUniqueContacts(Subscriber subscriber, Long excludedId) {
        for (Subscriber existing : subscriberRepository.findAll()) {
            if (Objects.equals(existing.getId(), excludedId)) continue;
            if (subscriber.getPhone().equals(existing.getPhone())) {
                throw new BusinessException("Абонент с таким телефоном уже существует");
            }
            if (subscriber.getEmail() != null && subscriber.getEmail().equals(existing.getEmail())) {
                throw new BusinessException("Абонент с таким email уже существует");
            }
        }
    }

    private static String required(String value, String field, String label, int maxLength) {
        if (value == null || value.isBlank() || value.strip().length() > maxLength) {
            throw new ValidationException(field, label + ": обязательное поле, максимум " + maxLength + " символов");
        }
        return value.strip();
    }
}
