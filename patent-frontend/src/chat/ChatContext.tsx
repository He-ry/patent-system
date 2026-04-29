import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
import { Message, ReferenceGroup, ConversationVO, ProgressState, SkillInfo, ReportInfo, ReportPreview } from './types';
import { chatApi, MessageVO, ReportVO, reportApi, ProgressData } from '../api';

interface ChatContextValue {
  messages: Message[];
  conversationId: string | null;
  conversations: ConversationVO[];
  reports: ReportVO[];
  progress: ProgressState | null;
  loading: boolean;
  searched: boolean;
  loadConversations: () => Promise<void>;
  loadConversationMessages: (convId: string) => Promise<void>;
  loadReports: () => Promise<void>;
  sendMessage: (content: string) => void;
  deleteConv: (convId: string) => Promise<void>;
  deleteReport: (reportId: string) => void;
  resetChat: () => void;
  setMessages: React.Dispatch<React.SetStateAction<Message[]>>;
}

const ChatContext = createContext<ChatContextValue | null>(null);

export function useChatContext() {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChatContext must be used within ChatProvider');
  }
  return context;
}

interface ChatProviderProps {
  children: React.ReactNode;
  onToast?: (type: 'success' | 'error' | 'info', message: string) => void;
}

const REPORT_PREVIEW_MARKER = '<!--REPORT_PREVIEW:';
const REPORT_META_MARKER = '<!--REPORT_META:';
const REPORT_REQUIREMENT_PROMPT_MARKER = '<!--REPORT_REQUIREMENT_CLARIFY-->';

function appendProgressStage(prev: ProgressState | null, status: string, progress?: number): ProgressState {
  return {
    status,
    progress,
    skill: prev?.skill,
    report: prev?.report,
  };
}

function parseStoredMessageMeta(content: string): { content: string; reportPreview?: ReportPreview; reportId?: string; reportPrompt?: boolean } {
  let nextContent = content;
  let reportPrompt = false;

  if (nextContent.includes(REPORT_REQUIREMENT_PROMPT_MARKER)) {
    reportPrompt = true;
    nextContent = nextContent.replace(REPORT_REQUIREMENT_PROMPT_MARKER, '').trim();
  }

  const metaMarkerIndex = nextContent.indexOf(REPORT_META_MARKER);
  if (metaMarkerIndex >= 0) {
    const endIndex = nextContent.indexOf('-->', metaMarkerIndex);
    if (endIndex >= 0) {
      const encoded = nextContent.slice(metaMarkerIndex + REPORT_META_MARKER.length, endIndex).trim();
      try {
        const binary = window.atob(encoded);
        const bytes = Uint8Array.from(binary, char => char.charCodeAt(0));
        const payload = JSON.parse(new TextDecoder().decode(bytes)) as { reportPreview?: ReportPreview; reportId?: string };
        return {
          content: nextContent.slice(0, metaMarkerIndex).trim(),
          reportPreview: payload.reportPreview,
          reportId: payload.reportId,
          reportPrompt,
        };
      } catch (error) {
        console.error('Failed to parse stored report meta:', error);
        return { content: nextContent.slice(0, metaMarkerIndex).trim() || nextContent, reportPrompt };
      }
    }
  }

  const markerIndex = nextContent.indexOf(REPORT_PREVIEW_MARKER);
  if (markerIndex < 0) {
    return { content: nextContent, reportPrompt };
  }

  const endIndex = nextContent.indexOf('-->', markerIndex);
  if (endIndex < 0) {
    return { content: nextContent, reportPrompt };
  }

  const encoded = nextContent.slice(markerIndex + REPORT_PREVIEW_MARKER.length, endIndex).trim();
  try {
    const binary = window.atob(encoded);
    const bytes = Uint8Array.from(binary, char => char.charCodeAt(0));
    const reportPreview = JSON.parse(new TextDecoder().decode(bytes)) as ReportPreview;
    return {
      content: nextContent.slice(0, markerIndex).trim(),
      reportPreview,
      reportPrompt,
    };
  } catch (error) {
    console.error('Failed to parse stored report preview:', error);
    return { content: nextContent.slice(0, markerIndex).trim() || nextContent, reportPrompt };
  }
}

