export type FormDialogOption = { value: string; label: string };
export type FormDialogField = {
  key: string;
  label: string;
  required?: boolean;
  type?: 'text' | 'date' | 'select';
  options?: readonly FormDialogOption[];
  section?: string;
  fullWidth?: boolean;
};
export type FormDialogValues = Record<string, string>;
export type FormDialogData = {
  fields: readonly FormDialogField[];
  values?: FormDialogValues;
  title: string;
  subtitle?: string;
  saveLabel?: string;
  cancelLabel?: string;
};
