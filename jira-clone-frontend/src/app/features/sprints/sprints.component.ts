import { Component, OnInit, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { MessageService, ConfirmationService } from 'primeng/api';
import { SprintService } from '../../core/services/sprint.service';
import { ProjectService } from '../../core/services/project.service';
import { IssueService } from '../../core/services/issue.service';
import { Sprint, SprintDto, SprintStatus } from '../../core/models/sprint.model';
import { Project } from '../../core/models/project.model';
import { Issue } from '../../core/models/issue.model';
import { forkJoin } from 'rxjs';

interface SprintWithStats extends Sprint {
  issueCount: number;
  doneCount: number;
  inProgressCount: number;
}

@Component({
  selector: 'app-sprints',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule, RouterLink,
    ButtonModule, TagModule, DialogModule,
    ConfirmDialogModule, InputTextModule, TextareaModule,
    SelectModule, ToastModule, ProgressSpinnerModule, TableModule
  ],
  providers: [MessageService, ConfirmationService],
  templateUrl: './sprints.component.html',
  styleUrl: './sprints.component.scss'
})
export class SprintsComponent implements OnInit {
  private sprintService = inject(SprintService);
  private projectService = inject(ProjectService);
  private issueService = inject(IssueService);
  private messageService = inject(MessageService);
  private confirmationService = inject(ConfirmationService);
  private cdr = inject(ChangeDetectorRef);

  loading = true;
  submitting = false;

  projects: Project[] = [];
  selectedProjectId = '';
  allIssues: Issue[] = [];
  sprints: SprintWithStats[] = [];

  showCreateDialog = false;
  showEditDialog = false;
  editingSprint: Sprint | null = null;

  createForm: SprintDto = { projectId: '', name: '' };
  editForm: Partial<SprintDto> = {};

  get projectOptions(): { label: string; value: string }[] {
    return this.projects.map(p => ({ label: p.name, value: p.id }));
  }

  get activeSprints(): SprintWithStats[] {
    return this.sprints.filter(s => s.status === 'ACTIVE');
  }

  get plannedSprints(): SprintWithStats[] {
    return this.sprints.filter(s => s.status === 'PLANNED');
  }

  get completedSprints(): SprintWithStats[] {
    return this.sprints.filter(s => s.status === 'COMPLETED');
  }

