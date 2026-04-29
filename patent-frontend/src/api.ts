export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

export interface PatentInfo {
  id: string;
  serialNumber: string;
  title: string;
  applicationDate: string;
  inventors: string;
  college: string;
  legalStatus: string;
  patentType: string;
  applicationNumber: string;
  grantDate: string;
  technicalFields: string;
  technicalProblem: string;
  technicalEffect: string;
  currentAssignee: string;
  originalAssignee: string;
  inventorCount: string;
  agency: string;
  currentAssigneeProvince: string;
  originalAssigneeProvince: string;
  originalAssigneeType: string;
  currentAssigneeType: string;
  applicationYear: string;
  publicationDate: string;
  grantYear: string;
  ipcClassifications: string;
  cpcClassifications: string;
  ipcMainClass: string;
  ipcMainClassInterpretation: string;
  strategicIndustryClassification: string;
  applicationFieldClassification: string;
  technicalSubjectClassification: string;
  expiryDate: string;
  simpleFamilyCitedPatents: string;
  citedPatents: string;
  citedIn5Years: string;
  claimsCount: string;
  patentValue: string;
  technicalValue: string;
  marketValue: string;
  transferEffectiveDate: string;
  licenseType: string;
  licenseCount: string;
  licenseEffectiveDate: string;
  transferor: string;
  transferee: string;
  createTime?: string;
  updateTime?: string;
}

export interface PageResponse<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export const PATENT_COLUMNS: { key: keyof PatentInfo; label: string }[] = [
  { key: 'serialNumber', label: '序号' },
  { key: 'title', label: '标题' },
  { key: 'applicationDate', label: '申请日' },
  { key: 'inventors', label: '发明人' },
  { key: 'college', label: '学院' },
  { key: 'legalStatus', label: '法律状态/事件' },
  { key: 'patentType', label: '专利类型' },
  { key: 'applicationNumber', label: '申请号' },
  { key: 'grantDate', label: '授权日' },
  { key: 'technicalFields', label: '技术领域' },
  { key: 'technicalProblem', label: '[标]技术问题短语' },
  { key: 'technicalEffect', label: '[标]技术功效短语' },
  { key: 'currentAssignee', label: '[标]当前申请(专利权)人' },
  { key: 'originalAssignee', label: '[标]原始申请(专利权)人' },
  { key: 'inventorCount', label: '发明人数量' },
  { key: 'agency', label: '[标]代理机构' },
  { key: 'currentAssigneeProvince', label: '当前申请(专利权)人州/省' },
  { key: 'originalAssigneeProvince', label: '原始申请(专利权)人州/省' },
  { key: 'originalAssigneeType', label: '[标]原始申请(专利权)人类型' },
  { key: 'currentAssigneeType', label: '[标]当前申请(专利权)人类型' },
  { key: 'applicationYear', label: '申请年' },
  { key: 'publicationDate', label: '公开(公告)日' },
  { key: 'grantYear', label: '授权年' },
  { key: 'ipcClassifications', label: 'IPC分类号' },
  { key: 'cpcClassifications', label: 'CPC分类号' },
  { key: 'ipcMainClass', label: 'IPC主分类号' },
  { key: 'ipcMainClassInterpretation', label: 'IPC主分类号(部)释义' },
  { key: 'strategicIndustryClassification', label: '战略新兴产业分类' },
  { key: 'applicationFieldClassification', label: '应用领域分类' },
  { key: 'technicalSubjectClassification', label: '技术主题分类' },
  { key: 'expiryDate', label: '失效日' },
  { key: 'citedPatents', label: '被引用专利数量' },
  { key: 'citedIn5Years', label: '5年内被引用数量' },
  { key: 'claimsCount', label: '权利要求数' },
  { key: 'patentValue', label: '专利价值' },
  { key: 'technicalValue', label: '技术价值' },
  { key: 'marketValue', label: '市场价值' },
  { key: 'transferEffectiveDate', label: '权利转移生效日' },
  { key: 'licenseType', label: '许可类型' },
  { key: 'licenseCount', label: '许可次数' },
  { key: 'licenseEffectiveDate', label: '许可生效日' },
  { key: 'transferor', label: '转让人' },
  { key: 'transferee', label: '受让人' },
];

