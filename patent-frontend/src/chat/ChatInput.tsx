import React, { useState } from 'react';
import { Send, Loader2 } from 'lucide-react';
import { useChatContext } from './ChatContext';

interface ChatInputProps {
  placeholder?: string;
  disabled?: boolean;
}

export function ChatInput({ placeholder = "输入问题...", disabled = false }: ChatInputProps) {
  const { sendMessage, loading } = useChatContext();
  const [input, setInput] = useState('');

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!input.trim() || loading || disabled) return;
    sendMessage(input);
    setInput('');
  };

  return (
    <div className="p-4 bg-white flex-shrink-0">
      <div className="max-w-5xl mx-auto">
        <form onSubmit={handleSubmit} className="flex items-center gap-3">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={placeholder}
            disabled={disabled || loading}
            className="flex-1 px-4 py-2 bg-white border border-slate-300 hover:border-blue-400 rounded-lg text-sm text-slate-700 shadow-sm transition-all focus:outline-none focus:ring-2 focus:ring-blue-500/20 disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={!input.trim() || loading || disabled}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg text-sm font-medium flex items-center gap-2 transition-colors"
          >
            {loading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <>
                <Send className="w-4 h-4" />
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
