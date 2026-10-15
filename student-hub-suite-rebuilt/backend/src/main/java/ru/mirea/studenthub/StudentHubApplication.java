package ru.mirea.studenthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Точка входа серверного приложения Student Hub.
 *
 * <p>Bootstrap-класс содержит только запуск Spring Boot. Бизнес-логика, безопасность
 * и инфраструктурный код вынесены в отдельные компоненты.</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StudentHubApplication {

    /**
     * Запускает Spring Boot и создаёт контекст приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(StudentHubApplication.class, args);
    }
}