export interface PatentQueryRequest {
  page?: number;
  size?: number;
  keyword?: string;
  patentType?: string;
  legalStatus?: string;
  applicationYear?: string;
  college?: string;
  ipcMainClassInterpretation?: string;
  inventors?: string;
  applicationFieldClassification?: string;
  technicalSubjectClassification?: string;
}

export interface WordCloudRequest {
  dimension?: 'inventors' | 'technicalFields' | 'technicalProblem' | 'technicalEffect' | 'ipcClassifications' | 'cpcClassifications' | 'technicalSubjectClassification' | 'applicationFieldClassification' | 'ipcMainClassInterpretation' | 'strategicIndustryClassification';
  limit?: number;
  patentType?: string;
  legalStatus?: string;
  applicationYear?: string;
}

export const WORD_CLOUD_COLUMNS: { key: WordCloudRequest['dimension']; label: string }[] = [
  { key: 'inventors', label: '发明人' },
  { key: 'technicalFields', label: '技术领域' },
  { key: 'technicalProblem', label: '技术问题短语' },
  { key: 'technicalEffect', label: '技术功效短语' },
  { key: 'ipcClassifications', label: 'IPC分类号' },
  { key: 'cpcClassifications', label: 'CPC分类号' },
  { key: 'technicalSubjectClassification', label: '技术主题分类' },
  { key: 'applicationFieldClassification', label: '应用领域分类' },
  { key: 'ipcMainClassInterpretation', label: 'IPC主分类号(部)释义' },
  { key: 'strategicIndustryClassification', label: '战略新兴产业分类' },
];

export interface WordCloudData {
  word: string;
  count: number;
}

export interface PatentAddRequest {
  id?: string;
  title: string;
  serialNumber?: string;
  applicationNumber?: string;
  applicationDate?: string;
  patentType?: string;
  legalStatus?: string;
  college?: string;
  currentAssignee?: string;
  originalAssignee?: string;
  applicationYear?: string;
  publicationDate?: string;
  grantDate?: string;
  grantYear?: string;
  ipcMainClass?: string;
  ipcMainClassInterpretation?: string;
  inventors?: string;
  technicalFields?: string;
  ipcClassifications?: string;
  cpcClassifications?: string;
  technicalProblem?: string;
  technicalEffect?: string;
  inventorCount?: string;
  agency?: string;
  currentAssigneeProvince?: string;
  originalAssigneeProvince?: string;
  originalAssigneeType?: string;
  currentAssigneeType?: string;
  strategicIndustryClassification?: string;
  applicationFieldClassification?: string;
  technicalSubjectClassification?: string;
  expiryDate?: string;
  simpleFamilyCitedPatents?: string;
  citedPatents?: string;
  citedIn5Years?: string;
  claimsCount?: string;
  patentValue?: string;
  technicalValue?: string;
  marketValue?: string;
  transferEffectiveDate?: string;
  licenseType?: string;
  licenseCount?: string;
  licenseEffectiveDate?: string;
  transferor?: string;
  transferee?: string;
}

export interface StatisticsRequest {
  field: string;
  limit?: number;
}

export interface StatisticsItem {
  name: string;
  value: number;
}

export const STATISTICS_COLUMNS: { key: StatisticsRequest['field']; label: string }[] = [
  { key: 'college', label: '学院' },
  { key: 'ipcMainClassInterpretation', label: 'IPC主分类号(部)释义' },
  { key: 'inventors', label: '发明人' },
  { key: 'applicationFieldClassification', label: '应用领域分类' },
  { key: 'technicalSubjectClassification', label: '技术主题分类' },
];

export interface ImportResult {
  total: number;
  success: number;
  skipped: number;
  duplicates: string[];
}

