export type IssueType = 'EPIC' | 'STORY' | 'TASK' | 'BUG' | 'SUB_TASK';
export type Priority = 'HIGHEST' | 'HIGH' | 'MEDIUM' | 'LOW' | 'LOWEST';
export type LinkType = 'BLOCKS' | 'IS_BLOCKED_BY' | 'RELATES_TO' | 'DUPLICATES' | 'IS_DUPLICATED_BY';

export interface IssueLink {
  id: string;
  sourceIssueId: string;
  targetIssueId: string;
  targetIssueKey: string;
  targetIssueSummary: string;
  linkType: LinkType;
}

export interface GitCommit {
  sha: string;
  shortSha: string;
  message: string;
  url: string;
  authorName: string;
  authorEmail: string;
  timestamp: string;
  branch: string;
  repoFullName: string;
}

export interface Issue {
  id: string;
  projectId: string;
  issueKey: string;
  issueNumber: number;
  type: IssueType;
  status: string;
  priority: Priority;
  summary: string;
  description: string;
  assignee: string;
  reporter: string;
  sprintId: string;
  epicId: string;
  parentId: string;
  labels: string[];
  storyPoints: number;
  timeEstimate: string;
  dueDate: string;
  createdAt: string;
  updatedAt: string;
  links: IssueLink[];
  gitCommits: GitCommit[];
}

export interface IssueDto {
  projectId: string;
  type: IssueType;
  status?: string;
  priority?: Priority;
  summary: string;
  description?: string;
  assignee?: string;
  reporter?: string;
  sprintId?: string;
  epicId?: string;
  parentId?: string;
  labels?: string[];
  storyPoints?: number;
  timeEstimate?: string;
  dueDate?: string;
}

export interface IssueLinkDto {
  targetIssueId: string;
  linkType: LinkType;
}

export interface StatusUpdateDto {
  status: string;
}
