import React from 'react';

export interface ChatRequest {
  conversationId?: string;
  content: string;
}

export interface RefItem {
  id: string;
  content: string;
  score: number;
}

export interface ReferenceGroup {
  docId: string;
  docTitle: string;
  items: RefItem[];
  count: number;
}

export interface SkillInfo {
  name: string;
  status: string;
}

export interface ReportInfo {
  reportId: string;
  title: string;
  downloadUrl: string;
}

export interface ReportInfoVO {
  id: string;
  title: string;
  reportType: string;
  sectionCount: number;
  chartCount: number;
  totalWords: number;
  createdAt: string;
}

export interface ReportPreviewChapter {
  title: string;
  objective: string;
  analysisMarkdown: string;
  chartTitle: string;
  chartType: string;
  chartOption: Record<string, any>;
  data: Record<string, any>[];
  keyFindings: string[];
}

export interface ReportPreview {
  title: string;
  generatedAt: string;
  executiveSummary: string;
  conclusionSummary?: string;
  keyFindings: string[];
  chapters: ReportPreviewChapter[];
}

export interface ChatEventVO {
  type: 'start' | 'content' | 'references' | 'status' | 'skill' | 'report' | 'report_preview' | 'done' | 'error';
  conversationId?: string;
  messageId?: string;
  text?: string;
  references?: ReferenceGroup[];
  status?: string;
  progress?: number;
  skill?: SkillInfo;
  skillName?: string;
  skillStatus?: string;
  report?: ReportInfo;
  reportPreview?: ReportPreview;
  error?: string;
}

export interface ProgressState {
  status: string;
  progress?: number;
  skill?: SkillInfo;
  report?: ReportInfo;
}

export interface ConversationVO {
  id: string;
  userId?: string;
  title: string;
  status: string;
  patentIds?: string;
  summary?: string;
  uploadedFiles?: string;
  createdAt: string;
  updatedAt: string;
  messageCount?: number;
}

export interface MessageReference {
  id: string;
  messageId: string;
  docId: string;
  docTitle: string;
  content: string;
  relevanceScore: number;
}

export interface MessageVO {
  id: string;
  conversationId: string;
  role: 'user' | 'assistant';
  content: string;
  messageOrder: number;
  likes: number;
  dislikes: number;
  briefSummary?: string;
  createdAt: string;
  references?: MessageReference[];
}

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  references?: ReferenceGroup[];
  reportPreview?: ReportPreview;
  reportId?: string;
  reportPrompt?: boolean;
  suggestions?: string[];
  isTyping?: boolean;
  expanded?: boolean;
}

export interface ChatConfig {
  apiBaseUrl: string;
  onNavigateToPatent?: (patentId: string) => void;
  renderCustomHeader?: () => React.ReactNode;
  customStyles?: {
    sidebarWidth?: string;
    primaryColor?: string;
  };
}
