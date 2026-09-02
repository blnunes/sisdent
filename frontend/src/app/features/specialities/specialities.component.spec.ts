import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { TranslateService } from '@ngx-translate/core';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { CatalogDisplayNameService } from '../../core/catalog-display-name.service';
import { CatalogueMutationGraphqlService } from '../../core/catalogue-mutation-graphql.service';
import { SpecialityCatalogGraphqlService } from '../../core/speciality-catalog-graphql.service';
import { TableQueryService } from '../../core/table-query.service';
import { SpecialitiesComponent } from './specialities.component';

describe('SpecialitiesComponent', () => {
  const read = { list: vi.fn(() => of({ content: [] as Record<string, unknown>[], totalElements: 0 })) };
  const mutations = { saveSpeciality: vi.fn(() => of({})), deactivateSpeciality: vi.fn(() => of(undefined)) };
  let component: SpecialitiesComponent;

  beforeEach(() => {
    read.list.mockReset(); read.list.mockReturnValue(of({ content: [] as Record<string, unknown>[], totalElements: 0 }));
    mutations.saveSpeciality.mockClear(); mutations.deactivateSpeciality.mockClear();
    TestBed.configureTestingModule({ imports: [SpecialitiesComponent], providers: [
      { provide: SpecialityCatalogGraphqlService, useValue: read },
      { provide: CatalogueMutationGraphqlService, useValue: mutations },
      { provide: AuthService, useValue: { hasPermission: () => true } },
      { provide: CatalogDisplayNameService, useValue: { speciality: (r: Record<string, unknown>) => String(r['name']), procedure: (r: Record<string, unknown>) => String(r['name']) } },
      { provide: TranslateService, useValue: { instant: (key: string) => key } },
      { provide: TableQueryService, useValue: { nextSort: vi.fn() } },
      { provide: MatDialog, useValue: { open: vi.fn() } },
    ] });
    TestBed.overrideComponent(SpecialitiesComponent, { set: { template: '' } });
    component = TestBed.createComponent(SpecialitiesComponent).componentInstance;
  });

  it('loads and renders speciality procedures', () => {
    read.list.mockReturnValue(of({ content: [{ id: 1, name: 'Dental', procedures: [{ name: 'Exam' }, { name: 'Cleaning' }] }], totalElements: 1 }));
    component.load();
    expect(component.rows()[0].cells).toEqual({ name: 'Dental', procedures: 'Exam, Cleaning' });
    expect(component.loading()).toBe(false);
  });

  it('reports loading and saving failures', () => {
    read.list.mockReturnValue(throwError(() => new Error('offline'))); component.load();
    expect(component.errorMessage()).toBe('The speciality catalogue could not be loaded.');
    mutations.saveSpeciality.mockReturnValue(throwError(() => new Error('offline')));
    (component as never as { save: (r: undefined, body: unknown) => void }).save(undefined, { name: 'Dental', translations: {}, procedures: [] });
    expect(component.errorMessage()).toBe('The speciality could not be saved.');
  });

  it('opens a creation editor and persists its result', () => {
    const dialog = TestBed.inject(MatDialog) as never as { open: ReturnType<typeof vi.fn> };
    dialog.open.mockReturnValue({ afterClosed: () => of({ name: 'Dental', translations: {}, procedures: [{ name: 'Exam' }] }) });
    component.create();
    expect(mutations.saveSpeciality).toHaveBeenCalledWith(undefined, expect.objectContaining({ name: 'Dental' }));
  });

  it('cancels and confirms deactivation with failure feedback', () => {
    vi.stubGlobal('confirm', vi.fn(() => false));
    (component as never as { remove: (r: Record<string, unknown>) => void }).remove({ id: 1, name: 'Dental' });
    expect(mutations.deactivateSpeciality).not.toHaveBeenCalled();
    vi.stubGlobal('confirm', vi.fn(() => true));
    mutations.deactivateSpeciality.mockReturnValue(throwError(() => new Error('offline')));
    (component as never as { remove: (r: Record<string, unknown>) => void }).remove({ id: 1, name: 'Dental' });
    expect(component.errorMessage()).toBe('The speciality could not be deactivated.');
  });
});
