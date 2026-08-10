package br.com.itbn.sisdent.localization;

import br.com.itbn.sisdent.model.Country;
import org.springframework.stereotype.Component;

import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Set;

@Component
public class CountryNameLocalizer implements CatalogNameLocalizer<Country> {
    private static final Set<String> ISO_COUNTRY_CODES = Set.of(Locale.getISOCountries());

    @Override
    public String localize(Country country, Locale locale) {
        if (!ISO_COUNTRY_CODES.contains(country.getCode())) {
            return country.getName();
        }
        try {
            Locale countryLocale = new Locale.Builder().setRegion(country.getCode()).build();
            String localizedName = countryLocale.getDisplayCountry(SupportedCatalogLocale.from(locale));
            return localizedName.isBlank() ? country.getName() : localizedName;
        } catch (IllformedLocaleException exception) {
            return country.getName();
        }
    }
}
