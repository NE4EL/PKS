package ru.mirea.studenthub.service;

import org.springframework.stereotype.Component;
import ru.mirea.studenthub.domain.Student;
import ru.mirea.studenthub.dto.StudentDtos.StudentResponse;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Преобразует entity в публичный DTO внутри активной транзакции. */
@Component
public class StudentMapper {
    public StudentResponse toResponse(Student student) {
        String fullName = Stream.of(student.getLastName(), student.getFirstName(), student.getMiddleName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        return new StudentResponse(student.getId(), student.getExternalId(), student.getFirstName(),
                student.getLastName(), student.getMiddleName(), fullName, student.getEmail(),
                student.getPhone(), student.getBirthDate(),
                student.getGroup() == null ? null : student.getGroup().getId(),
                student.getGroup() == null ? null : student.getGroup().getCode(),
                student.getStatus(), student.getAverageGrade(), student.isHasDebt(), student.getRiskLevel(),
                student.isArchived(), student.getArchivedAt(), Set.copyOf(student.getTags()),
                student.getCreatedAt(), student.getUpdatedAt(), student.getVersion());
    }
}
