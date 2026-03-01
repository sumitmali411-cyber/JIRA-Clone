import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Comment } from '../models/comment.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private base = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  getByIssue(issueId: string): Observable<Comment[]> {
    return this.http.get<any>(`${this.base}/issues/${issueId}/comments`).pipe(map(r => r.data));
  }

  create(issueId: string, body: string, author: string): Observable<Comment> {
    return this.http.post<any>(`${this.base}/issues/${issueId}/comments`, { body, author }).pipe(map(r => r.data));
  }

  update(id: string, body: string): Observable<Comment> {
    return this.http.put<any>(`${this.base}/comments/${id}`, { body }).pipe(map(r => r.data));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<any>(`${this.base}/comments/${id}`).pipe(map(() => void 0));
  }
}
