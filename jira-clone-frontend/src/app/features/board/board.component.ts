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
import { MessageService } from 'primeng/api';
import { ProjectService } from '../../core/services/project.service';
import { IssueService } from '../../core/services/issue.service';
import { Issue, IssueDto, IssueType, Priority } from '../../core/models/issue.model';
import { Project } from '../../core/models/project.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    ButtonModule, TagModule, DialogModule,
    InputTextModule, TextareaModule, SelectModule,
    ToastModule, ProgressSpinnerModule
  ],
  providers: [MessageService],
  templateUrl: './board.component.html',
  styleUrl: './board.component.scss'
})
export class BoardComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private projectService = inject(ProjectService);
  private issueService = inject(IssueService);
  private messageService = inject(MessageService);

  project: Project | null = null;
  issues: Issue[] = [];
  loading = true;
  showCreateDialog = false;
  submitting = false;
  draggedIssue: Issue | null = null;

  form: IssueDto = {
    projectId: '',
    type: 'TASK',
    summary: '',
    priority: 'MEDIUM'
  };

  typeOptions = [
    { label: 'Epic', value: 'EPIC' },
    { label: 'Story', value: 'STORY' },
    { label: 'Task', value: 'TASK' },
    { label: 'Bug', value: 'BUG' },
    { label: 'Sub-task', value: 'SUB_TASK' }
  ];

  priorityOptions = [
    { label: 'Highest', value: 'HIGHEST' },
    { label: 'High', value: 'HIGH' },
    { label: 'Medium', value: 'MEDIUM' },
    { label: 'Low', value: 'LOW' },
    { label: 'Lowest', value: 'LOWEST' }
  ];

  get projectId() { return this.route.snapshot.paramMap.get('projectId')!; }

  get columns(): { status: string; issues: Issue[] }[] {
    if (!this.project) return [];
    return (this.project.statuses || []).map(status => ({
      status,
      issues: this.issues.filter(i => i.status === status)
    }));
  }

  ngOnInit() {
    forkJoin({
      project: this.projectService.getById(this.projectId),
      issues: this.issueService.getByProject(this.projectId)
    }).subscribe({
      next: ({ project, issues }) => {
        this.project = project;
        this.issues = issues;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  openCreateDialog() {
    this.form = { projectId: this.projectId, type: 'TASK', summary: '', priority: 'MEDIUM' };
    this.showCreateDialog = true;
  }

  createIssue() {
    if (!this.form.summary) return;
    this.submitting = true;
    this.issueService.create(this.projectId, this.form).subscribe({
      next: issue => {
        this.issues.push(issue);
        this.showCreateDialog = false;
        this.submitting = false;
        this.messageService.add({ severity: 'success', summary: `Issue ${issue.issueKey} created` });
      },
      error: () => {
        this.submitting = false;
        this.messageService.add({ severity: 'error', summary: 'Failed to create issue' });
      }
    });
  }

  onDragStart(issue: Issue) {
    this.draggedIssue = issue;
  }

  onDragEnd() {
    this.draggedIssue = null;
  }

  onDrop(status: string) {
    if (!this.draggedIssue || this.draggedIssue.status === status) return;
    const issue = this.draggedIssue;
    const oldStatus = issue.status;
    issue.status = status;
    this.issueService.updateStatus(issue.id, { status }).subscribe({
      error: () => {
        issue.status = oldStatus;
        this.messageService.add({ severity: 'error', summary: 'Failed to update status' });
      }
    });
    this.draggedIssue = null;
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  openIssue(issue: Issue) {
    this.router.navigate(['/issues', issue.id]);
  }

  getPriorityIcon(priority: string): string {
    const map: Record<string, string> = {
      HIGHEST: 'pi pi-angle-double-up',
      HIGH: 'pi pi-angle-up',
      MEDIUM: 'pi pi-minus',
      LOW: 'pi pi-angle-down',
      LOWEST: 'pi pi-angle-double-down'
    };
    return map[priority] || 'pi pi-minus';
  }

  getPriorityColor(priority: string): string {
    const map: Record<string, string> = {
      HIGHEST: '#e74c3c', HIGH: '#e67e22', MEDIUM: '#3498db', LOW: '#27ae60', LOWEST: '#95a5a6'
    };
    return map[priority] || '#95a5a6';
  }

  getTypeIcon(type: string): string {
    const map: Record<string, string> = {
      EPIC: 'pi pi-bolt', STORY: 'pi pi-bookmark', TASK: 'pi pi-check-square',
      BUG: 'pi pi-bug', SUB_TASK: 'pi pi-sitemap'
    };
    return map[type] || 'pi pi-check-square';
  }

  getTypeColor(type: string): string {
    const map: Record<string, string> = {
      EPIC: '#9b59b6', STORY: '#27ae60', TASK: '#3498db', BUG: '#e74c3c', SUB_TASK: '#95a5a6'
    };
    return map[type] || '#3498db';
  }
}
