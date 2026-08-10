import { FormDialogShellComponent } from '../../shared/dialogs/form-dialog-shell/form-dialog-shell.component';
import { FormDialogField, FormDialogValues } from '../../shared/dialogs/form-dialog-shell/form-dialog-shell.models';
import { ResourceListController, ResourceListDefinition, ResourceRecord } from './resource-list.controller';

export type CatalogueFormDefinition = { fields: readonly FormDialogField[]; toRequest: (values: FormDialogValues) => unknown; fromRecord: (record: ResourceRecord) => FormDialogValues; title: (editing: boolean) => string };

export abstract class CatalogueListController extends ResourceListController {
  protected constructor(definition: ResourceListDefinition, private readonly formDefinition: CatalogueFormDefinition) { super(definition); }
  override create(): void { this.openEditor(); }
  protected override edit(record: ResourceRecord): void { this.openEditor(record); }
  protected openEditor(record?: ResourceRecord, fields = this.formDefinition.fields): void {
    this.dialog.open(FormDialogShellComponent, { width: '760px', maxWidth: '94vw', autoFocus: 'first-tabbable', data: { fields, values: record ? this.formDefinition.fromRecord(record) : undefined, title: this.formDefinition.title(!!record) } }).afterClosed().subscribe((values?: FormDialogValues) => { if (values) this.save(record, this.formDefinition.toRequest(values)); });
  }
}
