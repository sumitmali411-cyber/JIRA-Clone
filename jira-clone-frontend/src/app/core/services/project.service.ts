import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Project, ProjectDto } from '../models/project.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private base = `${environment.apiUrl}/projects`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Project[]> {
    return this.http.get<any>(this.base).pipe(map(r => r.data));
  }

  getById(id: string): Observable<Project> {
    return this.http.get<any>(`${this.base}/${id}`).pipe(map(r => r.data));
  }

  create(dto: ProjectDto): Observable<Project> {
    return this.http.post<any>(this.base, dto).pipe(map(r => r.data));
  }

  update(id: string, dto: Partial<ProjectDto>): Observable<Project> {
    return this.http.put<any>(`${this.base}/${id}`, dto).pipe(map(r => r.data));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<any>(`${this.base}/${id}`).pipe(map(() => void 0));
  }
}