export interface TrendRequest {
  dimension?: 'ipcMainClassInterpretation' | 'inventors' | 'applicationFieldClassification' | 'technicalSubjectClassification';
  limit?: number;
  patentType?: string;
  legalStatus?: string;
  startYear?: string;
  endYear?: string;
}

export const TREND_COLUMNS: { key: TrendRequest['dimension']; label: string }[] = [
  { key: 'ipcMainClassInterpretation', label: 'IPC主分类号(部)释义' },
  { key: 'inventors', label: '发明人' },
  { key: 'applicationFieldClassification', label: '应用领域分类' },
  { key: 'technicalSubjectClassification', label: '技术主题分类' },
];

export interface TrendData {
  year: string;
  name: string;
  count: number;
}

async function request<T>(
  url: string,
  options?: RequestInit
): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
    ...options,
  });

  const data = await response.json();
  return data;
}

export const patentApi = {
  getList: async (params: PatentQueryRequest): Promise<ApiResponse<PageResponse<PatentInfo>>> => {
    return request<PageResponse<PatentInfo>>('/api/patent/list', {
      method: 'POST',
      body: JSON.stringify(params),
    });
  },

  getDetail: async (id: string): Promise<ApiResponse<PatentInfo>> => {
    return request<PatentInfo>(`/api/patent/detail/${id}`, {
      method: 'GET',
    });
  },

  add: async (data: PatentAddRequest): Promise<ApiResponse<void>> => {
    return request<void>('/api/patent/add', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  update: async (data: PatentAddRequest): Promise<ApiResponse<void>> => {
    return request<void>('/api/patent/update', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  delete: async (id: string): Promise<ApiResponse<void>> => {
    return request<void>(`/api/patent/delete/${id}`, {
      method: 'DELETE',
    });
  },

  import: async (file: File): Promise<ApiResponse<ImportResult>> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${API_BASE_URL}/api/patent/import`, {
      method: 'POST',
      body: formData,
    });

    return response.json();
  },

  statistics: async (params: StatisticsRequest): Promise<ApiResponse<StatisticsItem[]>> => {
    return request('/api/patent/statistics', {
      method: 'POST',
      body: JSON.stringify(params),
    });
  },

  export: async (params: PatentQueryRequest): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/api/patent/export`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(params),
    });

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = '专利数据导出.xlsx';
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  getWordCloud: async (params: WordCloudRequest): Promise<ApiResponse<WordCloudData[]>> => {
    return request<WordCloudData[]>('/api/patent/wordcloud', {
      method: 'POST',
      body: JSON.stringify(params),
    });
  },

  getTrend: async (params: TrendRequest): Promise<ApiResponse<TrendData[]>> => {
    return request<TrendData[]>('/api/patent/trend', {
      method: 'POST',
      body: JSON.stringify(params),
    });
  },
};

export const ALL_PATENT_COLUMNS: { key: keyof PatentInfo; label: string; category?: string; inputType?: 'date' | 'number' }[] = [
  { key: 'serialNumber', label: '序号', category: '基本信息', inputType: 'number' },
  { key: 'title', label: '标题', category: '基本信息' },
  { key: 'applicationDate', label: '申请日', category: '基本信息', inputType: 'date' },
  { key: 'grantDate', label: '授权日', category: '基本信息', inputType: 'date' },
  { key: 'applicationYear', label: '申请年', category: '基本信息', inputType: 'number' },
  { key: 'publicationDate', label: '公开(公告)日', category: '基本信息', inputType: 'date' },
  { key: 'grantYear', label: '授权年', category: '基本信息', inputType: 'number' },
  { key: 'expiryDate', label: '失效日', category: '基本信息', inputType: 'date' },
  
  { key: 'patentType', label: '专利类型', category: '分类信息' },
  { key: 'legalStatus', label: '法律状态/事件', category: '分类信息' },
  { key: 'technicalFields', label: '技术领域', category: '分类信息' },
  { key: 'ipcMainClass', label: 'IPC主分类号', category: '分类信息' },
  { key: 'ipcMainClassInterpretation', label: 'IPC主分类号(部)释义', category: '分类信息' },
  { key: 'ipcClassifications', label: 'IPC分类号', category: '分类信息' },
  { key: 'cpcClassifications', label: 'CPC分类号', category: '分类信息' },
  { key: 'strategicIndustryClassification', label: '战略新兴产业分类', category: '分类信息' },
  { key: 'applicationFieldClassification', label: '应用领域分类', category: '分类信息' },
  { key: 'technicalSubjectClassification', label: '技术主题分类', category: '分类信息' },
  
  { key: 'college', label: '学院', category: '权利人信息' },
  { key: 'inventors', label: '发明人', category: '权利人信息' },
  { key: 'inventorCount', label: '发明人数量', category: '权利人信息', inputType: 'number' },
  { key: 'currentAssignee', label: '[标]当前申请(专利权)人', category: '权利人信息' },
  { key: 'originalAssignee', label: '[标]原始申请(专利权)人', category: '权利人信息' },
  { key: 'currentAssigneeProvince', label: '当前申请(专利权)人州/省', category: '权利人信息' },
  { key: 'originalAssigneeProvince', label: '原始申请(专利权)人州/省', category: '权利人信息' },
  { key: 'currentAssigneeType', label: '[标]当前申请(专利权)人类型', category: '权利人信息' },
  { key: 'originalAssigneeType', label: '[标]原始申请(专利权)人类型', category: '权利人信息' },
  
  { key: 'technicalProblem', label: '[标]技术问题短语', category: '技术信息' },
  { key: 'technicalEffect', label: '[标]技术功效短语', category: '技术信息' },
  
  { key: 'agency', label: '[标]代理机构', category: '流程信息' },
  { key: 'claimsCount', label: '权利要求数', category: '流程信息', inputType: 'number' },
  { key: 'simpleFamilyCitedPatents', label: '简单同族被引用专利总数', category: '流程信息', inputType: 'number' },
  { key: 'citedPatents', label: '被引用专利数量', category: '流程信息', inputType: 'number' },
  { key: 'citedIn5Years', label: '5年内被引用数量', category: '流程信息', inputType: 'number' },
  
  { key: 'patentValue', label: '专利价值', category: '价值评估' },
  { key: 'technicalValue', label: '技术价值', category: '价值评估' },
  { key: 'marketValue', label: '市场价值', category: '价值评估' },
  
  { key: 'transferor', label: '转让人', category: '转让许可' },
  { key: 'transferee', label: '受让人', category: '转让许可' },
  { key: 'transferEffectiveDate', label: '权利转移生效日', category: '转让许可', inputType: 'date' },
  { key: 'licenseType', label: '许可类型', category: '转让许可' },
  { key: 'licenseCount', label: '许可次数', category: '转让许可', inputType: 'number' },
  { key: 'licenseEffectiveDate', label: '许可生效日', category: '转让许可', inputType: 'date' },
];

export const DEFAULT_VISIBLE_COLUMNS: (keyof PatentInfo)[] = ALL_PATENT_COLUMNS.map(c => c.key);

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

export interface ConversationDetail {
  id: string;
  title: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  messages: MessageVO[];
}

export interface IndexResult {
  indexed: number;
  message: string;
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

export interface ProgressData {
  status: string;
  progress?: number;
  skill?: SkillInfo;
  report?: ReportInfo;
}

export const chatApi = {
  sendMessage: (
    request: ChatRequest,
    callbacks: {
      onStart?: (conversationId: string) => void;
      onContent?: (text: string) => void;
      onReferences?: (references: ReferenceGroup[]) => void;
      onStatus?: (data: ProgressData) => void;
      onSkill?: (skill: SkillInfo) => void;
      onReport?: (report: ReportInfo) => void;
      onReportPreview?: (reportPreview: ReportPreview) => void;
      onDone?: (conversationId: string, messageId: string) => void;
      onError?: (error: string) => void;
    }
  ): (() => void) => {
    const controller = new AbortController();

    fetch(`${API_BASE_URL}/api/chat/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(request),
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const reader = response.body?.getReader();
        if (!reader) {
          throw new Error('No reader available');
        }

        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                const json: ChatEventVO = JSON.parse(line.substring(5).trim());
                switch (json.type) {
                  case 'start':
                    if (json.conversationId) {
                      callbacks.onStart?.(json.conversationId);
                    }
                    break;
                  case 'content':
                    if (json.text) {
                      callbacks.onContent?.(json.text);
                    }
                    break;
                  case 'references':
                    if (json.references) {
                      callbacks.onReferences?.(json.references);
                    }
                    break;
                  case 'status':
                    callbacks.onStatus?.({
                      status: json.status || '',
                      progress: json.progress,
                    });
                    break;
                  case 'skill':
                    const skillInfo: SkillInfo = json.skill || {
                      name: json.skillName || '',
                      status: json.skillStatus || '',
                    };
                    callbacks.onSkill?.(skillInfo);
                    callbacks.onStatus?.({
                      status: skillInfo.status,
                      skill: skillInfo,
                    });
                    break;
                  case 'report':
                    if (json.report) {
                      callbacks.onReport?.(json.report);
                      callbacks.onStatus?.({
                        status: '报告生成完成！',
                        progress: 100,
                        report: json.report,
                      });
                    }
                    break;
                  case 'report_preview':
                    if (json.reportPreview) {
                      callbacks.onReportPreview?.(json.reportPreview);
                      callbacks.onStatus?.({
                        status: '报告预览生成完成',
                        progress: 100,
                      });
                    }
                    break;
                  case 'done':
                    callbacks.onDone?.(json.conversationId || '', json.messageId || '');
                    break;
                  case 'error':
                    callbacks.onError?.(json.error || 'Unknown error');
                    break;
                }
              } catch (e) {
                console.error('Failed to parse SSE data:', line, e);
              }
            }
          }
        }
      })
      .catch((error) => {
        if (error.name !== 'AbortError') {
          callbacks.onError?.(error.message || 'Network error');
        }
      });

    return () => {
      controller.abort();
    };
  },

  getConversations: async (): Promise<ConversationVO[]> => {
    const response = await fetch(`${API_BASE_URL}/api/chat/conversations`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return response.json();
  },

  getConversation: async (id: string): Promise<ConversationDetail> => {
    const response = await fetch(`${API_BASE_URL}/api/chat/conversation/${id}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return response.json();
  },

  deleteConversation: async (id: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/api/chat/conversation/${id}`, {
      method: 'DELETE',
    });
    if (!response.ok) {
      throw new Error(`删除会话失败: ${response.status}`);
    }
  },

  indexPatents: async (): Promise<IndexResult> => {
    const response = await fetch(`${API_BASE_URL}/api/chat/index/patents`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return response.json();
  },

  getSuggestions: async (conversationId: string): Promise<string[]> => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/chat/suggestions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ conversationId }),
      });
      const data = await response.json();
      return data.suggestions || [];
    } catch {
      return [];
    }
  },
};

export interface ReportVO {
  id: string;
  title: string;
  reportType: string;
  downloadUrl: string;
  summary: string;
  sectionCount: number;
  createdAt: string;
}

export const reportApi = {
  getConversationReports: async (conversationId: string): Promise<ReportVO[]> => {
    const response = await fetch(`${API_BASE_URL}/api/reports/conversation/${conversationId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return response.json();
  },

  getReport: async (reportId: string): Promise<ReportVO> => {
    const response = await fetch(`${API_BASE_URL}/api/reports/${reportId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    return response.json();
  },

  downloadReport: (reportId: string): void => {
    window.open(`${API_BASE_URL}/api/reports/${reportId}/download/html`, '_blank');
  },

  deleteReport: async (reportId: string): Promise<void> => {
    await fetch(`${API_BASE_URL}/api/reports/${reportId}`, {
      method: 'DELETE',
    });
  },
};
