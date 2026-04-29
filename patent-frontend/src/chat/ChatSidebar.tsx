import React, { useEffect, useState } from 'react';
import { Plus, MessageSquare, History, Trash2, Loader2 } from 'lucide-react';
import { useChatContext } from './ChatContext';
import { ConversationVO } from './types';

interface ChatSidebarProps {
  visible?: boolean;
}

export function ChatSidebar({ visible = true }: ChatSidebarProps) {
  const {
    conversations,
    conversationId,
    loading,
    loadConversations,
    loadConversationMessages,
    deleteConv,
    resetChat
  } = useChatContext();

  const [sidebarLoading, setSidebarLoading] = useState(false);
  const [deleteConfirmConversation, setDeleteConfirmConversation] = useState<ConversationVO | null>(null);

  useEffect(() => {
    if (!visible) return;

    const load = async () => {
      setSidebarLoading(true);
      try {
        await loadConversations();
      } finally {
        setSidebarLoading(false);
      }
    };

    load();
  }, [visible, loadConversations]);

  const handleNewSearch = () => {
    resetChat();
  };

  const handleDeleteConversation = (conversation: ConversationVO, event: React.MouseEvent) => {
    event.stopPropagation();
    setDeleteConfirmConversation(conversation);
  };

  const confirmDeleteConversation = async () => {
    if (!deleteConfirmConversation) return;
    await deleteConv(deleteConfirmConversation.id);
    setDeleteConfirmConversation(null);
  };

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    if (isNaN(date.getTime())) return '';

    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    const yesterday = new Date(now);
    yesterday.setDate(yesterday.getDate() - 1);
    const isYesterday = date.toDateString() === yesterday.toDateString();

    if (isToday) {
      return '今天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    }
    if (isYesterday) {
      return '昨天 ' + date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    }
    return date.toLocaleDateString('zh-CN', {
      month: 'numeric',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (!visible) return null;

  return (
    <>
      <div className="w-64 bg-white border border-slate-200 rounded-lg flex flex-col flex-shrink-0 overflow-hidden">
        <div className="p-3 border-b border-slate-100">
          <button
            onClick={handleNewSearch}
            title={loading ? '点击后会先停止当前回复，再开启新会话' : '开启新会话'}
            className="w-full flex items-center justify-center gap-2 px-3 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium transition-colors"
          >
            <Plus className="w-4 h-4" />
            开启新会话
          </button>
        </div>

        <div className="flex-1 overflow-y-auto chat-sidebar-scrollbar">
          <div className="text-xs font-medium text-slate-400 px-3 py-3 flex items-center gap-1.5">
            <History className="w-3.5 h-3.5" />
            历史会话
          </div>

          {sidebarLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="w-5 h-5 text-blue-500 animate-spin" />
            </div>
          ) : conversations.length === 0 ? (
            <div className="text-center py-8 text-slate-400 text-xs">暂无历史会话</div>
          ) : (
            <div className="space-y-1 px-2">
              {conversations.map(conv => (
                <div
                  key={conv.id}
                  onClick={() => loadConversationMessages(conv.id)}
                  className={`group flex items-center gap-2 p-2.5 rounded-lg cursor-pointer transition-colors ${
                    conversationId === conv.id
                      ? 'bg-blue-50 border border-blue-200'
                      : 'hover:bg-slate-50 border border-transparent'
                  }`}
                >
                  <MessageSquare className="w-4 h-4 text-slate-400 flex-shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="text-sm font-medium text-slate-700 truncate">
                      {conv.title || '新会话'}
                    </div>
                    <div className="text-[10px] text-slate-400">{formatDate(conv.createdAt)}</div>
                  </div>
                  <button
                    onClick={(event) => handleDeleteConversation(conv, event)}
                    className="opacity-0 group-hover:opacity-100 p-1 hover:bg-slate-100 text-slate-400 hover:text-red-500 rounded transition-all"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {deleteConfirmConversation && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            onClick={() => setDeleteConfirmConversation(null)}
          />
          <div className="relative bg-white rounded-2xl shadow-2xl w-full max-w-sm mx-4 overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center">
                  <Trash2 className="w-5 h-5 text-red-600" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-slate-900">删除会话</h3>
                  <p className="text-sm text-slate-500">确定要删除这个会话吗？</p>
                </div>
              </div>
              <div className="flex gap-3 justify-end">
                <button
                  onClick={() => setDeleteConfirmConversation(null)}
                  className="px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 rounded-lg transition-colors"
                >
                  取消
                </button>
                <button
                  onClick={confirmDeleteConversation}
                  className="px-4 py-2 text-sm font-medium bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
                >
                  删除
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
