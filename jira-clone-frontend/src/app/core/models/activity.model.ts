export interface ActivityLog {
  id: string;
  issueId: string;
  projectId: string;
  actor: string;
  field: string;
  oldValue: string;
  newValue: string;
  action: string;
  timestamp: string;
}
