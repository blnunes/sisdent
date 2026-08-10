import { TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { provideTranslateService } from '@ngx-translate/core';
import { FilterBarComponent } from './filter-bar.component';

describe('FilterBarComponent', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [FilterBarComponent], providers: [provideNativeDateAdapter(), provideTranslateService()] }));
  it('separates primary and advanced optional filters', () => {
    const fixture = TestBed.createComponent(FilterBarComponent);
    fixture.componentRef.setInput('filters', [{ key: 'name', label: 'Name', type: 'text' }, { key: 'date', label: 'Date', type: 'date', placement: 'advanced' }]);
    expect(fixture.componentInstance.primary().map(({ key }) => key)).toEqual(['name']);
    expect(fixture.componentInstance.advanced().map(({ key }) => key)).toEqual(['date']);
  });
  it('emits an ISO local date and safely clears a missing date', () => {
    const component = TestBed.createComponent(FilterBarComponent).componentInstance;
    const changed = vi.fn(); component.valueChange.subscribe(changed);
    component.onDate('birthDate', new Date(2020, 1, 3)); component.onDate('birthDate', null);
    expect(changed).toHaveBeenNthCalledWith(1, { key: 'birthDate', value: '2020-02-03' });
    expect(changed).toHaveBeenNthCalledWith(2, { key: 'birthDate', value: '' });
  });
});
