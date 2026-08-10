export type DataTableColumn = {
  key: string;
  label: string;
  sortable?: boolean;
};

export type DataTableRow = {
  id: string | number;
  cells: Readonly<Record<string, string>>;
  actions?: readonly DataTableRowAction[];
};

export type DataTableRowAction = {
  key: string;
  label: string;
  icon: string;
  destructive?: boolean;
};

export type DataTableActionEvent = { rowId: string | number; action: string };
export type DataTablePageEvent = { pageIndex: number; pageSize: number };
export type DataTableSortEvent = { active: string; direction: 'asc' | 'desc' | '' };
