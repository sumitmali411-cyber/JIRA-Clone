import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { ProjectService } from '../../core/services/project.service';
import { Project, ProjectDto, ProjectType } from '../../core/models/project.model';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    ButtonModule, CardModule, TagModule, DialogModule,
    InputTextModule, TextareaModule, SelectModule, ToastModule
  ],
  providers: [MessageService],
  templateUrl: './projects.component.html',
  styleUrl: './projects.component.scss'
})
export class ProjectsComponent implements OnInit {
  private projectService = inject(ProjectService);
  private router = inject(Router);
  private messageService = inject(MessageService);

  projects: Project[] = [];
  loading = true;
  showDialog = false;
  submitting = false;

  form: ProjectDto = {
    name: '',
    key: '',
    description: '',
    type: 'SCRUM'
  };

  typeOptions = [
    { label: 'Scrum', value: 'SCRUM' },
    { label: 'Kanban', value: 'KANBAN' }
  ];

  ngOnInit() {
    this.loadProjects();
  }

  loadProjects() {
    this.loading = true;
    this.projectService.getAll().subscribe({
      next: p => { this.projects = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openDialog() {
    this.form = { name: '', key: '', description: '', type: 'SCRUM' };
    this.showDialog = true;
  }

  keyError = '';

  onNameChange() {
    if (this.form.name && !this.form.key) {
      this.form.key = this.form.name
        .toUpperCase()
        .replace(/[^A-Z]/g, '')
        .substring(0, 6);
    }
  }

  onKeyChange() {
    const key = this.form.key.toUpperCase().replace(/[^A-Z]/g, '');
    this.form.key = key;
    if (key.length < 2) {
      this.keyError = 'Key must be at least 2 letters';
    } else if (key.length > 10) {
      this.keyError = 'Key must be 10 letters or fewer';
    } else {
      this.keyError = '';
    }
  }

  get keyInvalid(): boolean {
    return !/^[A-Z]{2,10}$/.test(this.form.key);
  }

  submit() {
    if (!this.form.name || this.keyInvalid) return;
    this.submitting = true;
    this.projectService.create(this.form).subscribe({
      next: p => {
        this.projects.push(p);
        this.showDialog = false;
        this.submitting = false;
        this.messageService.add({ severity: 'success', summary: 'Project created' });
      },
      error: (e) => {
        this.submitting = false;
        this.messageService.add({ severity: 'error', summary: e.error?.message || 'Failed to create project' });
      }
    });
  }

  navigate(project: Project) {
    this.router.navigate(['/projects', project.id, 'board']);
  }

  getTypeColor(type: string): 'info' | 'success' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    return type === 'SCRUM' ? 'info' : 'success';
  }
}
