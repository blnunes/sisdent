package br.com.itbn.sisdent.localization;

import br.com.itbn.sisdent.model.Speciality;
import br.com.itbn.sisdent.model.CatalogResourceType;
import br.com.itbn.sisdent.service.CatalogTranslationService;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SpecialityNameLocalizer implements CatalogNameLocalizer<Speciality> {
    private final CatalogTranslationService translations;

    public SpecialityNameLocalizer(CatalogTranslationService translations) {
        this.translations = translations;
    }

    @Override
    public String localize(Speciality speciality, Locale locale) {
        return translations.resolve(CatalogResourceType.SPECIALITY, speciality.getId(), speciality.getName(), locale);
    }
}
