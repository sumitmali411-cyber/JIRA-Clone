import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Issue, IssueDto, StatusUpdateDto } from '../models/issue.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class IssueService {
  private base = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  getByProject(projectId: string): Observable<Issue[]> {
    return this.http.get<any>(`${this.base}/projects/${projectId}/issues`).pipe(map(r => r.data));
  }

  getById(id: string): Observable<Issue> {
    return this.http.get<any>(`${this.base}/issues/${id}`).pipe(map(r => r.data));
  }

  create(projectId: string, dto: IssueDto): Observable<Issue> {
    return this.http.post<any>(`${this.base}/projects/${projectId}/issues`, dto).pipe(map(r => r.data));
  }

  update(id: string, dto: Partial<IssueDto>): Observable<Issue> {
    return this.http.put<any>(`${this.base}/issues/${id}`, dto).pipe(map(r => r.data));
  }

  updateStatus(id: string, dto: StatusUpdateDto): Observable<Issue> {
    return this.http.patch<any>(`${this.base}/issues/${id}/status`, dto).pipe(map(r => r.data));
  }

  updateSprint(id: string, sprintId: string | null): Observable<Issue> {
    return this.http.patch<any>(`${this.base}/issues/${id}/sprint`, { sprintId }).pipe(map(r => r.data));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<any>(`${this.base}/issues/${id}`).pipe(map(() => void 0));
  }
}
