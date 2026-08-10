export type FilterOption = { value: string; label: string };
export type FilterDefinition = {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date' | 'number' | 'autocomplete';
  options?: readonly FilterOption[];
  selectionRequired?: boolean;
  placement?: 'primary' | 'advanced';
  dateStart?: string;
};
export type FilterValueEvent = { key: string; value: string };
export type FilterAutocompleteEvent = { filter: FilterDefinition; query: string };
