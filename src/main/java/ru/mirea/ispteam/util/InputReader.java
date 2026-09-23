package ru.mirea.ispteam.util;

import java.util.NoSuchElementException;
import java.util.Scanner;

/*
 * Владелец: C (UI).
 *
 * Безопасное чтение ввода с консоли. Каждый метод чтения числа/выбора крутится в цикле,
 * пока пользователь не введёт корректное значение, — приложение не падает на мусорном вводе.
 * Конец ввода (Ctrl+D) превращается в InputClosedException, чтобы ConsoleApp
 * мог корректно завершиться, а не зациклиться.
 */
public class InputReader {

    private final Scanner scanner = new Scanner(System.in);

    /** Поток ввода закрыт (EOF) — читать больше нечего, приложение должно завершиться. */
    public static class InputClosedException extends RuntimeException {
        public InputClosedException() {
            super("Ввод завершён");
        }
    }

    /** Строка как есть (без пробелов по краям), может быть пустой. */
    public String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    public int readInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                printError("Введите целое число.");
            }
        }
    }

    /** Целое число в диапазоне [min, max] — для пунктов меню. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            printError("Введите число от " + min + " до " + max + ".");
        }
    }

    public long readLong(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Long.parseLong(line);
            } catch (NumberFormatException e) {
                printError("Введите целое число.");
            }
        }
    }

    public String readNonEmptyString(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (!line.isEmpty()) {
                return line;
            }
            printError("Значение не может быть пустым.");
        }
    }

    /** Необязательное значение: пустой ввод -> null. */
    public String readOptional(String prompt) {
        String line = readLine(prompt);
        return line.isEmpty() ? null : line;
    }

    /** Редактирование поля: пустой ввод (Enter) оставляет текущее значение. */
    public String readOrKeep(String prompt, String current) {
        String shown = (current == null || current.isEmpty()) ? "—" : current;
        String line = readLine(prompt + " [" + shown + "]: ");
        return line.isEmpty() ? current : line;
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String line = readLine(prompt + " (д/н): ").toLowerCase();
            switch (line) {
                case "д", "да", "y", "yes" -> {
                    return true;
                }
                case "н", "нет", "n", "no" -> {
                    return false;
                }
                default -> printError("Ответьте «д» или «н».");
            }
        }
    }

    /**
     * Выбор значения enum по номеру из пронумерованного списка.
     * labels[i] — подпись для values[i] (обычно русское название).
     */
    public <E extends Enum<E>> E readEnum(String title, E[] values, String[] labels) {
        System.out.println(title);
        for (int i = 0; i < values.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, labels[i]);
        }
        int choice = readInt("Ваш выбор: ", 1, values.length);
        return values[choice - 1];
    }

    public void waitForEnter() {
        readLine("\nНажмите Enter, чтобы продолжить...");
    }

    private void printError(String message) {
        System.out.println("  ! " + message);
    }
}
