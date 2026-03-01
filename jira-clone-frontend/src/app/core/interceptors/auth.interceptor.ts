import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { KeycloakService } from '../services/keycloak.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const kc = inject(KeycloakService);
  return from(kc.getToken()).pipe(
    catchError(() => of('')),
    switchMap(token => {
      if (!token) return next(req);
      return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
    })
  );
};
