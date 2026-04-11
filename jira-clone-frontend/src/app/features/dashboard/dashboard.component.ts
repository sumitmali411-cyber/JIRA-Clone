import { Component, OnInit, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ChartModule } from 'primeng/chart';
import { TagModule } from 'primeng/tag';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { BadgeModule } from 'primeng/badge';
import { TableModule } from 'primeng/table';
import { MessageService } from 'primeng/api';
import { forkJoin } from 'rxjs';
import { IssueService } from '../../core/services/issue.service';
import { SprintService } from '../../core/services/sprint.service';
import { ProjectService } from '../../core/services/project.service';
import { Issue } from '../../core/models/issue.model';
import { Sprint } from '../../core/models/sprint.model';
import { Project } from '../../core/models/project.model';

interface ProjectStats {
  project: Project;
  open: number;
  inProgress: number;
  done: number;
  total: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, RouterLink,
    CardModule, ChartModule, TagModule,
    ProgressSpinnerModule, ToastModule,
    BadgeModule, TableModule
  ],
  providers: [MessageService],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private projectService = inject(ProjectService);
  private issueService = inject(IssueService);
  private sprintService = inject(SprintService);
  private cdr = inject(ChangeDetectorRef);

  loading = true;

  projects: Project[] = [];
  allIssues: Issue[] = [];
  activeSprints: Sprint[] = [];
  projectStats: ProjectStats[] = [];

  recentIssues: Issue[] = [];

  velocityChartData: Record<string, unknown> = {};
  velocityChartOptions: Record<string, unknown> = {};

  burndownChartData: Record<string, unknown> = {};
  burndownChartOptions: Record<string, unknown> = {};

  get totalOpen(): number {
    return this.allIssues.filter(i => i.status === 'To Do').length;
  }

  get totalInProgress(): number {
    return this.allIssues.filter(i => i.status === 'In Progress').length;
  }

  get totalDone(): number {
    return this.allIssues.filter(i => i.status === 'Done').length;
  }

  get totalIssues(): number {
    return this.allIssues.length;
  }

  ngOnInit(): void {
    this.projectService.getAll().subscribe({
      next: projects => {
        this.projects = projects;
        if (projects.length === 0) {
          this.loading = false;
          this.cdr.markForCheck();
          return;
        }

        const requests = projects.map(p =>
          forkJoin({
            issues: this.issueService.getByProject(p.id),
            sprints: this.sprintService.getByProject(p.id)
          })
        );

        forkJoin(requests).subscribe({
          next: results => {
            this.allIssues = results.flatMap(r => r.issues);
            this.activeSprints = results.flatMap(r => r.sprints).filter(s => s.status === 'ACTIVE');

            this.projectStats = projects.map((p, i) => {
              const pIssues = results[i].issues;
              return {
                project: p,
                open: pIssues.filter(iss => iss.status === 'To Do').length,
                inProgress: pIssues.filter(iss => iss.status === 'In Progress').length,
                done: pIssues.filter(iss => iss.status === 'Done').length,
                total: pIssues.length
              };
            });

            this.recentIssues = [...this.allIssues]
              .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
              .slice(0, 10);

            this.buildVelocityChart(results.flatMap(r => r.sprints));
            this.buildBurndownChart();
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: () => {
            this.loading = false;
            this.cdr.markForCheck();
          }
        });
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private buildVelocityChart(sprints: Sprint[]): void {
    const completed = sprints
      .filter(s => s.status === 'COMPLETED')
      .sort((a, b) => new Date(a.completedAt || a.endDate).getTime() - new Date(b.completedAt || b.endDate).getTime())
      .slice(-6);

    const labels = completed.map(s => s.name);
    const velocities = completed.map(s => {
      return this.allIssues.filter(i => i.sprintId === s.id && i.status === 'Done').length;
    });

    this.velocityChartData = {
      labels,
      datasets: [
        {
          label: 'Issues Completed',
          data: velocities,
          backgroundColor: 'rgba(99, 102, 241, 0.7)',
          borderColor: 'rgba(99, 102, 241, 1)',
          borderWidth: 1,
          borderRadius: 4
        }
      ]
    };

    this.velocityChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        title: { display: false }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { stepSize: 1 },
          grid: { color: 'rgba(0,0,0,0.05)' }
        },
        x: {
          grid: { display: false }
        }
      }
    };
  }

  private buildBurndownChart(): void {
    if (this.activeSprints.length === 0) {
      this.burndownChartData = { labels: [], datasets: [] };
      return;
    }

    const activeSprint = this.activeSprints[0];
    const sprintIssues = this.allIssues.filter(i => i.sprintId === activeSprint.id);
    const total = sprintIssues.length;

    const start = activeSprint.startDate ? new Date(activeSprint.startDate) : new Date();
    const end = activeSprint.endDate ? new Date(activeSprint.endDate) : new Date(start.getTime() + 14 * 86400000);
    const today = new Date();

    const days: string[] = [];
    const ideal: number[] = [];
    const actual: number[] = [];

    let day = new Date(start);
    let dayIndex = 0;
    const totalDays = Math.max(1, Math.ceil((end.getTime() - start.getTime()) / 86400000));

    while (day <= end) {
      days.push(day.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
      ideal.push(Math.round(total - (total * dayIndex) / totalDays));

      if (day <= today) {
        const doneByDay = sprintIssues.filter(i => {
          if (i.status !== 'Done') return false;
          const updated = new Date(i.updatedAt);
          return updated <= day;
        }).length;
        actual.push(total - doneByDay);
      }

      day = new Date(day.getTime() + 86400000);
      dayIndex++;
    }

    this.burndownChartData = {
      labels: days,
      datasets: [
        {
          label: 'Ideal',
          data: ideal,
          borderColor: 'rgba(156, 163, 175, 0.7)',
          borderDash: [5, 5],
          fill: false,
          tension: 0,
          pointRadius: 0
        },
        {
          label: 'Actual',
          data: actual,
          borderColor: 'rgba(99, 102, 241, 1)',
          backgroundColor: 'rgba(99, 102, 241, 0.1)',
          fill: true,
          tension: 0.3,
          pointRadius: 3
        }
      ]
    };

    this.burndownChartOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } }
      },
      scales: {
        y: {
          beginAtZero: true,
          ticks: { stepSize: 1 },
          grid: { color: 'rgba(0,0,0,0.05)' }
        },
        x: {
          grid: { display: false },
          ticks: { maxTicksLimit: 7 }
        }
      }
    };
  }

  getTypeIcon(type: string): string {
    const m: Record<string, string> = {
      EPIC: 'pi pi-bolt', STORY: 'pi pi-bookmark',
      TASK: 'pi pi-check-square', BUG: 'pi pi-bug', SUB_TASK: 'pi pi-sitemap'
    };
    return m[type] || 'pi pi-check-square';
  }

  getTypeColor(type: string): string {
    const m: Record<string, string> = {
      EPIC: '#9b59b6', STORY: '#27ae60', TASK: '#3498db', BUG: '#e74c3c', SUB_TASK: '#95a5a6'
    };
    return m[type] || '#3498db';
  }

  getPriorityIcon(priority: string): string {
    const m: Record<string, string> = {
      HIGHEST: 'pi pi-angle-double-up', HIGH: 'pi pi-angle-up',
      MEDIUM: 'pi pi-minus', LOW: 'pi pi-angle-down', LOWEST: 'pi pi-angle-double-down'
    };
    return m[priority] || 'pi pi-minus';
  }

  getPriorityColor(priority: string): string {
    const m: Record<string, string> = {
      HIGHEST: '#e74c3c', HIGH: '#e67e22', MEDIUM: '#3498db', LOW: '#27ae60', LOWEST: '#95a5a6'
    };
    return m[priority] || '#95a5a6';
  }

  getSprintStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    return status === 'ACTIVE' ? 'success' : status === 'COMPLETED' ? 'secondary' : 'info';
  }
}
