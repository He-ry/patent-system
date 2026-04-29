import React, { useState } from 'react';
import { Copy, Check, MessageSquare, ChevronRight, CheckCircle2, Loader2, AlertCircle } from 'lucide-react';
import { cn } from '../lib/utils';
import { useChatContext } from './ChatContext';
import { Message, ReferenceGroup, ProgressState } from './types';
import { ReportPreviewCard } from './ReportPreviewCard';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface ChatMessageProps {
  message: Message;
  progress?: ProgressState | null;
}

function ReportPromptCard() {
  const { sendMessage, loading } = useChatContext();

  const scopeOptions = [
    '直接生成全库综合报告',
    '分析 2024-2025 年专利',
    '按学院维度分析',
  ];
  const focusOptions = [
    '重点看 IPC/CPC 技术布局',
    '重点看发明人团队与贡献',
    '重点看专利价值质量',
    '重点看成果转化与许可',
  ];

  const handleQuickSend = (text: string) => {
    if (!loading) {
      sendMessage(text);
    }
  };

  return (
    <div className="mt-4 max-w-3xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 bg-slate-50 px-4 py-3">
        <div className="text-sm font-semibold text-slate-900">报告需求采集</div>
        <div className="mt-1 text-xs leading-5 text-slate-500">先选一个范围，或者直接跳过，后面还可以继续补充你的重点。</div>
      </div>

      <div className="space-y-5 px-4 py-4">
        <section>
          <div className="mb-2 text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">范围</div>
          <div className="flex flex-wrap gap-2">
            {scopeOptions.map(option => (
              <button
                key={option}
                type="button"
                onClick={() => handleQuickSend(option)}
                disabled={loading}
                className="inline-flex h-9 items-center border border-slate-200 bg-white px-3 text-sm text-slate-700 hover:border-blue-300 hover:text-blue-700 disabled:opacity-50"
              >
                {option}
              </button>
            ))}
          </div>
        </section>

        <section>
          <div className="mb-2 text-xs font-semibold uppercase tracking-[0.14em] text-slate-400">重点</div>
          <div className="flex flex-wrap gap-2">
            {focusOptions.map(option => (
              <button
                key={option}
                type="button"
                onClick={() => handleQuickSend(option)}
                disabled={loading}
                className="inline-flex h-9 items-center border border-slate-200 bg-white px-3 text-sm text-slate-700 hover:border-blue-300 hover:text-blue-700 disabled:opacity-50"
              >
                {option}
              </button>
            ))}
          </div>
        </section>

        <div className="flex flex-wrap gap-2 border-t border-slate-200 pt-4">
          <button
            type="button"
            onClick={() => handleQuickSend('直接生成')}
            disabled={loading}
            className="inline-flex h-10 items-center bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            直接生成
          </button>
          <button
            type="button"
            onClick={() => handleQuickSend('先按全库分析，后续我再继续补充重点')}
            disabled={loading}
            className="inline-flex h-10 items-center border border-slate-200 bg-white px-4 text-sm text-slate-700 hover:border-blue-300 hover:text-blue-700 disabled:opacity-50"
          >
            先生成后补充
          </button>
        </div>
      </div>
    </div>
  );
}

function SkillProgressBar({ progress }: { progress: ProgressState }) {
  const { status } = progress;
  const isError = status?.includes('失败') || status?.includes('错误');
  const isComplete = status?.includes('完成');

  return (
    <div className="mt-2 flex items-center gap-2 text-sm">
      {isComplete ? (
        <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />
      ) : isError ? (
        <AlertCircle className="w-4 h-4 text-red-500 shrink-0" />
      ) : (
        <Loader2 className="w-4 h-4 text-blue-500 animate-spin shrink-0" />
      )}
      <span className={isError ? "text-red-600" : "text-slate-500"}>
        {status || '处理中...'}
      </span>
    </div>
  );
}

