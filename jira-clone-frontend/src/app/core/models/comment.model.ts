export interface Comment {
  id: string;
  issueId: string;
  author: string;
  body: string;
  createdAt: string;
  updatedAt: string;
  edited: boolean;
}

export interface CommentDto {
  author: string;
  body: string;
}