  ngOnInit(): void {
    this.projectService.getAll().subscribe({
      next: projects => {
        this.projects = projects;
        if (projects.length > 0) {
          this.selectedProjectId = projects[0].id;
          this.loadProjectData();
        } else {
          this.loading = false;
          this.cdr.markForCheck();
        }
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onProjectChange(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.loadProjectData();
  }

  private loadProjectData(): void {
    forkJoin({
      sprints: this.sprintService.getByProject(this.selectedProjectId),
      issues: this.issueService.getByProject(this.selectedProjectId)
    }).subscribe({
      next: ({ sprints, issues }) => {
        this.allIssues = issues;
        this.sprints = sprints
          .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
          .map(s => this.enrichSprint(s, issues));
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private enrichSprint(sprint: Sprint, issues: Issue[]): SprintWithStats {
    const sprintIssues = issues.filter(i => i.sprintId === sprint.id);
    return {
      ...sprint,
      issueCount: sprintIssues.length,
      doneCount: sprintIssues.filter(i => i.status === 'Done').length,
      inProgressCount: sprintIssues.filter(i => i.status === 'In Progress').length
    };
  }

  openCreateDialog(): void {
    this.createForm = { projectId: this.selectedProjectId, name: '', goal: '' };
    this.showCreateDialog = true;
  }

  createSprint(): void {
    if (!this.createForm.name) return;
    this.submitting = true;
    this.sprintService.create(this.selectedProjectId, this.createForm).subscribe({
      next: sprint => {
        const enriched = this.enrichSprint(sprint, this.allIssues);
        this.sprints = [enriched, ...this.sprints];
        this.showCreateDialog = false;
        this.submitting = false;
        this.messageService.add({ severity: 'success', summary: 'Sprint created', detail: sprint.name });
        this.cdr.markForCheck();
      },
      error: () => {
        this.submitting = false;
        this.messageService.add({ severity: 'error', summary: 'Failed to create sprint' });
        this.cdr.markForCheck();
      }
    });
  }

  openEditDialog(sprint: Sprint): void {
    this.editingSprint = sprint;
    this.editForm = {
      name: sprint.name,
      goal: sprint.goal,
      startDate: sprint.startDate ? sprint.startDate.substring(0, 10) : '',
      endDate: sprint.endDate ? sprint.endDate.substring(0, 10) : ''
    };
    this.showEditDialog = true;
  }

  saveSprint(): void {
    if (!this.editingSprint || !this.editForm.name) return;
    this.submitting = true;
    this.sprintService.update(this.editingSprint.id, this.editForm).subscribe({
      next: updated => {
        const idx = this.sprints.findIndex(s => s.id === updated.id);
        if (idx >= 0) {
          this.sprints[idx] = this.enrichSprint(updated, this.allIssues);
          this.sprints = [...this.sprints];
        }
        this.showEditDialog = false;
        this.submitting = false;
        this.messageService.add({ severity: 'success', summary: 'Sprint updated' });
        this.cdr.markForCheck();
      },
      error: () => {
        this.submitting = false;
        this.messageService.add({ severity: 'error', summary: 'Failed to update sprint' });
        this.cdr.markForCheck();
      }
    });
  }

  startSprint(sprint: Sprint): void {
    this.confirmationService.confirm({
      message: `Start sprint "${sprint.name}"? Once started, it will become the active sprint.`,
      header: 'Start Sprint',
      icon: 'pi pi-play',
      acceptLabel: 'Start Sprint',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-info',
      accept: () => {
        this.sprintService.start(sprint.id).subscribe({
          next: updated => {
            const idx = this.sprints.findIndex(s => s.id === updated.id);
            if (idx >= 0) {
              this.sprints[idx] = this.enrichSprint(updated, this.allIssues);
              this.sprints = [...this.sprints];
            }
            this.messageService.add({ severity: 'success', summary: 'Sprint started' });
            this.cdr.markForCheck();
          },
          error: (e: { error?: { message?: string } }) => {
            this.messageService.add({ severity: 'error', summary: e.error?.message || 'Failed to start sprint' });
            this.cdr.markForCheck();
          }
        });
      }
    });
  }

  completeSprint(sprint: Sprint): void {
    this.confirmationService.confirm({
      message: `Complete sprint "${sprint.name}"? All incomplete issues will move to the backlog.`,
      header: 'Complete Sprint',
      icon: 'pi pi-check-circle',
      acceptLabel: 'Complete Sprint',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => {
        this.sprintService.complete(sprint.id).subscribe({
          next: updated => {
            const idx = this.sprints.findIndex(s => s.id === updated.id);
            if (idx >= 0) {
              this.sprints[idx] = this.enrichSprint(updated, this.allIssues);
              this.sprints = [...this.sprints];
            }
            this.messageService.add({ severity: 'success', summary: 'Sprint completed' });
            this.cdr.markForCheck();
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Failed to complete sprint' });
            this.cdr.markForCheck();
          }
        });
      }
    });
  }

  deleteSprint(sprint: Sprint): void {
    this.confirmationService.confirm({
      message: `Delete sprint "${sprint.name}"? This action cannot be undone.`,
      header: 'Delete Sprint',
      icon: 'pi pi-trash',
      acceptLabel: 'Delete',
      rejectLabel: 'Cancel',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.sprintService.delete(sprint.id).subscribe({
          next: () => {
            this.sprints = this.sprints.filter(s => s.id !== sprint.id);
            this.messageService.add({ severity: 'success', summary: 'Sprint deleted' });
            this.cdr.markForCheck();
          },
          error: () => {
            this.messageService.add({ severity: 'error', summary: 'Failed to delete sprint' });
            this.cdr.markForCheck();
          }
        });
      }
    });
  }

  getStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    return status === 'ACTIVE' ? 'success' : status === 'COMPLETED' ? 'secondary' : 'info';
  }

  getProgressPercent(sprint: SprintWithStats): number {
    if (sprint.issueCount === 0) return 0;
    return Math.round((sprint.doneCount / sprint.issueCount) * 100);
  }
}
