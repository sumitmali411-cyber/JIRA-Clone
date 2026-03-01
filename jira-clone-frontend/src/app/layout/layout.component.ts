import { Component, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { RippleModule } from 'primeng/ripple';
import { TooltipModule } from 'primeng/tooltip';
import { ProjectService } from '../core/services/project.service';
import { Project } from '../core/models/project.model';
import { KeycloakService } from '../core/services/keycloak.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, CommonModule,
    ButtonModule, AvatarModule, RippleModule, TooltipModule
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent implements OnInit {
  projects: Project[] = [];
  currentProjectId: string | null = null;
  sidebarCollapsed = false;

  constructor(
    private projectService: ProjectService,
    private router: Router,
    private route: ActivatedRoute,
    public keycloakService: KeycloakService
  ) {}

  ngOnInit() {
    this.loadProjects();
  }

  loadProjects() {
    this.projectService.getAll().subscribe(p => this.projects = p);
  }

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  navigateToProject(id: string) {
    this.currentProjectId = id;
    this.router.navigate(['/projects', id, 'board']);
  }

  logout() {
    this.keycloakService.logout();
  }
}
