package ru.mirea.studenthub.fx;

import javafx.application.Application;

/**
 * Отдельная точка входа JavaFX-приложения.
 *
 * <p>Класс не наследуется от {@code Application}: обычный {@code main} запускается
 * стандартным Java launcher, а JavaFX берётся из Maven-зависимостей проекта.</p>
 */
public final class StudentHubLauncher {

    private StudentHubLauncher() {
        // Экземпляр launcher-класса создавать не требуется.
    }

    /**
     * Запускает основной JavaFX Application.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        Application.launch(StudentHubFxApplication.class, args);
    }
}