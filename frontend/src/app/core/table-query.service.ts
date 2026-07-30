import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Sort } from '@angular/material/sort';

export type SortDirection = 'asc' | 'desc';

export interface TableQuery {
  page: number;
  size: number;
  sort: string;
  direction: SortDirection;
  filters?: Record<string, string | boolean | null | undefined>;
}

/** Shared server-side paging and three-state ordering for every Material table. */
@Injectable({ providedIn: 'root' })
export class TableQueryService {
  readonly defaultQuery: TableQuery = { page: 0, size: 10, sort: 'id', direction: 'asc' };

  toHttpParams(query: TableQuery): HttpParams {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sort', query.sort)
      .set('direction', query.direction);
    for (const [key, value] of Object.entries(query.filters ?? {})) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, value);
      }
    }
    return params;
  }

  nextSort(current: TableQuery, change: Sort): TableQuery {
    if (change.active !== current.sort) {
      return { ...current, page: 0, sort: change.active, direction: 'asc' };
    }
    if (current.direction === 'asc') {
      return { ...current, page: 0, direction: 'desc' };
    }
    return { ...current, page: 0, sort: 'id', direction: 'asc' };
  }
}
