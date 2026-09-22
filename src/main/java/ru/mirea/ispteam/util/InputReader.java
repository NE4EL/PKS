package ru.mirea.ispteam.util;

import java.util.Scanner;

/*
 * Владелец: C (UI). Скелет создан A.
 *
 * TODO(C): безопасное чтение ввода с консоли:
 *   - readInt / readLong с циклом повторного запроса и try/catch NumberFormatException
 *   - readNonEmptyString
 *   - readLine
 * Scanner уже создан на System.in.
 */
public class InputReader {

    private final Scanner scanner = new Scanner(System.in);

    public String readLine(String prompt) {
        throw new UnsupportedOperationException("TODO(C): реализовать readLine");
    }

    public int readInt(String prompt) {
        throw new UnsupportedOperationException("TODO(C): реализовать readInt");
    }

    public long readLong(String prompt) {
        throw new UnsupportedOperationException("TODO(C): реализовать readLong");
    }

    public String readNonEmptyString(String prompt) {
        throw new UnsupportedOperationException("TODO(C): реализовать readNonEmptyString");
    }
}
