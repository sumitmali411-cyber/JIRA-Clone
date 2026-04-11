import { Component, OnInit, OnDestroy, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { TableModule } from 'primeng/table';
import { SelectModule } from 'primeng/select';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { SearchService, SearchFilter } from '../../core/services/search.service';
import { ProjectService } from '../../core/services/project.service';
import { Issue, IssueType, Priority } from '../../core/models/issue.model';
import { Project } from '../../core/models/project.model';

interface ChipFilter {
  key: keyof SearchFilter;
  label: string;
  value: string;
  displayValue: string;
}

@Component({
  selector: 'app-search',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    InputTextModule, ButtonModule, TagModule,
    TableModule, SelectModule,
    ProgressSpinnerModule, ToastModule
  ],
  providers: [MessageService],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss'
})
export class SearchComponent implements OnInit, OnDestroy {
  private searchService = inject(SearchService);
  private projectService = inject(ProjectService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  query = '';
  results: Issue[] = [];
  loading = false;
  hasSearched = false;

  projects: Project[] = [];

  activeFilters: ChipFilter[] = [];

  filterProject = '';
  filterStatus = '';
  filterType = '';
  filterPriority = '';

  readonly typeOptions = [
    { label: 'All Types', value: '' },
    { label: 'Epic', value: 'EPIC' },
    { label: 'Story', value: 'STORY' },
    { label: 'Task', value: 'TASK' },
    { label: 'Bug', value: 'BUG' },
    { label: 'Sub-task', value: 'SUB_TASK' }
  ];

  readonly statusOptions = [
    { label: 'All Statuses', value: '' },
    { label: 'To Do', value: 'To Do' },
    { label: 'In Progress', value: 'In Progress' },
    { label: 'In Review', value: 'In Review' },
    { label: 'Done', value: 'Done' }
  ];

  readonly priorityOptions = [
    { label: 'All Priorities', value: '' },
    { label: 'Highest', value: 'HIGHEST' },
    { label: 'High', value: 'HIGH' },
    { label: 'Medium', value: 'MEDIUM' },
    { label: 'Low', value: 'LOW' },
    { label: 'Lowest', value: 'LOWEST' }
  ];

  private searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  ngOnInit(): void {
    this.projectService.getAll().subscribe({
      next: projects => {
        this.projects = projects;
        this.cdr.markForCheck();
      }
    });

    this.searchSub = this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(q => {
        this.loading = true;
        this.cdr.markForCheck();
        const filter = this.buildFilter(q);
        return this.searchService.search(filter);
      })
    ).subscribe({
      next: results => {
        this.results = results;
        this.loading = false;
        this.hasSearched = true;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.hasSearched = true;
        this.cdr.markForCheck();
      }
    });
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  onQueryChange(value: string): void {
    this.query = value;
    if (value.trim() || this.hasActiveFilters()) {
      this.searchSubject.next(value);
    }
  }

  applyFilters(): void {
    this.buildChips();
    if (this.query.trim() || this.hasActiveFilters()) {
      this.searchSubject.next(this.query);
    }
  }

  removeFilter(chip: ChipFilter): void {
    switch (chip.key) {
      case 'projectId': this.filterProject = ''; break;
      case 'status': this.filterStatus = ''; break;
      case 'type': this.filterType = ''; break;
      case 'priority': this.filterPriority = ''; break;
    }
    this.buildChips();
    this.searchSubject.next(this.query);
  }

  clearAll(): void {
    this.query = '';
    this.filterProject = '';
    this.filterStatus = '';
    this.filterType = '';
    this.filterPriority = '';
    this.activeFilters = [];
    this.results = [];
    this.hasSearched = false;
    this.cdr.markForCheck();
  }

  openIssue(issue: Issue): void {
    this.router.navigate(['/issues', issue.id]);
  }

  getProjectName(id: string): string {
    return this.projects.find(p => p.id === id)?.name || id;
  }

  get projectOptions(): { label: string; value: string }[] {
    return [
      { label: 'All Projects', value: '' },
      ...this.projects.map(p => ({ label: p.name, value: p.id }))
    ];
  }

  private buildFilter(q: string): SearchFilter {
    return {
      q: q || undefined,
      projectId: this.filterProject || undefined,
      status: this.filterStatus || undefined,
      type: this.filterType || undefined,
      priority: this.filterPriority || undefined
    };
  }

  private hasActiveFilters(): boolean {
    return !!(this.filterProject || this.filterStatus || this.filterType || this.filterPriority);
  }

  private buildChips(): void {
    const chips: ChipFilter[] = [];
    if (this.filterProject) {
      chips.push({ key: 'projectId', label: 'Project', value: this.filterProject, displayValue: this.getProjectName(this.filterProject) });
    }
    if (this.filterStatus) {
      chips.push({ key: 'status', label: 'Status', value: this.filterStatus, displayValue: this.filterStatus });
    }
    if (this.filterType) {
      chips.push({ key: 'type', label: 'Type', value: this.filterType, displayValue: this.filterType });
    }
    if (this.filterPriority) {
      chips.push({ key: 'priority', label: 'Priority', value: this.filterPriority, displayValue: this.filterPriority });
    }
    this.activeFilters = chips;
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
}
