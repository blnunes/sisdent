import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, mergeMap, of, throwError } from 'rxjs';

export interface GraphQlErrorExtension {
  code?: string;
  correlationId?: string;
}

export interface GraphQlErrorPayload {
  message?: string;
  extensions?: GraphQlErrorExtension;
}

interface GraphQlResponse<T> {
  data?: T;
  errors?: GraphQlErrorPayload[];
}

/** A user-safe GraphQL failure. Raw transport payloads deliberately stay private. */
export class GraphQlUserError {
  constructor(
    readonly code: string,
    readonly message: string,
    readonly correlationId?: string,
  ) {}
}

@Injectable({ providedIn: 'root' })
export class GraphQlClientService {
  private readonly http = inject(HttpClient);

  query<T>(query: string, variables: Record<string, unknown>): Observable<T> {
    return this.http.post<GraphQlResponse<T>>('/graphql', { query, variables }).pipe(
      mergeMap((response) => {
        const error = response.errors?.[0];
        if (error) return throwError(() => this.mapGraphQlError(error));
        if (response.data) return of(response.data);
        return throwError(
          () =>
            new GraphQlUserError(
              'GRAPHQL.INVALID_RESPONSE',
              'The catalogue response could not be processed.',
            ),
        );
      }),
      // Security failures can be rejected before GraphQL creates its errors envelope.
      // They are still exposed to screens through the same safe error model.
      catchError((error: unknown) => throwError(() => this.mapTransportError(error))),
    );
  }

  private mapGraphQlError(error: GraphQlErrorPayload): GraphQlUserError {
    return new GraphQlUserError(
      error.extensions?.code ?? 'GRAPHQL.REQUEST_FAILED',
      error.message?.trim() || 'The catalogue request could not be completed.',
      error.extensions?.correlationId,
    );
  }

  private mapTransportError(error: unknown): GraphQlUserError {
    if (error instanceof GraphQlUserError) return error;
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401)
        return new GraphQlUserError(
          'AUTHENTICATION.UNAUTHORIZED',
          'Your session has expired. Please sign in again.',
        );
      if (error.status === 403)
        return new GraphQlUserError(
          'AUTHORIZATION.FORBIDDEN',
          'You are not authorized to view the country catalogue.',
        );
    }
    return new GraphQlUserError(
      'GRAPHQL.REQUEST_FAILED',
      'The catalogue request could not be completed.',
    );
  }
}
