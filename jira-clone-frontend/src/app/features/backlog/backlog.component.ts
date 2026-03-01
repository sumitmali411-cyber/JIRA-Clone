import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { AccordionModule } from 'primeng/accordion';
import { MessageService } from 'primeng/api';
import { ProjectService } from '../../core/services/project.service';
import { IssueService } from '../../core/services/issue.service';
import { SprintService } from '../../core/services/sprint.service';
import { Issue, IssueDto } from '../../core/models/issue.model';
import { Sprint, SprintDto } from '../../core/models/sprint.model';
import { Project } from '../../core/models/project.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-backlog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    ButtonModule, TagModule, DialogModule,
    InputTextModule, TextareaModule, SelectModule,
    ToastModule, ProgressSpinnerModule, AccordionModule
  ],
  providers: [MessageService],
  templateUrl: './backlog.component.html',
  styleUrl: './backlog.component.scss'
})
export class BacklogComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private issueService = inject(IssueService);
  private sprintService = inject(SprintService);
  private messageService = inject(MessageService);

  project: Project | null = null;
  issues: Issue[] = [];
  sprints: Sprint[] = [];
  loading = true;

  showSprintDialog = false;
  showIssueDialog = false;
  submitting = false;

  sprintForm: SprintDto = { projectId: '', name: '' };
  issueForm: IssueDto = { projectId: '', type: 'TASK', summary: '', priority: 'MEDIUM' };

  typeOptions = [
    { label: 'Epic', value: 'EPIC' }, { label: 'Story', value: 'STORY' },
    { label: 'Task', value: 'TASK' }, { label: 'Bug', value: 'BUG' }
  ];

  priorityOptions = [
    { label: 'Highest', value: 'HIGHEST' }, { label: 'High', value: 'HIGH' },
    { label: 'Medium', value: 'MEDIUM' }, { label: 'Low', value: 'LOW' }, { label: 'Lowest', value: 'LOWEST' }
  ];

  get projectId() { return this.route.snapshot.paramMap.get('projectId')!; }

  get backlogIssues() {
    return this.issues.filter(i => !i.sprintId);
  }

  sprintIssues(sprintId: string) {
    return this.issues.filter(i => i.sprintId === sprintId);
  }

  ngOnInit() {
    forkJoin({
      project: this.projectService.getById(this.projectId),
      issues: this.issueService.getByProject(this.projectId),
      sprints: this.sprintService.getByProject(this.projectId)
    }).subscribe({
      next: ({ project, issues, sprints }) => {
        this.project = project;
        this.issues = issues;
        this.sprints = sprints.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  openSprintDialog() {
    this.sprintForm = { projectId: this.projectId, name: '', goal: '' };
    this.showSprintDialog = true;
  }

  createSprint() {
    if (!this.sprintForm.name) return;
    this.submitting = true;
    this.sprintService.create(this.projectId, this.sprintForm).subscribe({
      next: sprint => {
        this.sprints.unshift(sprint);
        this.showSprintDialog = false;
        this.submitting = false;
        this.messageService.add({ severity: 'success', summary: 'Sprint created' });
      },
      error: () => {
        this.submitting = false;
        this.messageService.add({ severity: 'error', summary: 'Failed to create sprint' });
      }
    });
  }

  openIssueDialog() {
    this.issueForm = { projectId: this.projectId, type: 'TASK', summary: '', priority: 'MEDIUM' };
    this.showIssueDialog = true;
  }

  createIssue() {
    if (!this.issueForm.summary) return;
    this.submitting = true;
    this.issueService.create(this.projectId, this.issueForm).subscribe({
      next: issue => {
        this.issues.push(issue);
        this.showIssueDialog = false;
        this.submitting = false;
        this.messageService.add({ severity: 'success', summary: `${issue.issueKey} created` });
      },
      error: () => {
        this.submitting = false;
        this.messageService.add({ severity: 'error', summary: 'Failed to create issue' });
      }
    });
  }

  startSprint(sprint: Sprint) {
    this.sprintService.start(sprint.id).subscribe({
      next: s => {
        const idx = this.sprints.findIndex(x => x.id === s.id);
        if (idx >= 0) this.sprints[idx] = s;
        this.messageService.add({ severity: 'success', summary: 'Sprint started' });
      },
      error: (e) => {
        this.messageService.add({ severity: 'error', summary: e.error?.message || 'Failed to start sprint' });
      }
    });
  }

  completeSprint(sprint: Sprint) {
    this.sprintService.complete(sprint.id).subscribe({
      next: s => {
        const idx = this.sprints.findIndex(x => x.id === s.id);
        if (idx >= 0) this.sprints[idx] = s;
        this.messageService.add({ severity: 'success', summary: 'Sprint completed' });
      },
      error: () => {
        this.messageService.add({ severity: 'error', summary: 'Failed to complete sprint' });
      }
    });
  }

  openIssue(issue: Issue) {
    this.router.navigate(['/issues', issue.id]);
  }

  getSprintStatusSeverity(status: string): 'success' | 'info' | 'warn' | 'danger' | 'secondary' {
    return status === 'ACTIVE' ? 'success' : status === 'COMPLETED' ? 'secondary' : 'info';
  }

  getPriorityIcon(p: string) {
    const m: Record<string, string> = { HIGHEST: 'pi pi-angle-double-up', HIGH: 'pi pi-angle-up', MEDIUM: 'pi pi-minus', LOW: 'pi pi-angle-down', LOWEST: 'pi pi-angle-double-down' };
    return m[p] || 'pi pi-minus';
  }

  getTypeIcon(t: string) {
    const m: Record<string, string> = { EPIC: 'pi pi-bolt', STORY: 'pi pi-bookmark', TASK: 'pi pi-check-square', BUG: 'pi pi-bug', SUB_TASK: 'pi pi-sitemap' };
    return m[t] || 'pi pi-check-square';
  }
}
