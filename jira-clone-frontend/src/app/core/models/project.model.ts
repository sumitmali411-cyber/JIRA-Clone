export type ProjectType = 'SCRUM' | 'KANBAN';

export interface Project {
  id: string;
  name: string;
  key: string;
  description: string;
  type: ProjectType;
  statuses: string[];
  createdAt: string;
  updatedAt: string;
  issueCounter: number;
}

export interface ProjectDto {
  name: string;
  key: string;
  description?: string;
  type: ProjectType;
  statuses?: string[];
}
