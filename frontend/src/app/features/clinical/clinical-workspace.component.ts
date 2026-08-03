import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatIconModule } from '@angular/material/icon';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { distinctUntilChanged } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { ClinicalEncounter, OdontogramFinding, PageResponse } from '../../core/models';
import { AppHeaderComponent } from '../../shared/app-header.component';
import { ModuleNavigationComponent } from '../../shared/module-navigation.component';

@Component({selector: 'app-clinical-workspace', standalone: true, imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatSidenavModule, MatIconModule, TranslatePipe, AppHeaderComponent, ModuleNavigationComponent], templateUrl: './clinical-workspace.component.html', styleUrl: './clinical-workspace.component.scss'})
export class ClinicalWorkspaceComponent {
  readonly auth = inject(AuthService); private readonly http = inject(HttpClient); private readonly destroyRef = inject(DestroyRef); private readonly translate = inject(TranslateService);
  readonly membership = this.auth.activeMembership; readonly patients = signal<{globalId:string;name:string}[]>([]); readonly encounters = signal<ClinicalEncounter[]>([]); readonly chart = signal<OdontogramFinding[]>([]); readonly error = signal('');
  patientId = ''; narrative = ''; toothCode = ''; condition = 'SOUND'; surface = 'WHOLE_TOOTH'; clinicUnitId = '';
  constructor(){toObservable(this.auth.activeMembership).pipe(distinctUntilChanged((a,b)=>a?.id===b?.id),takeUntilDestroyed(this.destroyRef)).subscribe(()=>this.reset());}
  reset(){this.patientId='';this.patients.set([]);this.encounters.set([]);this.chart.set([]);this.error.set(''); const m=this.membership(); if(!m||!this.auth.canReadClinical())return; this.clinicUnitId=m.clinicUnitId ?? ''; if(this.clinicUnitId){this.loadPatients();return;} this.http.get<{id:string}[]>(`/api/organizations/${m.organizationId}/clinic-units`).subscribe({next:units=>{this.clinicUnitId=units[0]?.id ?? '';this.loadPatients();},error:()=>this.fail('LOAD')});}
  private loadPatients(){const m=this.membership();if(!m||!this.clinicUnitId)return;this.http.get<PageResponse<{globalId:string;name:string}>>(`/api/organizations/${m.organizationId}/patients?clinicUnitId=${this.clinicUnitId}`).subscribe({next:p=>this.patients.set(p.content),error:()=>this.fail('LOAD')});}
  selectPatient(){const m=this.membership();if(!m||!this.patientId||!this.clinicUnitId)return;const q=`clinicUnitId=${encodeURIComponent(this.clinicUnitId)}&patientId=${encodeURIComponent(this.patientId)}`;this.http.get<PageResponse<ClinicalEncounter>>(`/api/organizations/${m.organizationId}/clinical/encounters?${q}`).subscribe({next:p=>this.encounters.set(p.content),error:()=>this.fail('LOAD')});this.http.get<OdontogramFinding[]>(`/api/organizations/${m.organizationId}/clinical/odontogram/current?${q}`).subscribe({next:p=>this.chart.set(p),error:()=>this.fail('LOAD')});}
  createDraft(){const m=this.membership();if(!m||!this.patientId||!this.narrative.trim())return;this.http.post<ClinicalEncounter>(`/api/organizations/${m.organizationId}/clinical/encounters`,{clinicUnitId:this.clinicUnitId,patientId:this.patientId,careAt:new Date().toISOString(),careTimezone:Intl.DateTimeFormat().resolvedOptions().timeZone,narrative:this.narrative}).subscribe({next:()=>{this.narrative='';this.selectPatient();},error:()=>this.fail('SAVE')});}
  finalize(encounter:ClinicalEncounter){const m=this.membership();if(!m)return;this.http.post(`/api/organizations/${m.organizationId}/clinical/encounters/${encounter.globalId}/finalize`,{}, {params:{clinicUnitId:this.clinicUnitId}}).subscribe({next:()=>this.selectPatient(),error:()=>this.fail('SAVE')});}
  recordFinding(){const m=this.membership();if(!m||!this.patientId)return;this.http.post(`/api/organizations/${m.organizationId}/clinical/odontogram/findings`,{clinicUnitId:this.clinicUnitId,patientId:this.patientId,toothCode:this.toothCode,surface:this.surface,condition:this.condition,observedAt:new Date().toISOString(),observationTimezone:Intl.DateTimeFormat().resolvedOptions().timeZone}).subscribe({next:()=>{this.toothCode='';this.selectPatient();},error:()=>this.fail('SAVE')});}
  voidFinding(f:OdontogramFinding){const m=this.membership();if(!m)return;this.http.post(`/api/organizations/${m.organizationId}/clinical/odontogram/findings/${f.globalId}/void`,{reason:'Clinical correction',version:f.version},{params:{clinicUnitId:this.clinicUnitId}}).subscribe({next:()=>this.selectPatient(),error:()=>this.fail('SAVE')});}
  private fail(key:string){this.error.set(this.translate.instant(`CLINICAL.ERROR.${key}`));}
}