export function ChatMessage({ message, progress }: ChatMessageProps) {
  const { sendMessage } = useChatContext();
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(false);

  const copyToClipboard = async (text: string, msgId: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopiedId(msgId);
      setTimeout(() => setCopiedId(null), 2000);
    } catch (error) {
      console.error('Failed to copy:', error);
    }
  };

  const getReferenceCount = (references?: ReferenceGroup[]) => {
    if (!references || !Array.isArray(references)) return 0;
    return references.reduce((sum, g) => sum + (g.count || g.items?.length || 0), 0);
  };

  if (message.role === 'user') {
    return (
      <div className="flex justify-end mb-6">
        <div className="bg-blue-600 text-white px-4 py-2.5 rounded-2xl max-w-[75%] ml-12">
          <span className="text-[15px] leading-relaxed whitespace-pre-wrap">{message.content}</span>
        </div>
      </div>
    );
  }

  if (message.isTyping && !message.content) {
    return (
      <div className="mb-6 mr-12">
        <div className="flex items-start gap-3">
          <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
            <MessageSquare className="w-4 h-4 text-blue-600" />
          </div>
          <div className="flex-1 min-w-0">
            {progress ? (
              <SkillProgressBar progress={progress} />
            ) : (
              <div className="flex items-center gap-2 text-sm text-slate-500">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>正在处理...</span>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="mb-6 mr-12">
      <div className="flex items-start gap-3">
        <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
          <MessageSquare className="w-4 h-4 text-blue-600" />
        </div>
        <div className="flex-1 min-w-0">
          {!message.reportPrompt && message.content && (
            <div className="text-[15px] text-slate-700 leading-loose prose prose-slate prose-sm max-w-none">
              <ReactMarkdown remarkPlugins={[remarkGfm]}>
                {message.content}
              </ReactMarkdown>
            </div>
          )}

          {!message.reportPrompt && !message.content && !progress && (
            <div className="text-[15px] text-slate-500 italic">思考中...</div>
          )}

          {message.reportPrompt && (
            <div className="text-[15px] leading-7 text-slate-700">
              我先帮你确认一下报告方向，你可以点选，也可以继续像聊天一样直接输入你的要求。
            </div>
          )}

          {message.reportPrompt && <ReportPromptCard />}

          {/* 实时进度反馈：技能执行和报告生成期间始终显示 */}
          {progress && (
            <SkillProgressBar progress={progress} />
          )}

          {message.references && Array.isArray(message.references) && message.references.length > 0 && (
            <div className="mt-4 pt-4 border-t border-slate-200">
              <button
                onClick={() => setExpanded(!expanded)}
                className="group flex items-center gap-2 text-sm text-slate-600 hover:text-blue-600 transition-colors"
              >
                <div className="w-1 h-4 bg-blue-600 rounded-full" />
                <span>参考内容 ({getReferenceCount(message.references)})</span>
                <ChevronRight className={cn(
                  "w-4 h-4 transition-transform",
                  expanded && "rotate-90"
                )} />
              </button>

              {expanded && (
                <div className="mt-3 pl-3 border-l-2 border-slate-200 space-y-4 animate-in fade-in slide-in-from-top-2 duration-200">
                  {message.references.map((group, gIdx) => (
                    <div key={gIdx}>
                      <div className="text-sm font-medium text-slate-800 mb-2">
                        {group.docTitle}
                        <span className="ml-2 text-xs text-slate-400">
                          ({group.count || group.items?.length || 0}条)
                        </span>
                      </div>
                      <div className="space-y-2">
                        {group.items?.map((item, iIdx) => (
                          <div
                            key={iIdx}
                            className="text-sm text-slate-600 bg-white rounded-lg p-3 border border-slate-100 hover:border-blue-200 transition-colors"
                          >
                            <div className="leading-relaxed text-slate-700">{item.content}</div>
                            <div className="mt-1 text-xs text-slate-400">相关度: {(item.score * 100).toFixed(0)}%</div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {message.reportPreview && (
            <ReportPreviewCard preview={message.reportPreview} reportId={message.reportId || message.id} />
          )}

          {message.suggestions && message.suggestions.length > 0 && (
            <div className="mt-4 pt-4 border-t border-slate-100">
              <div className="text-xs text-slate-400 mb-2">猜你想问</div>
              <div className="flex flex-wrap gap-2">
                {message.suggestions.map((suggestion, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => sendMessage(suggestion)}
                    className="inline-flex items-center px-3 py-1.5 text-xs bg-white text-slate-600 border border-slate-200 rounded-full hover:border-blue-300 hover:text-blue-600 hover:bg-blue-50 transition-colors"
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="flex items-center gap-2 mt-4">
            <button
              onClick={() => copyToClipboard(message.content, message.id)}
              className="flex items-center gap-1.5 px-2.5 py-1.5 text-xs text-slate-500 hover:text-slate-700 hover:bg-white rounded-lg transition-colors"
            >
              {copiedId === message.id ? (
                <>
                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                  <span className="text-emerald-600">已复制</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>复制</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
