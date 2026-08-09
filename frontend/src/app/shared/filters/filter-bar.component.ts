import { Component, input, output, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';
import { FilterAutocompleteEvent, FilterDefinition, FilterOption, FilterValueEvent } from './filter.models';

@Component({ selector: 'app-filter-bar', imports: [NgTemplateOutlet, MatAutocompleteModule, MatButtonModule, MatCardModule, MatDatepickerModule, MatNativeDateModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, TranslatePipe], templateUrl: './filter-bar.component.html', styleUrl: './filter-bar.component.scss' })
export class FilterBarComponent {
  readonly filters = input<readonly FilterDefinition[]>([]);
  readonly values = input<Readonly<Record<string, string>>>({});
  readonly displayValues = input<Readonly<Record<string, string>>>({});
  readonly autocompleteOptions = input<Readonly<Record<string, readonly FilterOption[]>>>({});
  readonly translationPrefix = input('');
  readonly ariaLabel = input('Filters');
  readonly clearAllAriaLabel = input('Clear all');
  readonly valueChange = output<FilterValueEvent>();
  readonly autocompleteChange = output<FilterAutocompleteEvent>();
  readonly optionSelected = output<FilterValueEvent & { label: string }>();
  readonly apply = output<void>();
  readonly clear = output<string>();
  readonly clearAll = output<void>();
  readonly advancedOpen = signal(false);

  primary(): readonly FilterDefinition[] { return this.filters().filter(({ placement }) => placement !== 'advanced'); }
  advanced(): readonly FilterDefinition[] { return this.filters().filter(({ placement }) => placement === 'advanced'); }
  key(suffix: string): string { return this.translationPrefix() ? `${this.translationPrefix()}.${suffix}` : suffix; }
  dateValue(value: string | undefined): Date | null { return value ? new Date(`${value}T00:00:00`) : null; }
  dateStart(filter: FilterDefinition): Date | null { return filter.dateStart ? new Date(`${filter.dateStart}T00:00:00`) : null; }
  onDate(key: string, date: Date | null): void {
    const value = date ? [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-') : '';
    this.valueChange.emit({ key, value });
  }
}
