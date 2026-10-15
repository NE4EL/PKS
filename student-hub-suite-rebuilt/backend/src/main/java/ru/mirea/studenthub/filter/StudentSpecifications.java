package ru.mirea.studenthub.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.studenthub.domain.Student;
import ru.mirea.studenthub.domain.StudyGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Создаёт параметризованную JPA Specification из разобранного фильтра. */
public final class StudentSpecifications {
    private StudentSpecifications() { }

    public static Specification<Student> from(StudentFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (filter.archived() != null) predicates.add(cb.equal(root.get("archived"), filter.archived()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.debt() != null) predicates.add(cb.equal(root.get("hasDebt"), filter.debt()));
            if (filter.risk() != null) predicates.add(cb.equal(root.get("riskLevel"), filter.risk()));

            Join<Student, StudyGroup> groupJoin = root.join("group", JoinType.LEFT);
            if (filter.group() != null) {
                predicates.add(cb.like(cb.lower(groupJoin.get("code")), "%" + escape(filter.group()) + "%", '\\'));
            }
            if (filter.tag() != null) {
                Join<Student, String> tagJoin = root.joinSet("tags", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(tagJoin), filter.tag().toLowerCase(Locale.ROOT)));
            }
            if (filter.freeText() != null && !filter.freeText().isBlank()) {
                String like = "%" + escape(filter.freeText()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("lastName")), like, '\\'),
                        cb.like(cb.lower(root.get("firstName")), like, '\\'),
                        cb.like(cb.lower(root.get("middleName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("externalId")), like, '\\'),
                        cb.like(cb.lower(groupJoin.get("code")), like, '\\')));
            }
            filter.grades().forEach(constraint -> predicates.add(switch (constraint.operator()) {
                case LT -> cb.lessThan(root.get("averageGrade"), constraint.value());
                case LTE -> cb.lessThanOrEqualTo(root.get("averageGrade"), constraint.value());
                case EQ -> cb.equal(root.get("averageGrade"), constraint.value());
                case GTE -> cb.greaterThanOrEqualTo(root.get("averageGrade"), constraint.value());
                case GT -> cb.greaterThan(root.get("averageGrade"), constraint.value());
            }));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
