import React from 'react';
import { Search, Eye, Edit2, Trash2, Database, Loader2, List, X } from 'lucide-react';

interface PatentDataManagementProps {
  data: any[];
  columns: string[];
  allColumns: { key: string; label: string }[];
  defaultVisibleColumns: string[];
  searchTerm: string;
  setSearchTerm: (term: string) => void;
  searchField: string;
  setSearchField: (field: string) => void;
  searchFieldOptions: { value: string; label: string }[];
  filteredData: any[];
  paginatedData: any[];
  currentPage: number;
  setCurrentPage: React.Dispatch<React.SetStateAction<number>>;
  totalPages: number;
  itemsPerPage: number;
  setItemsPerPage: React.Dispatch<React.SetStateAction<number>>;
  setViewingRecord: (record: any) => void;
  setIsViewModalOpen: (isOpen: boolean) => void;
  openEditModal: (record?: any) => void;
  handleDelete: (record: any) => void;
  onImportClick: () => void;
  loading?: boolean;
  total?: number;
}

export const PatentDataManagement: React.FC<PatentDataManagementProps> = ({
  data,
  columns,
  allColumns,
  defaultVisibleColumns,
  searchTerm,
  setSearchTerm,
  searchField,
  setSearchField,
  searchFieldOptions,
  filteredData,
  paginatedData,
  currentPage,
  setCurrentPage,
  totalPages,
  itemsPerPage,
  setItemsPerPage,
  setViewingRecord,
  setIsViewModalOpen,
  openEditModal,
  handleDelete,
  onImportClick,
  loading = false,
  total = 0,
}) => {
  const [visibleColumns, setVisibleColumns] = React.useState<string[]>(defaultVisibleColumns);
  const [isColumnSelectorOpen, setIsColumnSelectorOpen] = React.useState(false);

  const renderCellValue = (col: string, value: any) => {
    if (col === 'patentType') {
      return (
        <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
          value === '发明专利' ? 'bg-blue-50 text-blue-700' :
          value === '实用新型' ? 'bg-purple-50 text-purple-700' :
          value === '外观设计' ? 'bg-orange-50 text-orange-700' :
          'bg-slate-100 text-slate-600'
        }`}>{value || '-'}</span>
      );
    }

    if (col === 'legalStatus') {
      return (
        <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
          value === '已授权' || value === '授权' ? 'bg-green-50 text-green-700' :
          value === '实质审查' ? 'bg-yellow-50 text-yellow-700' :
          value === '公开' ? 'bg-blue-50 text-blue-700' :
          value === '驳回' ? 'bg-red-50 text-red-700' :
          value === '撤回' ? 'bg-gray-50 text-gray-700' :
          'bg-slate-100 text-slate-600'
        }`}>{value || '-'}</span>
      );
    }
    
    if (col === 'applicationDate' || col === 'grantDate' || col === 'publicationDate' || col === 'expiryDate' || col === 'transferEffectiveDate' || col === 'licenseEffectiveDate') {
      if (!value) return '-';
      const str = String(value);
      return str.substring(0, 10);
    }
    
    if (value === undefined || value === null || String(value).trim() === '') {
      return <span className="text-slate-300">-</span>;
    }
    
    return String(value);
  };

  const toggleColumn = (colKey: string) => {
    if (visibleColumns.includes(colKey)) {
      if (visibleColumns.length > 1) {
        setVisibleColumns(visibleColumns.filter(k => k !== colKey));
      }
    } else {
      setVisibleColumns([...visibleColumns, colKey]);
    }
  };

  const resetColumns = () => {
    setVisibleColumns(defaultVisibleColumns);
  };

  return (
    <div className="bg-white border border-slate-200 rounded-2xl shadow-sm flex flex-col h-full overflow-hidden">
      <div className="px-6 py-4 border-b border-slate-200 flex justify-between items-center bg-slate-50/50 shrink-0">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3 bg-white p-2 rounded-xl border border-slate-200 shadow-sm">
            <label className="text-sm font-medium text-slate-700 pl-2">搜索字段:</label>
            <select
              value={searchField}
              onChange={(e) => {
                setSearchField(e.target.value);
                setCurrentPage(1);
              }}
              className="appearance-none bg-transparent pr-6 py-1.5 text-sm text-slate-700 focus:outline-none cursor-pointer min-w-[120px]"
            >
              {searchFieldOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
          <div className="relative w-72">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
            <input 
              type="text" 
              placeholder="搜索专利信息..." 
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full pl-9 pr-4 py-2 bg-white border border-slate-300 rounded-lg text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all shadow-sm"
            />
            {searchTerm && (
              <button
                onClick={() => {
                  setSearchTerm('');
                  setCurrentPage(1);
                }}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                <X size={14} />
              </button>
            )}
          </div>
          <div className="relative">
            <button
              onClick={() => setIsColumnSelectorOpen(!isColumnSelectorOpen)}
              className="inline-flex items-center gap-2 px-3 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors shadow-sm"
            >
              <List size={16} />
              选择列 ({visibleColumns.length})
            </button>
            {isColumnSelectorOpen && (
              <>
                <div className="fixed inset-0 z-10" onClick={() => setIsColumnSelectorOpen(false)} />
                <div className="absolute left-0 top-full mt-2 w-80 bg-white border border-slate-200 rounded-xl shadow-lg z-20 max-h-[60vh] overflow-y-auto custom-scrollbar">
                  <div className="p-3 border-b border-slate-100 sticky top-0 bg-white">
                    <div className="flex justify-between items-center">
                      <span className="text-sm font-medium text-slate-700">选择显示列</span>
                      <button
                        onClick={resetColumns}
                        className="text-xs text-blue-600 hover:text-blue-700 font-medium"
                      >
                        重置
                      </button>
                    </div>
                  </div>
                  <div className="p-2">
                    {allColumns.map((col) => (
                      <label
                        key={col.key}
                        className="flex items-center gap-2 px-2 py-1.5 hover:bg-slate-50 rounded-lg cursor-pointer"
                      >
                        <input
                          type="checkbox"
                          checked={visibleColumns.includes(col.key)}
                          onChange={() => toggleColumn(col.key)}
                          className="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500"
                        />
                        <span className="text-sm text-slate-700">{col.label}</span>
                      </label>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className="text-sm text-slate-500">共 <span className="font-semibold text-slate-700">{total}</span> 条</span>
        </div>
      </div>
      
      <div className="flex-1 min-h-0">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 size={32} className="animate-spin text-blue-600" />
          </div>
        ) : (
          <div className="h-full overflow-auto border-t border-slate-100">
            <table className="border-collapse">
              <thead className="bg-slate-50 sticky top-0 z-10">
                <tr>
                  <th className="px-4 py-3 text-center text-xs font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap border-b border-slate-200 bg-slate-50 min-w-[120px]">
                    操作
                  </th>
                  {visibleColumns.map((col) => {
                    const colInfo = allColumns.find(c => c.key === col);
                    return (
                      <th key={col} className="px-4 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider whitespace-nowrap border-b border-slate-200 bg-slate-50">
                        {colInfo?.label || col}
                      </th>
                    );
                  })}
                </tr>
              </thead>
              <tbody className="bg-white">
                {paginatedData.length === 0 && data.length > 0 ? (
                  <tr>
                    <td colSpan={visibleColumns.length + 1} className="px-4 py-16 text-center text-slate-500">
                      <Database size={40} className="mx-auto mb-3 text-slate-300" />
                      <p className="text-base font-medium text-slate-600 mb-1">暂无匹配的专利数据</p>
                      <p className="text-sm text-slate-400">请尝试调整搜索条件</p>
                    </td>
                  </tr>
                ) : paginatedData.length === 0 && data.length === 0 ? (
                  <tr>
                    <td colSpan={visibleColumns.length + 1} className="px-4 py-20 text-center text-slate-500 bg-white border-dashed">
                      <div className="flex flex-col items-center justify-center">
                        <Database size={48} className="text-slate-300 mb-4" />
                        <p className="text-lg font-medium text-slate-700 mb-2">暂无专利数据</p>
                        <p className="text-sm text-slate-500 mb-6">请导入包含专利信息的 Excel 文件以开始管理</p>
                        <button
                          onClick={onImportClick}
                          className="inline-flex items-center gap-2 px-5 py-2.5 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors shadow-sm"
                        >
                          导入 Excel 数据
                        </button>
                      </div>
                    </td>
                  </tr>
                ) : (
                  paginatedData.map((row, rowIndex) => (
                    <tr key={rowIndex} className="hover:bg-slate-50/50 transition-colors group">
                      <td className="px-4 py-3 whitespace-nowrap text-center">
                        <div className="flex items-center justify-center gap-1">
                          <button
                            onClick={() => { setViewingRecord(row); setIsViewModalOpen(true); }}
                            className="p-1.5 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="查看详情"
                          >
                            <Eye size={16} />
                          </button>
                          <button
                            onClick={() => openEditModal(row)}
                            className="p-1.5 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                            title="编辑"
                          >
                            <Edit2 size={16} />
                          </button>
                          <button
                            onClick={() => handleDelete(row)}
                            className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="删除"
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                      {visibleColumns.map((col) => (
                        <td key={col} className="px-4 py-3 whitespace-nowrap text-sm text-slate-600 max-w-[200px] truncate" title={String(row[col] || '')}>
                          {renderCellValue(col, row[col])}
                        </td>
                      ))}
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {filteredData.length > 0 && (
        <div className="px-6 py-4 border-t border-slate-200 bg-white shrink-0">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <span className="text-sm text-slate-500">每页显示</span>
              <div className="flex items-center bg-slate-50 rounded-lg p-1 border border-slate-200">
                {[10, 20, 50, 100].map((s) => (
                  <button
                    key={s}
                    onClick={() => { setItemsPerPage(s); setCurrentPage(1); }}
                    className={`px-3 py-1.5 text-sm font-medium rounded-md transition-all ${
                      itemsPerPage === s
                        ? 'bg-white text-blue-600 shadow-sm'
                        : 'text-slate-600 hover:text-slate-900'
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
              <span className="text-sm text-slate-500">条</span>
            </div>

            <div className="flex items-center gap-1">
              <button
                onClick={() => setCurrentPage(1)}
                disabled={currentPage === 1}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="首页"
              >
                «
              </button>
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="上一页"
              >
                ‹
              </button>

              <div className="flex items-center gap-1 mx-2">
                {(() => {
                  const pages: (number | string)[] = [];
                  const showPages = 5;
                  
                  if (totalPages <= showPages + 2) {
                    for (let i = 1; i <= totalPages; i++) pages.push(i);
                  } else {
                    pages.push(1);
                    if (currentPage > 3) pages.push('...');
                    const start = Math.max(2, currentPage - 1);
                    const end = Math.min(totalPages - 1, currentPage + 1);
                    for (let i = start; i <= end; i++) pages.push(i);
                    if (currentPage < totalPages - 2) pages.push('...');
                    pages.push(totalPages);
                  }
                  
                  return pages.map((p, i) => (
                    p === '...' ? (
                      <span key={`ellipsis-${i}`} className="px-2 text-slate-400">...</span>
                    ) : (
                      <button
                        key={p}
                        onClick={() => setCurrentPage(p as number)}
                        className={`w-9 h-9 flex items-center justify-center text-sm font-medium rounded-lg transition-all ${
                          currentPage === p
                            ? 'bg-blue-600 text-white shadow-sm'
                            : 'text-slate-600 hover:bg-slate-100'
                        }`}
                      >
                        {p}
                      </button>
                    )
                  ));
                })()}
              </div>

              <button
                onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="下一页"
              >
                ›
              </button>
              <button
                onClick={() => setCurrentPage(totalPages)}
                disabled={currentPage === totalPages}
                className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:bg-transparent transition-colors"
                title="末页"
              >
                »
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};