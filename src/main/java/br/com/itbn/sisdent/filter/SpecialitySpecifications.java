package br.com.itbn.sisdent.filter;

import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.model.CatalogStatus;
import org.springframework.data.jpa.domain.Specification;

/** Reusable database predicates for the speciality resource. */
public final class SpecialitySpecifications {

    private SpecialitySpecifications() {
    }

    public static Specification<Speciality> matching(SpecialityFilter filter) {
        return (root, query, builder) -> {
            var predicate = builder.equal(root.get("status"), CatalogStatus.ACTIVE);
            String name = filter.normalized(filter.name());
            if (name != null) {
                predicate = builder.and(predicate,
                        builder.like(builder.lower(root.get("name")), "%" + name + "%"));
            }
            String procedure = filter.normalized(filter.procedure());
            if (procedure != null) {
                query.distinct(true);
                predicate = builder.and(predicate,
                        builder.equal(root.join("procedures").get("status"), CatalogStatus.ACTIVE),
                        builder.like(builder.lower(root.join("procedures").get("name")), "%" + procedure + "%"));
            }
            return predicate;
        };
    }
}
