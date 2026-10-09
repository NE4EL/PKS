package ru.mirea.ispteam.fx;

/**
 * Главный класс для запуска из jar (java -jar target/isp-team.jar).
 * Он намеренно НЕ наследует Application: иначе Java при запуске с classpath требует JavaFX
 * в module path и выдаёт «JavaFX runtime components are missing».
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        IspFxApplication.main(args);
    }
}
