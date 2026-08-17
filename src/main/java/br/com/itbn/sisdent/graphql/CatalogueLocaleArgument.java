package br.com.itbn.sisdent.graphql;

import br.com.itbn.sisdent.error.ErrorCode;
import br.com.itbn.sisdent.error.ValidationException;
import br.com.itbn.sisdent.localization.SupportedCatalogLocale;
import java.util.Locale;
import java.util.IllformedLocaleException;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Parses the BFF locale argument without leaking an invalid requested value. */
@Component
public class CatalogueLocaleArgument {
    public Locale resolve(String value) {
        if (value == null) return Locale.ENGLISH;
        try {
            Locale locale = new Locale.Builder().setLanguageTag(value).build();
            if (value.isBlank() || !SupportedCatalogLocale.supports(locale)) throw unsupported();
            return locale;
        } catch (IllformedLocaleException exception) {
            throw unsupported();
        }
    }

    private ValidationException unsupported() {
        return new ValidationException(ErrorCode.CATALOG_UNSUPPORTED_LOCALE,
                Map.of("supportedLocales", SupportedCatalogLocale.supportedLanguageTags()));
    }
}
