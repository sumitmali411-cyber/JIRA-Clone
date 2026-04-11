import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Issue } from '../models/issue.model';
import { environment } from '../../../environments/environment';

export interface SearchFilter {
  q?: string;
  projectId?: string;
  type?: string;
  status?: string;
  priority?: string;
  assignee?: string;
  label?: string;
  sprintId?: string;
}

@Injectable({ providedIn: 'root' })
export class SearchService {
  private base = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  search(filter: SearchFilter): Observable<Issue[]> {
    let params = new HttpParams();
    if (filter.q) params = params.set('q', filter.q);
    if (filter.projectId) params = params.set('projectId', filter.projectId);
    if (filter.type) params = params.set('type', filter.type);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.priority) params = params.set('priority', filter.priority);
    if (filter.assignee) params = params.set('assignee', filter.assignee);
    if (filter.label) params = params.set('label', filter.label);
    if (filter.sprintId) params = params.set('sprintId', filter.sprintId);
    return this.http.get<any>(`${this.base}/search`, { params }).pipe(map(r => r.data));
  }
}