function convertMessageVOToMessage(msg: MessageVO): Message {
  const references: ReferenceGroup[] = [];
  const storedPreview = parseStoredMessageMeta(msg.content || '');
  
  if (msg.references && msg.references.length > 0) {
    const groupedByDoc = new Map<string, ReferenceGroup>();
    
    for (const ref of msg.references) {
      if (!groupedByDoc.has(ref.docId)) {
        groupedByDoc.set(ref.docId, {
          docId: ref.docId,
          docTitle: ref.docTitle,
          items: [],
          count: 0,
        });
      }
      const group = groupedByDoc.get(ref.docId)!;
      group.items.push({
        id: ref.id,
        content: ref.content,
        score: ref.relevanceScore,
      });
      group.count++;
    }
    
    references.push(...groupedByDoc.values());
  }

  return {
    id: msg.id,
    role: msg.role,
    content: storedPreview.content,
    references: references.length > 0 ? references : undefined,
    reportPreview: storedPreview.reportPreview,
    reportId: storedPreview.reportId,
    reportPrompt: storedPreview.reportPrompt,
    isTyping: false,
    expanded: false,
  };
}

export function ChatProvider({ children, onToast }: ChatProviderProps) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [conversations, setConversations] = useState<ConversationVO[]>([]);
  const [reports, setReports] = useState<ReportVO[]>([]);
  const [progress, setProgress] = useState<ProgressState | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const abortRef = useRef<(() => void) | null>(null);

  const interruptStreamingReply = useCallback(() => {
    if (abortRef.current) {
      abortRef.current();
      abortRef.current = null;
    }
    setLoading(false);
    setProgress(null);
  }, []);

  const loadConversations = useCallback(async () => {
    try {
      const result = await chatApi.getConversations();
      setConversations(result || []);
    } catch (error) {
      console.error('Failed to load conversations:', error);
      setConversations([]);
    }
  }, []);

  const loadConversationMessages = useCallback(async (convId: string) => {
    interruptStreamingReply();
    try {
      const detail = await chatApi.getConversation(convId);
      const convertedMessages = detail.messages.map(convertMessageVOToMessage);
      setMessages(convertedMessages);
      setConversationId(convId);
      setSearched(true);
      
      const reportsResult = await reportApi.getConversationReports(convId);
      setReports(reportsResult || []);
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  }, [interruptStreamingReply]);

  const loadReports = useCallback(async () => {
    if (!conversationId) return;
    try {
      const result = await reportApi.getConversationReports(conversationId);
      setReports(result || []);
    } catch (error) {
      console.error('Failed to load reports:', error);
    }
  }, [conversationId]);

  const sendMessage = useCallback((content: string) => {
    if (!content.trim()) return;

    const userContent = content.trim();

    // If already processing, abort old request and start a new conversation
    if (loading) {
      if (abortRef.current) {
        abortRef.current();
        abortRef.current = null;
      }
      setConversationId(null);
      setProgress(null);
    }

    setLoading(true);
    setSearched(true);
    setProgress({
      status: '\u6b63\u5728\u5904\u7406...'
    });

    const userMsgId = Date.now().toString();
    setMessages(prev => [...prev, {
      id: userMsgId,
      role: 'user',
      content: userContent
    }]);

    const aiMsgId = (Date.now() + 1).toString();
    setMessages(prev => [...prev, {
      id: aiMsgId,
      role: 'assistant',
      content: '',
      isTyping: true,
      expanded: false
    }]);

    const currentConvId = conversationId;
    
    const abort = chatApi.sendMessage({
      conversationId: currentConvId || undefined,
      content: userContent
    }, {
      onStart: (convId) => {
        if (!currentConvId) {
          setConversationId(convId);
        }
      },
      onContent: (text) => {
        setMessages(prev => prev.map(msg => {
          if (msg.id !== aiMsgId) return msg;
          const parsed = parseStoredMessageMeta((msg.content || '') + text);
          return {
            ...msg,
            content: parsed.content,
            reportPrompt: parsed.reportPrompt,
            reportPreview: parsed.reportPreview ?? msg.reportPreview,
            reportId: parsed.reportId ?? msg.reportId,
            isTyping: false,
          };
        }));
      },
      onReferences: (refs) => {
        setMessages(prev => prev.map(msg =>
          msg.id === aiMsgId
            ? { ...msg, references: refs }
            : msg
        ));
      },
      onStatus: (data: ProgressData) => {
        setProgress(prev => {
          const next = appendProgressStage(prev, data.status, data.progress);
          return {
            ...next,
            skill: data.skill ?? prev?.skill,
            report: data.report ?? prev?.report,
          };
        });
      },
      onSkill: (skill: SkillInfo) => {
        setProgress(prev => {
          const status = skill.status || '技能执行中...';
          const next = appendProgressStage(prev, status, prev?.progress);
          return { ...next, skill };
        });
      },
      onReport: (report: ReportInfo) => {
        setProgress(prev => {
          const next = appendProgressStage(prev, '报告生成完成', 100);
          return { ...next, report };
        });
        setMessages(prev => prev.map(msg =>
          msg.id === aiMsgId
            ? { ...msg, reportId: report.reportId }
            : msg
        ));
      },
      onReportPreview: (reportPreview: ReportPreview) => {
        setProgress(prev => appendProgressStage(prev, '报告预览渲染完成', 100));
        setMessages(prev => prev.map(msg =>
          msg.id === aiMsgId
            ? {
                ...msg,
                content: 'HTML 报告预览已生成。本预览会保留在当前会话记录中。',
                reportPreview,
                isTyping: false,
              }
            : msg
        ));
      },
      onDone: async (convId) => {
        if (!currentConvId && convId) {
          setConversationId(convId);
        }
        setLoading(false);
        setProgress(null);
        loadConversations();

        // 获取"猜你想问"建议
        if (convId) {
          chatApi.getSuggestions(convId).then(suggestions => {
            if (suggestions.length > 0) {
              setMessages(prev => prev.map(msg =>
                msg.id === aiMsgId ? { ...msg, suggestions } : msg
              ));
            }
          });
        }

        if (convId) {
          try {
            const newReports = await reportApi.getConversationReports(convId);
            setReports(newReports || []);
          } catch (error) {
            console.error('Failed to load reports:', error);
          }
        }
      },
      onError: (error) => {
        setProgress(null);
        setMessages(prev => prev.map(msg =>
          msg.id === aiMsgId
            ? { ...msg, content: `错误: ${error}`, isTyping: false }
            : msg
        ));
        setLoading(false);
      }
    });

    abortRef.current = abort;
  }, [conversationId, loading, loadConversations]);

  const resetChat = useCallback(() => {
    interruptStreamingReply();
    setMessages([]);
    setConversationId(null);
    setSearched(false);
    setReports([]);
    setProgress(null);
  }, [interruptStreamingReply]);

  const deleteConv = useCallback(async (convId: string) => {
    try {
      if (loading && conversationId === convId) {
        interruptStreamingReply();
      }
      await chatApi.deleteConversation(convId);
      setConversations(prev => prev.filter(c => c.id !== convId));
      if (conversationId === convId) {
        resetChat();
      }
      onToast?.('success', '对话已删除');
    } catch (error) {
      console.error('Failed to delete conversation:', error);
      onToast?.('error', '删除失败，请重试');
    }
  }, [conversationId, interruptStreamingReply, loading, onToast, resetChat]);

  const deleteReport = useCallback((reportId: string) => {
    setReports(prev => prev.filter(r => r.id !== reportId));
  }, []);

  return (
    <ChatContext.Provider
      value={{
        messages,
        conversationId,
        conversations,
        reports,
        progress,
        loading,
        searched,
        loadConversations,
        loadConversationMessages,
        loadReports,
        sendMessage,
        deleteConv,
        deleteReport,
        resetChat,
        setMessages
      }}
    >
      {children}
    </ChatContext.Provider>
  );
}
