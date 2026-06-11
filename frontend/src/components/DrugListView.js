import React, { useState } from 'react';

/**
 * 药品列表视图组件
 * 支持紧凑列表显示、展开详情、多选批量操作
 */
function DrugListView({ 
  drugList, 
  searchQuery, 
  isSearching,
  filteredDrugList,
  calendarPlans,
  onSearch,
  onAddDrug,
  onOpenDrugDetail,
  onOpenAddToPlanModal,
  onDiscardDrug,
  onDeleteDrug,
  onReloadDrugList  // 新增：重新加载药品列表的回调
}) {
  // 展开状态
  const [expandedDrugs, setExpandedDrugs] = useState({});
  // 选中状态
  const [selectedDrugs, setSelectedDrugs] = useState(new Set());
  // 多选模式
  const [isSelectMode, setIsSelectMode] = useState(false);

  // 检查药品是否已设置用药计划
  const hasPlan = (drug) => {
    return calendarPlans.some(p => p.boxItemId === drug.boxItemId);
  };

  // 切换药品展开/折叠
  const toggleDrugExpand = (boxItemId) => {
    setExpandedDrugs(prev => ({
      ...prev,
      [boxItemId]: !prev[boxItemId]
    }));
  };

  // 切换药品选中状态
  const toggleDrugSelect = (boxItemId) => {
    setSelectedDrugs(prev => {
      const newSet = new Set(prev);
      if (newSet.has(boxItemId)) {
        newSet.delete(boxItemId);
      } else {
        newSet.add(boxItemId);
      }
      return newSet;
    });
  };

  // 全选/取消全选
  const toggleSelectAll = () => {
    if (selectedDrugs.size === drugList.length) {
      setSelectedDrugs(new Set());
    } else {
      setSelectedDrugs(new Set(drugList.map(d => d.boxItemId)));
    }
  };

  // 批量丢弃
  const handleBatchDiscard = async () => {
    if (selectedDrugs.size === 0) {
      alert('请先选择要丢弃的药品');
      return;
    }

    if (!window.confirm(`确定要丢弃选中的 ${selectedDrugs.size} 盒药品吗？`)) {
      return;
    }

    try {
      const promises = Array.from(selectedDrugs).map(boxItemId =>
        fetch(`http://localhost:8080/api/medicine-box/${boxItemId}/discard`, {
          method: 'PATCH',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
      );

      await Promise.all(promises);
      alert(`成功丢弃 ${selectedDrugs.size} 盒药品`);
      setSelectedDrugs(new Set());
      setIsSelectMode(false);
      // 重新加载药品列表
      if (onReloadDrugList) {
        onReloadDrugList();
      }
    } catch (err) {
      console.error('批量丢弃失败:', err);
      alert('批量丢弃失败，请重试');
    }
  };

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedDrugs.size === 0) {
      alert('请先选择要删除的药品');
      return;
    }

    if (!window.confirm(`确定要删除选中的 ${selectedDrugs.size} 盒药品吗？此操作不可恢复！`)) {
      return;
    }

    try {
      const promises = Array.from(selectedDrugs).map(boxItemId =>
        fetch(`http://localhost:8080/api/medicine-box/${boxItemId}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`
          }
        })
      );

      await Promise.all(promises);
      alert(`成功删除 ${selectedDrugs.size} 盒药品`);
      setSelectedDrugs(new Set());
      setIsSelectMode(false);
      if (onReloadDrugList) {
        onReloadDrugList();
      }
    } catch (err) {
      console.error('批量删除失败:', err);
      alert('批量删除失败，请重试');
    }
  };

  // 显示列表（过滤后或完整列表）
  const displayList = filteredDrugList.length > 0 && searchQuery.trim() ? filteredDrugList : drugList;

  // 排序逻辑：已过期 > 临期 > 正常
  const sortedList = [...displayList].sort((a, b) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const expiryDateA = new Date(a.expiryDate);
    expiryDateA.setHours(0, 0, 0, 0);
    const daysUntilExpiryA = Math.ceil((expiryDateA - today) / (1000 * 60 * 60 * 24));
    
    const expiryDateB = new Date(b.expiryDate);
    expiryDateB.setHours(0, 0, 0, 0);
    const daysUntilExpiryB = Math.ceil((expiryDateB - today) / (1000 * 60 * 60 * 24));
    
    const isExpiredA = daysUntilExpiryA < 0;
    const isExpiringA = daysUntilExpiryA >= 0 && daysUntilExpiryA <= 7;
    const isExpiredB = daysUntilExpiryB < 0;
    const isExpiringB = daysUntilExpiryB >= 0 && daysUntilExpiryB <= 7;
    
    // 优先级：已过期(0) > 临期(1) > 正常(2)
    const priorityA = isExpiredA ? 0 : (isExpiringA ? 1 : 2);
    const priorityB = isExpiredB ? 0 : (isExpiringB ? 1 : 2);
    
    if (priorityA !== priorityB) {
      return priorityA - priorityB;
    }
    
    // 同优先级内，按效期排序（临期/过期越近越靠前）
    return daysUntilExpiryA - daysUntilExpiryB;
  });

  return (
    <div className="card">
      <h2 className="card-title">
        <span className="card-title-icon">🏠</span>
        家庭药箱
      </h2>

      <div className="search-box">
        <div className="search-input-wrapper">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            className="search-input"
            placeholder="搜索药品名称、用量或备注..."
            value={searchQuery}
            onChange={(e) => onSearch(e.target.value)}
          />
        </div>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button 
            className="btn btn-primary btn-large" 
            onClick={onAddDrug}
          >
            ➕ 添加新药
          </button>
          <button 
            className="btn btn-large" 
            onClick={() => setIsSelectMode(!isSelectMode)}
            style={{
              background: isSelectMode ? '#ef4444' : '#6b7280',
              color: 'white'
            }}
          >
            {isSelectMode ? '✕ 取消' : '☑ 多选'}
          </button>
        </div>
      </div>

      {/* 多选工具栏 */}
      {isSelectMode && (
        <div className="select-toolbar" style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '12px 16px',
          background: '#f3f4f6',
          borderRadius: '8px',
          marginBottom: '16px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <input
              type="checkbox"
              checked={selectedDrugs.size === sortedList.length && sortedList.length > 0}
              onChange={toggleSelectAll}
              style={{ width: '20px', height: '20px', cursor: 'pointer' }}
            />
            <span style={{ fontSize: '14px', color: '#6b7280' }}>
              已选 {selectedDrugs.size} 盒
            </span>
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              className="btn btn-large"
              onClick={handleBatchDiscard}
              disabled={selectedDrugs.size === 0}
              style={{
                background: '#f59e0b',
                color: 'white',
                opacity: selectedDrugs.size === 0 ? 0.5 : 1
              }}
            >
              🗑️ 批量丢弃
            </button>
            <button
              className="btn btn-large"
              onClick={handleBatchDelete}
              disabled={selectedDrugs.size === 0}
              style={{
                background: '#ef4444',
                color: 'white',
                opacity: selectedDrugs.size === 0 ? 0.5 : 1
              }}
            >
              🗑️ 批量删除
            </button>
          </div>
        </div>
      )}

      {/* 药品列表 */}
      <div className="drug-list-container">
        {sortedList.length === 0 && searchQuery.trim() && !isSearching ? (
          <div style={{ textAlign: 'center', padding: '48px', color: 'var(--text-light)' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
            <p style={{ fontSize: '22px' }}>未找到与"{searchQuery}"相关的药品</p>
            <p style={{ fontSize: '18px', marginTop: '12px' }}>请尝试其他关键词</p>
          </div>
        ) : (
          sortedList.map((drug) => {
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const expiryDate = new Date(drug.expiryDate);
            expiryDate.setHours(0, 0, 0, 0);
            const daysUntilExpiry = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
            
            const isExpired = daysUntilExpiry < 0;
            const isExpiring = daysUntilExpiry >= 0 && daysUntilExpiry <= 7;
            const isNormal = !isExpired && !isExpiring;
            
            const hasDrugPlan = hasPlan(drug);
            const isExpanded = expandedDrugs[drug.boxItemId];
            const isSelected = selectedDrugs.has(drug.boxItemId);
            
            return (
              <div key={drug.boxItemId} className="drug-list-wrapper">
                {/* 列表项 */}
                <div 
                  className={`drug-list-item ${isExpired ? 'expired' : isExpiring ? 'expiring' : ''} ${isSelected ? 'selected' : ''}`}
                  onClick={() => {
                    if (isSelectMode) {
                      toggleDrugSelect(drug.boxItemId);
                    } else {
                      toggleDrugExpand(drug.boxItemId);
                    }
                  }}
                >
                  {/* 多选框 */}
                  {isSelectMode && (
                    <input
                      type="checkbox"
                      checked={isSelected}
                      onChange={(e) => {
                        e.stopPropagation();
                        toggleDrugSelect(drug.boxItemId);
                      }}
                      style={{ width: '20px', height: '20px', cursor: 'pointer', marginRight: '12px' }}
                    />
                  )}
                  
                  {/* 药品图标 */}
                  <span className="drug-list-icon">💊</span>
                  
                  {/* 药品名称 */}
                  <span className="drug-list-name">{drug.name}</span>
                  
                  {/* 状态标签 */}
                  <span className={`drug-list-status ${isExpired ? 'expired' : isExpiring ? 'expiring' : 'normal'}`}>
                    {isExpired ? '已过期' : isExpiring ? '临期' : '正常'}
                  </span>
                  
                  {/* 展开箭头 */}
                  <span className="drug-list-arrow">
                    {isExpanded ? '▼' : '▶'}
                  </span>
                </div>
                
                {/* 展开的卡片 */}
                {isExpanded && (
                  <div className="drug-expanded-card">
                    <div 
                      className={`drug-bottle-card ${isExpired ? 'expired' : isExpiring ? 'expiring' : ''}`}
                      style={{ position: 'relative' }}
                    >
                      {isExpired && <span className="expired-tag">已过期</span>}
                      {isExpiring && !isExpired && <span className="expiring-tag">即将过期</span>}
                      {/* 未设置用药时段的提示图标 */}
                      {!hasDrugPlan && (
                        <span 
                          style={{
                            position: 'absolute',
                            top: '10px',
                            left: '10px',
                            background: '#fff3cd',
                            color: '#856404',
                            padding: '4px 8px',
                            borderRadius: '4px',
                            fontSize: '12px',
                            fontWeight: 'bold',
                            zIndex: 11
                          }}
                          title="请添加用药时段"
                        >
                          ⚠️ 未设置
                        </span>
                      )}
                      
                      {/* 添加到用药日历按钮 - 已过期药品不显示 */}
                      {!isExpired && (
                        <button
                          className="btn btn-primary add-to-calendar-btn"
                          onClick={(e) => onOpenAddToPlanModal(drug, e)}
                          title={hasDrugPlan ? "修改用药时段" : "添加到用药日历"}
                          style={{
                            position: 'absolute',
                            top: '10px',
                            right: '10px',
                            padding: '8px 12px',
                            fontSize: '14px',
                            zIndex: 10,
                            borderRadius: '8px',
                            background: hasDrugPlan ? '#28a745' : 'var(--tech-blue)',
                            color: 'white',
                            border: 'none',
                            cursor: 'pointer',
                            boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                          }}
                        >
                          📅
                        </button>
                      )}
                      
                      <div onClick={() => onOpenDrugDetail(drug)}>
                        <div className="bottle-icon">💊</div>
                        <h4 className="bottle-name">{drug.name}</h4>
                        <p className="bottle-info">规格：{drug.spec}</p>
                        <p className="bottle-info">用法：{drug.dosage}</p>
                        <p className="bottle-info">剩余：{drug.remaining}/{drug.totalQuantity}片</p>
                        <div className="bottle-progress">
                          <div className="bottle-progress-fill" style={{ width: `${Math.max(0, Math.min(100, (drug.remaining / drug.totalQuantity) * 100))}%` }}></div>
                        </div>
                        <p className="bottle-info" style={{ color: (isExpired || isExpiring) ? 'var(--warning-orange)' : 'var(--text-light)' }}>
                          效期：{drug.expiryDate}
                        </p>
                      </div>
                      
                      {/* 过期和临期药的"我已丢弃"按钮 */}
                      {(isExpired || isExpiring) && (
                        <button
                          className="btn btn-danger discard-btn"
                          onClick={(e) => {
                            e.stopPropagation();
                            onDiscardDrug(drug);
                          }}
                          style={{
                            position: 'absolute',
                            bottom: '15px',
                            right: '15px',
                            padding: '10px 20px',
                            fontSize: '14px',
                            fontWeight: '600',
                            borderRadius: '8px',
                            background: isExpired ? '#ef4444' : '#f59e0b',
                            color: 'white',
                            border: 'none',
                            cursor: 'pointer',
                            boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
                            zIndex: 10
                          }}
                        >
                          我已丢弃
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      <div className="stats-bar">
        <div className="stats-item">
          <div className="stats-value">{sortedList.length}</div>
          <div className="stats-label">种药品</div>
        </div>
        <div className="stats-item">
          <div className="stats-value warning">
            {sortedList.filter(d => {
              const expDate = new Date(d.expiryDate);
              expDate.setHours(0, 0, 0, 0);
              const days = Math.ceil((expDate - new Date()) / (1000 * 60 * 60 * 24));
              return days < 7;
            }).length}
          </div>
          <div className="stats-label">需关注</div>
        </div>
      </div>
    </div>
  );
}

export default DrugListView;
