import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { SpecialityFormDialogComponent } from './speciality-form-dialog.component';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';

describe('SpecialityFormDialogComponent', () => {
  const close = vi.fn();
  const warningDialog = { open: vi.fn(() => ({ afterClosed: () => of('continue') })) };
  beforeEach(() => {
    close.mockReset();
    warningDialog.open.mockClear();
    TestBed.configureTestingModule({
      imports: [SpecialityFormDialogComponent],
      providers: [
        provideTranslateService(),
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            title: 'Edit speciality',
            name: 'Dentistry',
            procedures: [{ id: 1, name: 'Exam' }],
          },
        },
        { provide: MatDialogRef, useValue: { close } },
      ],
    });
    TestBed.overrideProvider(MatDialog, { useValue: warningDialog });
  });

  it('loads, adds, and removes procedures without losing existing identifiers', () => {
    const component = TestBed.createComponent(SpecialityFormDialogComponent).componentInstance;
    component.form.controls.procedureName.setValue(' Cleaning ');
    component.addProcedure();
    expect(component.procedures()).toEqual([
      { id: 1, name: 'Exam' },
      { name: 'Cleaning', translations: { en: 'Cleaning' } },
    ]);
    component.removeProcedure(1);
    expect(component.procedures()).toEqual([{ id: 1, name: 'Exam' }]);
  });

  it('rejects a missing name or an empty procedure list', () => {
    const component = TestBed.createComponent(SpecialityFormDialogComponent).componentInstance;
    component.form.controls.name.setValue('   ');
    component.procedures.set([]);
    component.save();
    expect(component.proceduresTouched()).toBe(true);
    expect(close).not.toHaveBeenCalled();
  });

  it('returns a trimmed valid speciality', () => {
    const component = TestBed.createComponent(SpecialityFormDialogComponent).componentInstance;
    component.form.controls.name.setValue(' Dentistry ');
    component.save();
    expect(warningDialog.open).toHaveBeenCalled();
    expect(close).toHaveBeenCalledWith({
      name: 'Dentistry',
      translations: { en: 'Dentistry', 'pt-PT': '', nl: '' },
      procedures: [{ id: 1, name: 'Exam', translations: { en: 'Exam' } }],
    });
  });

  it('saves immediately when every speciality and procedure translation is complete', () => {
    const component = TestBed.createComponent(SpecialityFormDialogComponent).componentInstance;
    component.form.patchValue({
      translationEn: 'Dentistry',
      translationPt: 'Medicina dentária',
      translationNl: 'Tandheelkunde',
    });
    component.procedures.set([
      { id: 1, name: 'Exam', translations: { en: 'Exam', 'pt-PT': 'Exame', nl: 'Onderzoek' } },
    ]);
    component.save();
    expect(warningDialog.open).not.toHaveBeenCalled();
    expect(close).toHaveBeenCalled();
  });

  it('associates every speciality form field with an accessible label', () => {
    const fixture = TestBed.createComponent(SpecialityFormDialogComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    for (const id of [
      'speciality-name',
      'speciality-procedure-name',
      'speciality-translation-en',
      'speciality-translation-pt',
      'speciality-translation-nl',
    ]) {
      expect(element.querySelector(`input#${id}`)).not.toBeNull();
      expect(element.querySelector(`label[for="${id}"]`)).not.toBeNull();
    }

    expect(element.querySelector('section.procedures[role="region"]')).toBeNull();
    expect(element.querySelector('section.procedures')).not.toBeNull();
  });
});
