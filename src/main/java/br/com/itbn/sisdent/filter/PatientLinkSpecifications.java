package br.com.itbn.sisdent.filter;

import br.com.itbn.sisdent.model.PatientOrganizationLink;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/** Tenant and clinic-unit predicates for patient links. Inactive links are never readable. */
public final class PatientLinkSpecifications {
    private PatientLinkSpecifications() {}

    public static Specification<PatientOrganizationLink> matching(UUID organizationId, UUID clinicUnitId,
            PatientFilter filter) {
        return (root, query, builder) -> {
            Join<Object, Object> patient = root.join("patient");
            var predicate = builder.and(
                    builder.equal(root.join("organization").get("globalId"), organizationId),
                    builder.isTrue(root.get("active")));
            if (clinicUnitId != null) {
                predicate = builder.and(predicate,
                        builder.equal(root.join("clinicUnit").get("globalId"), clinicUnitId));
            }
            predicate = withEqual(predicate, builder, patient.get("id"), filter.id());
            predicate = like(predicate, builder, patient.get("name"), filter.normalized(filter.name()));
            predicate = withEqual(predicate, builder, patient.get("birthDate"), filter.birthDate());
            predicate = withEqual(predicate, builder, patient.get("active"), filter.active());
            predicate = withEqual(predicate, builder, patient.get("gender"), filter.gender());
            predicate = like(predicate, builder, patient.get("taxId"), filter.normalized(filter.taxId()));
            predicate = withEqual(predicate, builder, patient.get("identificationType"), filter.identificationType());
            predicate = like(predicate, builder, patient.get("identificationNumber"), filter.normalized(filter.identificationNumber()));
            if (filter.nationalityCode() != null && !filter.nationalityCode().isBlank()) {
                predicate = builder.and(predicate, builder.equal(patient.join("nationality").get("code"),
                        filter.nationalityCode().trim().toUpperCase()));
            }
            predicate = withEqual(predicate, builder, patient.join("address").get("id"), filter.addressId());
            if (filter.specialityId() != null) {
                query.distinct(true);
                predicate = builder.and(predicate,
                        builder.equal(patient.join("specialities").get("id"), filter.specialityId()));
            }
            return predicate;
        };
    }

    private static <T> jakarta.persistence.criteria.Predicate withEqual(jakarta.persistence.criteria.Predicate predicate,
            jakarta.persistence.criteria.CriteriaBuilder builder, jakarta.persistence.criteria.Expression<T> path, T value) {
        return value == null ? predicate : builder.and(predicate, builder.equal(path, value));
    }
    private static jakarta.persistence.criteria.Predicate like(jakarta.persistence.criteria.Predicate predicate,
            jakarta.persistence.criteria.CriteriaBuilder builder, jakarta.persistence.criteria.Expression<String> path, String value) {
        return value == null ? predicate : builder.and(predicate, builder.like(builder.lower(path), "%" + value + "%"));
    }
}
