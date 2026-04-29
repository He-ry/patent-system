import React, { useState } from 'react';
import { Download, FileText, File, Maximize2, Minimize2 } from 'lucide-react';
import { ReportPreview } from './types';
import { API_BASE_URL } from '../api';

interface ReportPreviewCardProps {
  preview: ReportPreview;
  reportId: string;
}

export function ReportPreviewCard({ preview, reportId }: ReportPreviewCardProps) {
  const [isFullscreen, setIsFullscreen] = useState(false);

  const downloadFile = async (format: 'html' | 'docx') => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/reports/${reportId}/download/${format}`);
      if (!response.ok) {
        throw new Error(`Download failed with status ${response.status}`);
      }
      const blob = await response.blob();
      const objectUrl = window.URL.createObjectURL(blob);
      const disposition = response.headers.get('content-disposition') || '';
      const filenameMatch = disposition.match(/filename\*?=(?:UTF-8''|")?([^\";]+)/i);
      const decodedFilename = filenameMatch ? decodeURIComponent(filenameMatch[1].replace(/"/g, '')) : `${preview.title}.${format}`;

      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = decodedFilename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(objectUrl);
    } catch (error) {
      console.error('Failed to download report:', error);
      window.open(`${API_BASE_URL}/api/reports/${reportId}/download/${format}`, '_blank', 'noopener,noreferrer');
    }
  };

  const formatDateTime = (dateStr: string) => {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      const y = d.getFullYear();
      const m = d.getMonth() + 1;
      const day = d.getDate();
      const h = d.getHours().toString().padStart(2, '0');
      const min = d.getMinutes().toString().padStart(2, '0');
      return `${y}-${m}-${day} ${h}:${min}`;
    } catch {
      return dateStr;
    }
  };

  const previewUrl = `${API_BASE_URL}/api/reports/${reportId}/view/html`;
  const totalChapters = preview.chapters.length;
  const totalCharts = preview.chapters.filter(c => c.chartOption).length;

  return (
    <div className="mt-4 border border-slate-200 bg-white rounded-xl overflow-hidden shadow-sm">
      <div className="border-b border-slate-200 bg-white px-5 py-4">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <div className="text-base font-bold text-slate-900 truncate">{preview.title}</div>
            <div className="mt-1 flex items-center gap-3 text-xs text-slate-400">
              <span>{formatDateTime(preview.generatedAt)}</span>
              <span className="w-1 h-1 rounded-full bg-slate-300" />
              <span>{totalChapters} 个章节</span>
              {totalCharts > 0 && (
                <>
                  <span className="w-1 h-1 rounded-full bg-slate-300" />
                  <span>{totalCharts} 个图表</span>
                </>
              )}
            </div>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={() => downloadFile('html')}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 rounded-lg transition-colors"
              title="下载 HTML 版本"
            >
              <FileText className="w-3.5 h-3.5" />
              HTML
            </button>
            <button
              onClick={() => downloadFile('docx')}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-blue-50 text-blue-700 hover:bg-blue-100 rounded-lg transition-colors"
              title="下载 Word 版本"
            >
              <File className="w-3.5 h-3.5" />
              Word
            </button>
            <button
              onClick={() => setIsFullscreen(true)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-slate-100 text-slate-600 hover:bg-slate-200 rounded-lg transition-colors"
              title="全屏查看"
            >
              <Maximize2 className="w-3.5 h-3.5" />
              全屏
            </button>
          </div>
        </div>
      </div>

      <div className={`${isFullscreen ? 'fixed inset-0 z-50 bg-white' : 'relative'}`}>
        {isFullscreen && (
          <div className="absolute top-4 right-4 z-10">
            <button
              onClick={() => setIsFullscreen(false)}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-white border border-slate-200 text-slate-700 hover:bg-slate-50 rounded-lg shadow-sm transition-colors"
            >
              <Minimize2 className="w-3.5 h-3.5" />
              退出全屏
            </button>
          </div>
        )}
        <div className={`${isFullscreen ? 'h-screen' : 'h-[600px]'} border border-slate-100 rounded-lg overflow-hidden`}>
          <iframe
            src={previewUrl}
            style={{
              width: '100%',
              height: '100%',
              border: 'none',
            }}
            title="报告预览"
          />
        </div>
      </div>
    </div>
  );
}
