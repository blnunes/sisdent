import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { SpecialityFormDialogComponent } from './speciality-form-dialog.component';

describe('SpecialityFormDialogComponent', () => {
  const close = vi.fn();
  beforeEach(() => { close.mockReset(); TestBed.configureTestingModule({ imports: [SpecialityFormDialogComponent], providers: [{ provide: MAT_DIALOG_DATA, useValue: { title: 'Edit speciality', name: 'Dentistry', procedures: [{ id: 1, name: 'Exam' }] } }, { provide: MatDialogRef, useValue: { close } }] }); });

  it('loads, adds, and removes procedures without losing existing identifiers', () => {
    const component = TestBed.createComponent(SpecialityFormDialogComponent).componentInstance;
    component.form.controls.procedureName.setValue(' Cleaning '); component.addProcedure();
    expect(component.procedures()).toEqual([{ id: 1, name: 'Exam' }, { name: 'Cleaning' }]);
    component.removeProcedure(1);
    expect(component.procedures()).toEqual([{ id: 1, name: 'Exam' }]);
  });

  it('rejects a missing name or an empty procedure list', () => {
    const component = TestBed.createComponent(SpecialityFormDialogComponent).componentInstance;
    component.form.controls.name.setValue('   '); component.procedures.set([]); component.save();
    expect(component.proceduresTouched()).toBe(true); expect(close).not.toHaveBeenCalled();
  });

  it('returns a trimmed valid speciality', () => {
    const component = TestBed.createComponent(SpecialityFormDialogComponent).componentInstance;
    component.form.controls.name.setValue(' Dentistry '); component.save();
    expect(close).toHaveBeenCalledWith({ name: 'Dentistry', procedures: [{ id: 1, name: 'Exam' }] });
  });
});
