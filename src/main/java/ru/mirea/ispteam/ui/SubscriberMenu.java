package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.service.SubscriberService;
import ru.mirea.ispteam.util.InputReader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/*
 * Владелец: C (UI).
 * Подменю "Абоненты" — список/создание/редактирование/удаление через SubscriberService.
 * Проверка данных (формат телефона, запрет удаления с активными заявками) — в сервисе (B);
 * здесь только ввод, вызов сервиса и вывод результата.
 */
public class SubscriberMenu {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String[] TABLE_HEADERS =
            {"ID", "ФИО", "Телефон", "Email", "Адрес", "Дата регистрации"};

    private final SubscriberService subscriberService;
    private final InputReader input;

    public SubscriberMenu(SubscriberService subscriberService, InputReader input) {
        this.subscriberService = subscriberService;
        this.input = input;
    }

    public void show() {
        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.printHeader("АБОНЕНТЫ");
            System.out.println("1. Список абонентов");
            System.out.println("2. Добавить абонента");
            System.out.println("3. Изменить абонента");
            System.out.println("4. Удалить абонента");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите пункт: ", 0, 4);
            if (choice == 0) {
                return;
            }
            ConsoleHelper.runSafely(() -> {
                switch (choice) {
                    case 1 -> printAll();
                    case 2 -> create();
                    case 3 -> edit();
                    case 4 -> delete();
                    default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
                }
            });
            input.waitForEnter();
        }
    }

    public void printAll() {
        ConsoleHelper.printHeader("ТАБЛИЦА: АБОНЕНТЫ");
        printTable(subscriberService.getAll());
    }

    private void create() {
        ConsoleHelper.printHeader("НОВЫЙ АБОНЕНТ");
        String fullName = input.readNonEmptyString("ФИО: ");
        String phone = input.readNonEmptyString("Телефон (+7XXXXXXXXXX): ");
        String email = input.readOptional("Email (Enter — пропустить): ");
        String address = input.readNonEmptyString("Адрес: ");

        Subscriber created = subscriberService.create(
                new Subscriber(fullName, phone, email, address, LocalDate.now()));
        ConsoleHelper.printSuccess("Абонент добавлен, ID = " + created.getId());
    }

    private void edit() {
        long id = input.readLong("ID абонента: ");
        Subscriber current = subscriberService.getById(id);
        printTable(List.of(current));

        System.out.println("Введите новые значения (Enter — оставить как есть):");
        String fullName = input.readOrKeep("ФИО", current.getFullName());
        String phone = input.readOrKeep("Телефон", current.getPhone());
        String email = input.readOrKeep("Email", current.getEmail());
        String address = input.readOrKeep("Адрес", current.getAddress());

        Subscriber updated = new Subscriber(id, fullName, phone, email, address,
                current.getRegistrationDate());
        subscriberService.update(id, updated);
        ConsoleHelper.printSuccess("Данные абонента ID = " + id + " обновлены");
    }

    private void delete() {
        long id = input.readLong("ID абонента: ");
        Subscriber current = subscriberService.getById(id);
        printTable(List.of(current));

        if (!input.readYesNo("Удалить абонента «" + current.getFullName() + "»?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        subscriberService.delete(id);
        ConsoleHelper.printSuccess("Абонент ID = " + id + " удалён");
    }

    private void printTable(List<Subscriber> subscribers) {
        List<String[]> rows = new ArrayList<>();
        for (Subscriber s : subscribers) {
            rows.add(new String[]{
                    String.valueOf(s.getId()),
                    s.getFullName(),
                    s.getPhone(),
                    s.getEmail(),
                    s.getAddress(),
                    s.getRegistrationDate() != null ? s.getRegistrationDate().format(DATE_FORMAT) : null
            });
        }
        TablePrinter.print(TABLE_HEADERS, rows);
    }
}
