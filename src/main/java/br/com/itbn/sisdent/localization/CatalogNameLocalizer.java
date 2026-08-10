package br.com.itbn.sisdent.localization;

import java.util.Locale;

/**
 * Defines how a catalogue item is presented in a requested locale without
 * changing its canonical, persisted name.
 */
@FunctionalInterface
public interface CatalogNameLocalizer<T> {

    String localize(T item, Locale locale);
}
