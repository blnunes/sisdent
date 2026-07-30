package br.com.itbn.sisdent.filter;

import br.com.itbn.sisdent.model.Speciality;
import org.springframework.data.jpa.domain.Specification;

/** Reusable database predicates for the speciality resource. */
public final class SpecialitySpecifications {

    private SpecialitySpecifications() {
    }

    public static Specification<Speciality> matching(SpecialityFilter filter) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            String name = filter.normalized(filter.name());
            if (name != null) {
                predicate = builder.and(predicate,
                        builder.like(builder.lower(root.get("name")), "%" + name + "%"));
            }
            String procedure = filter.normalized(filter.procedure());
            if (procedure != null) {
                query.distinct(true);
                predicate = builder.and(predicate,
                        builder.like(builder.lower(root.join("procedures").get("name")), "%" + procedure + "%"));
            }
            return predicate;
        };
    }
}
