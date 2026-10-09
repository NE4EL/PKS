package ru.mirea.ispteam.fx.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

/**
 * Выбор файла для экспорта через FileChooser. Путь выбирает пользователь —
 * в программе нет ни одного «зашитого» пути на диске.
 */
public final class ExportFiles {

    private static final FileChooser.ExtensionFilter XLSX =
            new FileChooser.ExtensionFilter("Книга Excel (*.xlsx)", "*.xlsx");
    private static final FileChooser.ExtensionFilter CSV =
            new FileChooser.ExtensionFilter("Текст CSV, разделитель «;» (*.csv)", "*.csv");

    private ExportFiles() {
    }

    /**
     * @param baseName начало имени файла по умолчанию, например «zayavki»
     * @return выбранный путь или пусто, если пользователь закрыл окно
     */
    public static Optional<Path> chooseTarget(Window owner, String title, String baseName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().setAll(XLSX, CSV);
        chooser.setSelectedExtensionFilter(XLSX);
        chooser.setInitialFileName(baseName + "_" + LocalDate.now() + ".xlsx");
        File home = new File(System.getProperty("user.home"));
        if (home.isDirectory()) {
            chooser.setInitialDirectory(home);
        }
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return Optional.empty();
        }
        return Optional.of(withExtension(file, chooser.getSelectedExtensionFilter()));
    }

    // Если пользователь стёр расширение — добавляем расширение выбранного типа файла.
    private static Path withExtension(File file, FileChooser.ExtensionFilter filter) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".csv")) {
            return file.toPath();
        }
        String extension = filter == CSV ? ".csv" : ".xlsx";
        return file.toPath().resolveSibling(file.getName() + extension);
    }
}
