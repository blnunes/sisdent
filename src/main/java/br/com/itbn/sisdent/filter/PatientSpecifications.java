package br.com.itbn.sisdent.filter;

import br.com.itbn.sisdent.model.Patient;
import org.springframework.data.jpa.domain.Specification;

/** Reusable database predicates for the patient resource. */
public final class PatientSpecifications {

    private PatientSpecifications() {
    }

    public static Specification<Patient> matching(PatientFilter filter) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            predicate = equalWhenPresent(predicate, builder, root.get("id"), filter.id());
            predicate = likeWhenPresent(predicate, builder, root.get("name"), filter.normalized(filter.name()));
            predicate = equalWhenPresent(predicate, builder, root.get("birthDate"), filter.birthDate());
            predicate = equalWhenPresent(predicate, builder, root.get("active"), filter.active());
            predicate = equalWhenPresent(predicate, builder, root.get("gender"), filter.gender());
            predicate = likeWhenPresent(predicate, builder, root.get("taxId"), filter.normalized(filter.taxId()));
            predicate = equalWhenPresent(predicate, builder, root.get("identificationType"), filter.identificationType());
            predicate = likeWhenPresent(predicate, builder, root.get("identificationNumber"), filter.normalized(filter.identificationNumber()));
            if (filter.nationalityCode() != null && !filter.nationalityCode().isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.join("nationality").get("code"), filter.nationalityCode().trim().toUpperCase()));
            }
            predicate = equalWhenPresent(predicate, builder, root.get("address").get("id"), filter.addressId());
            if (filter.specialityId() != null) {
                query.distinct(true);
                predicate = builder.and(predicate, builder.equal(root.join("specialities").get("id"), filter.specialityId()));
            }
            return predicate;
        };
    }

    private static <T> jakarta.persistence.criteria.Predicate equalWhenPresent(
            jakarta.persistence.criteria.Predicate predicate,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.Expression<T> path,
            T value) {
        return value == null ? predicate : builder.and(predicate, builder.equal(path, value));
    }

    private static jakarta.persistence.criteria.Predicate likeWhenPresent(
            jakarta.persistence.criteria.Predicate predicate,
            jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.Expression<String> path,
            String value) {
        return value == null ? predicate : builder.and(predicate, builder.like(builder.lower(path), "%" + value + "%"));
    }
}
