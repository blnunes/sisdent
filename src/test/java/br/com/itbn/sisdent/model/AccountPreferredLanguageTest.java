package br.com.itbn.sisdent.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountPreferredLanguageTest {
    @Test
    void defaultsNewAccountsToEnglishAndAcceptsEverySupportedUiLanguage() {
        Account account = new Account(new Person("Ana"), "ana@example.com", "encoded", false);

        assertThat(account.getPreferredLanguage()).isEqualTo("en");
        for (String language : new String[] {"pt-PT", "en", "nl"}) {
            account.changePreferredLanguage(language);
            assertThat(account.getPreferredLanguage()).isEqualTo(language);
        }
    }

    @Test
    void rejectsNullBlankMalformedAndUnsupportedUiLanguages() {
        Account account = new Account(new Person("Ana"), "ana@example.com", "encoded", false);

        for (String language : new String[] {null, "", "pt", "pt-BR", "en-US", "nl-BE"}) {
            assertThatThrownBy(() -> account.changePreferredLanguage(language)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
