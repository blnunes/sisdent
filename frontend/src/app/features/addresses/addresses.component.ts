import { Component, inject } from '@angular/core';
import {
  AddressGraphqlItem,
  AddressGraphqlService,
  AddressWrite,
} from '../../core/address-graphql.service';
import { DataTableColumn } from '../../shared/data-table/data-table.models';
import { FormDialogValues } from '../../shared/dialogs/form-dialog-shell/form-dialog-shell.models';
import { CatalogueListController } from '../resource-support/catalogue-list.controller';
import { RESOURCE_PAGE_IMPORTS } from '../resource-support/resource-page.imports';
import { ResourceRecord } from '../resource-support/resource-list.controller';
import { textValue } from '../../shared/text-value';

const COLUMNS: readonly DataTableColumn[] = [
  { key: 'street', label: 'Street', sortable: true },
  { key: 'district', label: 'District', sortable: true },
  { key: 'postalCode', label: 'Postal code', sortable: true },
  { key: 'administrativeDivision', label: 'Administrative division' },
  { key: 'country', label: 'Country' },
  { key: 'actions', label: '' },
];
const FIELDS = [
  { key: 'street', label: 'Street', required: true },
  { key: 'district', label: 'District' },
  { key: 'city', label: 'City', required: true },
  { key: 'additionalInfo', label: 'Additional information' },
  { key: 'block', label: 'Block' },
  { key: 'postalCode', label: 'Postal code', required: true },
  { key: 'administrativeDivisionName', label: 'Administrative division name' },
  { key: 'administrativeDivisionCode', label: 'Administrative division code' },
  { key: 'administrativeDivisionType', label: 'Administrative division type' },
  { key: 'countryCode', label: 'Country code', required: true },
] as const;
@Component({
  selector: 'app-addresses',
  imports: [...RESOURCE_PAGE_IMPORTS],
  templateUrl: '../resource-support/resource-page.component.html',
  styleUrl: '../resource-support/resource-page.component.scss',
})
export class AddressesComponent extends CatalogueListController {
  private readonly addresses = inject(AddressGraphqlService);
  readonly activeKey = 'addresses';
  readonly title = 'MODULES.ADDRESSES';
  readonly description = 'MODULES.ADDRESSES_DESCRIPTION';
  readonly translationPrefix = 'RESOURCE';
  readonly columns = COLUMNS;
  constructor() {
    super(
      {
        maintainPermission: 'MAINTAIN_ADDRESSES',
        columns: COLUMNS,
        identifier: (record) => Number(record['id']),
        primary: (record) => textValue(record['street'], '—'),
        cells: addressCells,
      },
      {
        fields: FIELDS,
        fromRecord: addressValues,
        toRequest: addressRequest,
        title: (editing) => (editing ? 'Edit address' : 'New address'),
      },
    );
    this.load();
  }
  override load(): void {
    this.loading.set(true);
    this.error.set(false);
    this.addresses.list(this.page(), this.pageSize(), this.sort(), this.sortDirection()).subscribe({
      next: (response) => {
        this.records.set(response.content);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
  protected override save(record: ResourceRecord | undefined, body: unknown): void {
    this.addresses
      .save(record as AddressGraphqlItem | undefined, body as AddressWrite)
      .subscribe({ next: () => this.load(), error: () => this.error.set(true) });
  }
  protected override remove(record: ResourceRecord): void {
    if (
      !confirm(
        this.translate.instant('RESOURCE.DELETE_CONFIRM', {
          name: textValue(record['street'], '—'),
        }),
      )
    )
      return;
    this.addresses
      .delete(String(record['id']))
      .subscribe({ next: () => this.load(), error: () => this.error.set(true) });
  }
}
function nested(record: ResourceRecord, key: string): ResourceRecord {
  const value = record[key];
  return value && typeof value === 'object' && !Array.isArray(value)
    ? (value as ResourceRecord)
    : {};
}
function addressCells(record: ResourceRecord): Readonly<Record<string, string>> {
  return {
    street: textValue(record['street'], '—'),
    district: textValue(record['district'], '—'),
    postalCode: textValue(record['postalCode'], '—'),
    administrativeDivision: textValue(nested(record, 'administrativeDivision')['name'], '—'),
    country: textValue(nested(record, 'country')['name'], '—'),
  };
}
function addressValues(record: ResourceRecord): FormDialogValues {
  const division = nested(record, 'administrativeDivision');
  const country = nested(record, 'country');
  return {
    street: textValue(record['street']),
    district: textValue(record['district']),
    city: textValue(record['city']),
    additionalInfo: textValue(record['additionalInfo']),
    block: textValue(record['block']),
    postalCode: textValue(record['postalCode']),
    administrativeDivisionName: textValue(division['name']),
    administrativeDivisionCode: textValue(division['code']),
    administrativeDivisionType: textValue(division['type']),
    countryCode: textValue(country['code']),
  };
}
function addressRequest(value: FormDialogValues): unknown {
  return {
    street: value['street'],
    district: value['district'],
    city: value['city'],
    additionalInfo: value['additionalInfo'] || null,
    block: value['block'] || null,
    postalCode: value['postalCode'],
    administrativeDivision: value['administrativeDivisionCode']
      ? {
          name: value['administrativeDivisionName'],
          code: value['administrativeDivisionCode'],
          type: value['administrativeDivisionType'],
        }
      : null,
    countryCode: value['countryCode'],
  };
}
