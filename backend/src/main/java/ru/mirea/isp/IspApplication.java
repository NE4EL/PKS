package ru.mirea.isp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Точка входа серверного приложения ИС интернет-провайдера.
 *
 * <p>Bootstrap-класс содержит только запуск Spring Boot. Бизнес-логика, безопасность
 * и инфраструктурный код вынесены в отдельные компоненты.</p>
 */
@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class IspApplication {

    /**
     * Запускает Spring Boot и создаёт контекст приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(IspApplication.class, args);
    }
}
