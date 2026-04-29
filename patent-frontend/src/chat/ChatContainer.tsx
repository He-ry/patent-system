import React, { useEffect, useRef, useState } from 'react';
import { Sparkles, PanelLeftClose, PanelLeft, Plus } from 'lucide-react';
import { ChatProvider, useChatContext } from './ChatContext';
import { ChatSidebar } from './ChatSidebar';
import { ChatMessage } from './ChatMessage';
import { ChatInput } from './ChatInput';

interface ChatContainerProps {
  title?: string;
  subtitle?: string;
  welcomeMessage?: string;
  sidebarVisible?: boolean;
  onSidebarVisibleChange?: (visible: boolean) => void;
  onToast?: (type: 'success' | 'error' | 'info', message: string) => void;
}

function ChatContainerInner({
  title = '智能助手',
  subtitle = '我可以帮你解答专利问题、分析数据并生成报告。',
  welcomeMessage = '你好，我是专利智能助手',
  sidebarVisible = true,
  onSidebarVisibleChange,
  onToast
}: ChatContainerProps) {
  const { messages, searched, resetChat, progress, loading } = useChatContext();
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll when new messages arrive or during streaming
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const lastAiMessageIndex = messages.findLastIndex(message => message.role === 'assistant');

  return (
    <div className="h-full flex animate-in fade-in duration-500">
      <ChatSidebar visible={sidebarVisible} />

      <div className="flex-1 flex flex-col min-w-0 bg-white">
        <div className="h-14 border-b border-slate-100 flex items-center justify-between px-6 flex-shrink-0">
          <div className="flex items-center gap-3">
            {onSidebarVisibleChange && (
              <button
                onClick={() => onSidebarVisibleChange(!sidebarVisible)}
                className="p-2 hover:bg-slate-100 rounded-lg text-slate-500 hover:text-slate-700 transition-colors"
              >
                {sidebarVisible ? <PanelLeftClose className="w-4 h-4" /> : <PanelLeft className="w-4 h-4" />}
              </button>
            )}
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-blue-600" />
              <h1 className="text-lg font-bold text-slate-900">{title}</h1>
            </div>
          </div>

          {searched && (
            <button
              onClick={resetChat}
              title={loading ? '切换前会自动停止当前回复' : '开启新会话'}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-medium rounded-lg transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              新会话
            </button>
          )}
        </div>

        <div className="flex-1 flex flex-col overflow-hidden">
          {!searched ? (
            <div className="flex-1 flex flex-col justify-center items-center px-6">
              <div className="w-full max-w-5xl">
                <div className="text-center mb-8">
                  <h2 className="text-2xl font-bold text-slate-900 mb-2">{welcomeMessage}</h2>
                  <p className="text-slate-500">{subtitle}</p>
                </div>
                <ChatInput />
              </div>
            </div>
          ) : (
            <div className="flex-1 flex flex-col overflow-hidden">
              <div className="flex-1 overflow-y-auto bg-[#fbfcfd]">
                <div className="max-w-5xl mx-auto py-8 px-8 space-y-6">
                  {messages.map((message, index) => (
                    <ChatMessage
                      key={message.id}
                      message={message}
                      progress={index === lastAiMessageIndex && loading ? progress : null}
                    />
                  ))}
                  <div ref={messagesEndRef} />
                </div>
              </div>

              <ChatInput />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export function ChatContainer(props: ChatContainerProps) {
  return (
    <ChatProvider onToast={props.onToast}>
      <ChatContainerInner {...props} />
    </ChatProvider>
  );
}

export { ChatProvider, useChatContext } from './ChatContext';
export * from './types';
