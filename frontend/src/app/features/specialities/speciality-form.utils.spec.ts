import { parseProcedures } from './speciality-form.utils';

describe('parseProcedures', () => {
  it('keeps valid names and numeric identifiers', () => { expect(parseProcedures('[{"id":2,"name":" Cleaning "},{"name":"Exam"}]')).toEqual([{ id: 2, name: 'Cleaning' }, { name: 'Exam' }]); });
  it('rejects invalid JSON, non-arrays, and entries without a name', () => { expect(parseProcedures('invalid')).toEqual([]); expect(parseProcedures('{}')).toEqual([]); expect(parseProcedures('[null,{"id":1},{"name":"  "}]')).toEqual([]); });
});
