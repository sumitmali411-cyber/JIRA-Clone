import { Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';

export const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', redirectTo: 'projects', pathMatch: 'full' },
      {
        path: 'projects',
        loadComponent: () => import('./features/projects/projects.component').then(m => m.ProjectsComponent)
      },
      {
        path: 'projects/:projectId/board',
        loadComponent: () => import('./features/board/board.component').then(m => m.BoardComponent)
      },
      {
        path: 'projects/:projectId/backlog',
        loadComponent: () => import('./features/backlog/backlog.component').then(m => m.BacklogComponent)
      },
      {
        path: 'issues/:issueId',
        loadComponent: () => import('./features/issue-detail/issue-detail.component').then(m => m.IssueDetailComponent)
      }
    ]
  }
];
