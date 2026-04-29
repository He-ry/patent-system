import React from 'react';
import { Zap, CheckCircle2 } from 'lucide-react';
import { ProgressState } from './types';

interface ProgressIndicatorProps {
  progress: ProgressState;
}

export function ProgressIndicator({ progress }: ProgressIndicatorProps) {
  const { skill, progress: pct } = progress;
  const isComplete = pct === 100;
  const hasSkill = !!skill;

  if (isComplete) {
    return (
      <div className="flex items-center gap-2 px-4 py-3 bg-emerald-50 rounded-lg border border-emerald-100">
        <CheckCircle2 className="w-5 h-5 text-emerald-500" />
        <span className="text-sm font-medium text-emerald-700">完成</span>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-3 px-4 py-3 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-xl border border-blue-100 animate-in slide-in-from-bottom-2 duration-300">
      <div className="flex-shrink-0">
        {hasSkill ? (
          <Zap className="w-5 h-5 text-blue-500" />
        ) : (
          <div className="w-3 h-3 bg-blue-400 rounded-full animate-pulse" />
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          {hasSkill && (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 bg-blue-50 text-blue-600 rounded text-xs font-medium border border-blue-100">
              <Zap className="w-3 h-3" />
              {skill.name}
            </span>
          )}
          <span className="text-sm text-slate-500">
            思考中
            <span className="inline-flex gap-0.5 ml-1">
              <span className="w-1 h-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
              <span className="w-1 h-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
              <span className="w-1 h-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
            </span>
          </span>
        </div>
      </div>
    </div>
  );
}

export function ProgressIndicatorInline({ progress }: ProgressIndicatorProps) {
  const { skill, progress: pct } = progress;
  const isComplete = pct === 100;

  return (
    <div className="flex items-center gap-2 text-sm text-slate-500">
      {isComplete ? (
        <CheckCircle2 className="w-4 h-4 text-emerald-500" />
      ) : skill ? (
        <>
          <span className="inline-flex items-center gap-1 px-1.5 py-0.5 bg-blue-50 text-blue-600 rounded text-xs font-medium">
            <Zap className="w-3 h-3" />
            {skill.name}
          </span>
          <span className="text-slate-400">·</span>
        </>
      ) : (
        <div className="w-2 h-2 bg-blue-400 rounded-full animate-pulse" />
      )}
      <span>思考中</span>
      <span className="inline-flex gap-0.5">
        <span className="w-1 h-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
        <span className="w-1 h-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
        <span className="w-1 h-1 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
      </span>
    </div>
  );
}
