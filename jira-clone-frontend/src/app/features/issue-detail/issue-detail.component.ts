import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { DividerModule } from 'primeng/divider';
import { MessageService } from 'primeng/api';
import { IssueService } from '../../core/services/issue.service';
import { CommentService } from '../../core/services/comment.service';
import { Issue, Priority, IssueType, GitCommit } from '../../core/models/issue.model';
import { Comment } from '../../core/models/comment.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-issue-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    ButtonModule, TagModule, SelectModule,
    InputTextModule, TextareaModule, ToastModule,
    ProgressSpinnerModule, DividerModule
  ],
  providers: [MessageService],
  templateUrl: './issue-detail.component.html',
  styleUrl: './issue-detail.component.scss'
})
export class IssueDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private issueService = inject(IssueService);
  private commentService = inject(CommentService);
  private messageService = inject(MessageService);

  issue: Issue | null = null;
  comments: Comment[] = [];
  loading = true;
  saving = false;
  editingSummary = false;
  editingDescription = false;
  newComment = '';
  addingComment = false;
  currentUser = 'me';

  statusOptions: { label: string; value: string }[] = [];
  priorityOptions = [
    { label: 'Highest', value: 'HIGHEST' }, { label: 'High', value: 'HIGH' },
    { label: 'Medium', value: 'MEDIUM' }, { label: 'Low', value: 'LOW' }, { label: 'Lowest', value: 'LOWEST' }
  ];
  typeOptions = [
    { label: 'Epic', value: 'EPIC' }, { label: 'Story', value: 'STORY' },
    { label: 'Task', value: 'TASK' }, { label: 'Bug', value: 'BUG' }
  ];

  summaryEdit = '';
  descriptionEdit = '';

  get issueId() { return this.route.snapshot.paramMap.get('issueId')!; }

  ngOnInit() {
    forkJoin({
      issue: this.issueService.getById(this.issueId),
      comments: this.commentService.getByIssue(this.issueId)
    }).subscribe({
      next: ({ issue, comments }) => {
        this.issue = issue;
        this.comments = comments;
        this.loading = false;
        // Build status options from project (simplified)
        this.statusOptions = ['To Do', 'In Progress', 'In Review', 'Done'].map(s => ({ label: s, value: s }));
      },
      error: () => { this.loading = false; }
    });
  }

  goBack() { this.router.navigate(['/projects', this.issue?.projectId, 'board']); }

  updateStatus(status: string) {
    if (!this.issue) return;
    this.issueService.updateStatus(this.issue.id, { status }).subscribe({
      next: updated => {
        this.issue = updated;
        this.messageService.add({ severity: 'success', summary: 'Status updated', life: 2000 });
      },
      error: () => this.messageService.add({ severity: 'error', summary: 'Failed' })
    });
  }

  updatePriority(priority: string) {
    if (!this.issue) return;
    this.issueService.update(this.issue.id, { priority: priority as Priority }).subscribe({
      next: u => this.issue = u
    });
  }

  updateAssignee(assignee: string) {
    if (!this.issue) return;
    this.issueService.update(this.issue.id, { assignee }).subscribe({
      next: u => this.issue = u
    });
  }

  startEditSummary() {
    this.summaryEdit = this.issue?.summary || '';
    this.editingSummary = true;
  }

  saveSummary() {
    if (!this.issue || !this.summaryEdit.trim()) return;
    this.issueService.update(this.issue.id, { summary: this.summaryEdit }).subscribe({
      next: u => { this.issue = u; this.editingSummary = false; }
    });
  }

  startEditDescription() {
    this.descriptionEdit = this.issue?.description || '';
    this.editingDescription = true;
  }

  saveDescription() {
    if (!this.issue) return;
    this.issueService.update(this.issue.id, { description: this.descriptionEdit }).subscribe({
      next: u => { this.issue = u; this.editingDescription = false; }
    });
  }

  addComment() {
    if (!this.newComment.trim() || !this.issue) return;
    this.addingComment = true;
    this.commentService.create(this.issue.id, this.newComment, this.currentUser).subscribe({
      next: c => {
        this.comments.push(c);
        this.newComment = '';
        this.addingComment = false;
      },
      error: () => { this.addingComment = false; }
    });
  }

  deleteComment(commentId: string) {
    this.commentService.delete(commentId).subscribe({
      next: () => { this.comments = this.comments.filter(c => c.id !== commentId); }
    });
  }

  getPriorityIcon(p: string) {
    const m: Record<string, string> = { HIGHEST: 'pi pi-angle-double-up', HIGH: 'pi pi-angle-up', MEDIUM: 'pi pi-minus', LOW: 'pi pi-angle-down', LOWEST: 'pi pi-angle-double-down' };
    return m[p] || 'pi pi-minus';
  }

  getPriorityColor(p: string) {
    const m: Record<string, string> = { HIGHEST: '#e74c3c', HIGH: '#e67e22', MEDIUM: '#3498db', LOW: '#27ae60', LOWEST: '#95a5a6' };
    return m[p] || '#95a5a6';
  }

  getTypeColor(t: string) {
    const m: Record<string, string> = { EPIC: '#9b59b6', STORY: '#27ae60', TASK: '#3498db', BUG: '#e74c3c', SUB_TASK: '#95a5a6' };
    return m[t] || '#3498db';
  }

  getBranchName(branch: string): string {
    return branch.replace('refs/heads/', '');
  }
}
