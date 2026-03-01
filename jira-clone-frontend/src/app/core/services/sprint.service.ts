import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Sprint, SprintDto } from '../models/sprint.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SprintService {
  private base = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  getByProject(projectId: string): Observable<Sprint[]> {
    return this.http.get<any>(`${this.base}/projects/${projectId}/sprints`).pipe(map(r => r.data));
  }

  create(projectId: string, dto: SprintDto): Observable<Sprint> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/sprints`, dto).pipe(map(r => r.data));
  }

  update(id: string, dto: Partial<SprintDto>): Observable<Sprint> {
    return this.http.put<any>(`${this.base}/sprints/${id}`, dto).pipe(map(r => r.data));
  }

  start(id: string): Observable<Sprint> {
    return this.http.post<any>(`${this.base}/sprints/${id}/start`, {}).pipe(map(r => r.data));
  }

  complete(id: string): Observable<Sprint> {
    return this.http.post<any>(`${this.base}/sprints/${id}/complete`, {}).pipe(map(r => r.data));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<any>(`${this.base}/sprints/${id}`).pipe(map(() => void 0));
  }
}
