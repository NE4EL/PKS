package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.exception.BusinessException;
import ru.mirea.ispteam.exception.DatabaseException;
import ru.mirea.ispteam.exception.EntityNotFoundException;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.util.InputReader;

/*
 * Владелец: C (UI).
 * Общие помощники консольного интерфейса.
 */
final class ConsoleHelper {

    private ConsoleHelper() {
    }

    /*
     * Выполняет действие меню и превращает исключения нижних слоёв в понятные сообщения.
     * Благодаря этому ошибка в одном действии не роняет приложение — пользователь
     * видит причину и возвращается в меню.
     * Возвращает true, если действие выполнилось без ошибок.
     */
    static boolean runSafely(Runnable action) {
        try {
            action.run();
            return true;
        } catch (InputReader.InputClosedException e) {
            throw e; // конец ввода — пробрасываем наверх, ConsoleApp завершит работу
        } catch (ValidationException e) {
            printError("Некорректные данные: " + e.getMessage());
        } catch (EntityNotFoundException e) {
            printError("Не найдено: " + e.getMessage());
        } catch (BusinessException e) {
            printError("Операция запрещена: " + e.getMessage());
        } catch (DatabaseException e) {
            printError("Ошибка базы данных: " + e.getMessage());
        } catch (UnsupportedOperationException e) {
            printError("Функция ещё не реализована: " + e.getMessage());
        } catch (RuntimeException e) {
            printError("Непредвиденная ошибка: " + e);
        }
        return false;
    }

    /*
     * Очищает экран ANSI-последовательностью:
     * ESC[H — курсор в левый верхний угол, ESC[2J — стереть экран,
     * ESC[3J — стереть историю прокрутки. ESC[3J идёт последним: некоторые терминалы
     * (например, VS Code) при ESC[2J не стирают текст, а сдвигают его в историю —
     * ESC[3J удаляет его и оттуда (в альтернативном экране истории нет — там это
     * просто страховка на случай, если терминал альтернативный экран не поддерживает).
     * Работает только в настоящем терминале; если вывод идёт в консоль IDE или в файл
     * (System.console() == null), ничего не делаем — иначе там вместо очистки
     * появились бы мусорные символы.
     */
    static void clearScreen() {
        if (System.console() == null) {
            return;
        }
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }

    private static boolean inAlternateScreen = false;

    /*
     * Переключает терминал на альтернативный экран (ESC[?1049h) — как делают vim, htop, less.
     * Программа рисует там, а при выходе терминал возвращается к прежнему виду
     * со всеми командами, которые были до запуска.
     */
    static synchronized void enterAlternateScreen() {
        if (System.console() == null || inAlternateScreen) {
            return;
        }
        System.out.print("\033[?1049h");
        System.out.flush();
        inAlternateScreen = true;
    }

    /*
     * Возвращает обычный экран (ESC[?1049l). Безопасно вызывать повторно:
     * вызывается и из finally, и из shutdown hook (Ctrl+C), сработает только один раз.
     */
    static synchronized void exitAlternateScreen() {
        if (!inAlternateScreen) {
            return;
        }
        System.out.print("\033[?1049l");
        System.out.flush();
        inAlternateScreen = false;
    }

    static void printError(String message) {
        System.out.println();
        System.out.println("[ОШИБКА] " + message);
    }

    static void printSuccess(String message) {
        System.out.println();
        System.out.println("[OK] " + message);
    }

    static void printHeader(String title) {
        System.out.println();
        System.out.println(title);
        System.out.println("=".repeat(title.length()));
    }
}
