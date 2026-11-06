package ru.mirea.isp.filter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.isp.domain.ConnectionRequest;
import ru.mirea.isp.domain.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Создаёт параметризованную JPA Specification из разобранного фильтра. */
public final class ConnectionRequestSpecifications {
    private ConnectionRequestSpecifications() { }

    public static Specification<ConnectionRequest> from(ConnectionRequestFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            query.distinct(true);

            if (filter.archived() != null) predicates.add(cb.equal(root.get("archived"), filter.archived()));
            if (filter.status() != null) predicates.add(cb.equal(root.get("status"), filter.status()));
            if (filter.urgent() != null) predicates.add(cb.equal(root.get("urgent"), filter.urgent()));
            if (filter.priority() != null) predicates.add(cb.equal(root.get("priorityLevel"), filter.priority()));

            Join<ConnectionRequest, Subscriber> subscriberJoin = root.join("subscriber", JoinType.LEFT);
            if (filter.subscriber() != null) {
                predicates.add(cb.like(cb.lower(subscriberJoin.get("code")), "%" + escape(filter.subscriber()) + "%", '\\'));
            }
            if (filter.tag() != null) {
                Join<ConnectionRequest, String> tagJoin = root.joinSet("tags", JoinType.LEFT);
                predicates.add(cb.equal(cb.lower(tagJoin), filter.tag().toLowerCase(Locale.ROOT)));
            }
            if (filter.freeText() != null && !filter.freeText().isBlank()) {
                String like = "%" + escape(filter.freeText()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), like, '\\'),
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.get("address")), like, '\\'),
                        cb.like(cb.lower(root.get("phone")), like, '\\'),
                        cb.like(cb.lower(root.get("tariffPlan")), like, '\\'),
                        cb.like(cb.lower(subscriberJoin.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("externalId")), like, '\\'),
                        cb.like(cb.lower(subscriberJoin.get("code")), like, '\\')));
            }
            filter.hours().forEach(constraint -> predicates.add(switch (constraint.operator()) {
                case LT -> cb.lessThan(root.get("estimatedHours"), constraint.value());
                case LTE -> cb.lessThanOrEqualTo(root.get("estimatedHours"), constraint.value());
                case EQ -> cb.equal(root.get("estimatedHours"), constraint.value());
                case GTE -> cb.greaterThanOrEqualTo(root.get("estimatedHours"), constraint.value());
                case GT -> cb.greaterThan(root.get("estimatedHours"), constraint.value());
            }));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
