package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.dto.AdministrativeDivisionResponse;
import br.com.itbn.sisdent.dto.PageResponse;
import br.com.itbn.sisdent.service.AdministrativeDivisionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AdministrativeDivisionQueryController {
    private final AdministrativeDivisionService divisions;

    public AdministrativeDivisionQueryController(AdministrativeDivisionService divisions) {
        this.divisions = divisions;
    }

    @QueryMapping
    public PageResponse<AdministrativeDivisionResponse> administrativeDivisions(@Argument CataloguePageInput page) {
        return divisions.findPage(page == null
                ? new CataloguePageInput(null, null, null, null).toPageQuery()
                : page.toPageQuery());
    }
}
