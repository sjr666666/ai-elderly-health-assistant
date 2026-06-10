import React, { useState, useRef, useEffect, useMemo } from 'react';
import html2canvas from 'html2canvas';
import './App.css';
import Login from './components/Login';
import Register from './components/Register';
import ProfileModal from './components/ProfileModal';
import ProfileEdit from './components/ProfileEdit';
import EmergencyContacts from './components/EmergencyContacts';
import AddDrugModal from './components/AddDrugModal';
import EditDrugModal from './components/EditDrugModal';
import ConfirmDeleteModal from './components/ConfirmDeleteModal';
import ManualDrugSearch from './components/ManualDrugSearch';
import EmergencyAssistant from './components/EmergencyAssistant';
import AddToPlanModal from './components/AddToPlanModal';
import ConfirmDrugModal from './components/ConfirmDrugModal';
import MedicationReminderModal from './components/MedicationReminderModal';
import DrugListView from './components/DrugListView';
import { useToast } from './components/Toast';

function App() {
  const { showToast } = useToast();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState('home');
  const [drugList, setDrugList] = useState([]); // 从数据库动态加载
  const [imagePreview, setImagePreview] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showComplete, setShowComplete] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filteredDrugList, setFilteredDrugList] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [speechRate, setSpeechRate] = useState(1);
  const [reminders, setReminders] = useState([
    { id: 1, time: '08:00', period: '早上', drug: '硝苯地平缓释片', taken: true, missed: false },
    { id: 2, time: '12:00', period: '中午', drug: '二甲双胍片', taken: true, missed: false },
    { id: 3, time: '20:00', period: '晚上', drug: '硝苯地平缓释片', taken: false, missed: false }
  ]);
  const [calendarPlans, setCalendarPlans] = useState([]); // 从后端获取的用药计划
  const [isLoadingCalendar, setIsLoadingCalendar] = useState(false); // 用药日历加载状态
  const [calendarViewMode, setCalendarViewMode] = useState('today'); // 用药日历视图模式：today/week
  const [weeklyMedicationData, setWeeklyMedicationData] = useState(null); // 一周用药数据
  const [selectedWeekDay, setSelectedWeekDay] = useState(null); // 周视图中就地展开的某天（YYYY-MM-DD）
  const [showAddToPlanModal, setShowAddToPlanModal] = useState(false); // 添加到用药日历弹窗
  const [selectedDrugForPlan, setSelectedDrugForPlan] = useState(null); // 选中的要添加到计划的药品
  const [showCelebration, setShowCelebration] = useState(false);
  const [particles, setParticles] = useState([]);
  const [takenButtons, setTakenButtons] = useState({});
  const [recognizedDrugs, setRecognizedDrugs] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [showProfileEdit, setShowProfileEdit] = useState(false);
  const [emergencyContacts, setEmergencyContacts] = useState([]);
  const [showAddContact, setShowAddContact] = useState(false);
  const [showAddDrugModal, setShowAddDrugModal] = useState(false);
  const [showEditDrugModal, setShowEditDrugModal] = useState(false); // 编辑药品弹窗
  const [showConfirmDelete, setShowConfirmDelete] = useState(false); // 确认删除弹窗
  const [pendingDeleteDrug, setPendingDeleteDrug] = useState(null); // 待删除的药品
  const [showConfirmDrugModal, setShowConfirmDrugModal] = useState(false); // 确认药品弹窗
  const [pendingDrugInfo, setPendingDrugInfo] = useState(null); // 待确认的药品信息
  const [showDrugDetailModal, setShowDrugDetailModal] = useState(false); // 药品详情弹窗
  const [selectedDrug, setSelectedDrug] = useState(null); // 选中的药品
  const [drugsWithPlan, setDrugsWithPlan] = useState(new Set()); // 已设置用药计划的药品ID集合
  const [showExpiringDrugsModal, setShowExpiringDrugsModal] = useState(false); // 过期药品弹窗
  const [showExpiredDrugModal, setShowExpiredDrugModal] = useState(false); // 添加药品时发现过期的弹窗
  const [expiredDrugInfo, setExpiredDrugInfo] = useState(null); // 过期药品信息
  const [showTodayExpiredModal, setShowTodayExpiredModal] = useState(false); // 已过期且未丢弃药品弹窗
  const [todayExpiredDrugs, setTodayExpiredDrugs] = useState([]); // 已过期且未丢弃药品列表
  const [showUndoConfirmModal, setShowUndoConfirmModal] = useState(false); // 撤销确认弹窗
  const [pendingUndoId, setPendingUndoId] = useState(null); // 待撤销的计划ID
  const [showMedicationReminder, setShowMedicationReminder] = useState(false); // 用药提醒弹窗
  const [missedReminders, setMissedReminders] = useState([]); // 超时未服用的用药计划
  const lastCheckedTimeRef = useRef(null); // 上次检查的时间，避免重复提醒
  const lastReminderTimeRef = useRef(null); // 上次弹窗提醒的时间，避免频繁提醒
  
  // 药品冲突检测相关状态
  const [conflictReport, setConflictReport] = useState(null); // 冲突检测报告
  const [isCheckingConflicts, setIsCheckingConflicts] = useState(false); // 是否正在检测冲突
  const [conflictError, setConflictError] = useState(null); // 冲突检测错误
  const [showConflictReport, setShowConflictReport] = useState(false); // 是否显示冲突报告卡片
  
  // 新药入箱冲突检测弹窗相关状态
  const [showConflictAlert, setShowConflictAlert] = useState(false); // 新药入箱冲突检测结果弹窗
  const [conflictAlertResult, setConflictAlertResult] = useState(null); // 新药入箱冲突检测结果
  const [conflictNeedsRecheck, setConflictNeedsRecheck] = useState(false); // 冲突检测页面是否需要重新检测
  
  // 批量识别相关状态
  const [batchRecognizeItems, setBatchRecognizeItems] = useState([]); // 批量识别的图片列表
  const [batchSelectedForAdd, setBatchSelectedForAdd] = useState(new Set()); // 选中的要添加的药品
  const [isBatchAdding, setIsBatchAdding] = useState(false); // 是否正在添加到药箱
  const batchFileInputRef = useRef(null); // 批量文件输入引用
  
  const fileInputRef = useRef(null);
  const conflictReportRef = useRef(null); // 冲突报告卡片引用（用于弹窗显示）
  const screenshotContainerRef = useRef(null); // 隐藏的截图容器引用
  const [showScreenshotContainer, setShowScreenshotContainer] = useState(false); // 控制隐藏截图容器显示

  const expiringDrugsResult = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const expiringDrugs = [];
    const expiredDrugs = [];

    drugList.forEach(drug => {
      if (drug.expiryDate) {
        const expiryDate = new Date(drug.expiryDate);
        expiryDate.setHours(0, 0, 0, 0);

        const daysUntilExpiry = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));

        if (daysUntilExpiry < 0) {
          expiredDrugs.push({
            ...drug,
            daysUntilExpiry,
            isExpired: true
          });
        } else if (daysUntilExpiry <= 7) {
          // 临期药：当前时间距离有效期7天以内
          expiringDrugs.push({
            ...drug,
            daysUntilExpiry,
            isExpired: false
          });
        }
      }
    });

    expiringDrugs.sort((a, b) => a.daysUntilExpiry - b.daysUntilExpiry);
    expiredDrugs.sort((a, b) => b.daysUntilExpiry - a.daysUntilExpiry);

    return { expiredDrugs, expiringDrugs };
  }, [drugList]);

  const handleRegister = (registerData) => {
    setShowRegister(false);
  };

  // 从数据库加载药箱列表
  const loadMedicineBoxList = async (userId) => {
    if (!userId) return;
    
    try {
      const response = await fetch(`/api/v1/box/list?userId=${userId}`);
      const data = await response.json();
      
      console.log('=== 药箱列表响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);
      console.log('==================');
      
      if (response.ok && data.code === 200) {
        // 转换后端数据格式为前端需要的格式
        const drugs = data.data.map(item => ({
          boxItemId: item.boxItemId,
          drugId: item.drugId,
          name: item.drugName,
          genericName: item.genericName,
          tradeName: item.tradeName,
          commonName: item.commonName,
          spec: item.specification,
          dosage: item.dosage,
          frequency: item.frequency,
          startDate: item.startDate,
          endDate: item.endDate,
          expiryDate: item.expiryDate,
          totalQuantity: item.totalQuantity,
          remaining: item.remainingQuantity || item.totalQuantity, // 优先使用剩余数量
          note: item.note,
          status: item.status,
          createdAt: item.createdAt
        }));
        setDrugList(drugs);
      } else {
        console.error('获取药箱列表失败:', data.message);
      }
    } catch (err) {
      console.error('获取药箱列表异常:', err);
    }
  };

  // 从数据库加载紧急联系人列表
  const loadEmergencyContacts = async (elderId) => {
    if (!elderId) return;
    
    try {
      const response = await fetch(`/api/emergency/v1/contacts?elderId=${elderId}`);
      const data = await response.json();
      
      console.log('=== 紧急联系人列表响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);
      console.log('==================');
      
      if (response.ok && data.code === 200) {
        setEmergencyContacts(data.data);
      } else {
        console.error('获取紧急联系人列表失败:', data.message);
      }
    } catch (err) {
      console.error('获取紧急联系人列表异常:', err);
    }
  };

  // 今日用药计划缓存 key
  const TODAY_PLANS_CACHE_KEY = 'today_plans_cache';

  // 从本地缓存加载用药计划（断网时使用）
  const loadPlansFromCache = () => {
    try {
      const cached = localStorage.getItem(TODAY_PLANS_CACHE_KEY);
      if (cached) {
        let { plans, timestamp } = JSON.parse(cached);
        console.log('从本地缓存加载用药计划，缓存时间:', new Date(timestamp).toLocaleString());
        
        // 过滤掉已过期药品的用药计划
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        plans = plans.filter(plan => {
          const drug = drugList.find(d => d.boxItemId === plan.boxItemId);
          if (!drug) return true;
          
          if (drug.expiryDate) {
            const expiryDate = new Date(drug.expiryDate);
            expiryDate.setHours(0, 0, 0, 0);
            const isExpired = expiryDate < today;
            
            if (isExpired) {
              console.warn(`缓存中的药品已过期，已过滤 - 药品: ${drug.name}, 有效期: ${drug.expiryDate}`);
              return false;
            }
          }
          
          return true;
        });
        
        // 合并本地服药状态
        const plansWithStatus = mergeLocalMedicationStatus(plans);
        setCalendarPlans(plansWithStatus);
        
        const drugIds = new Set(plansWithStatus.map(p => p.drugId).filter(Boolean));
        setDrugsWithPlan(drugIds);
        
        showToast('已加载缓存的用药计划（离线模式）', 'info');
        return true;
      }
    } catch (err) {
      console.error('从缓存加载用药计划失败:', err);
    }
    setCalendarPlans([]);
    setDrugsWithPlan(new Set());
    return false;
  };

  // 从后端加载今日用药计划（根据家庭药箱自动生成）
  const loadCalendarPlans = async () => {
    if (!user || !user.userId) {
      console.warn('用户未登录，无法加载用药计划');
      setCalendarPlans([]);
      return;
    }
    
    setIsLoadingCalendar(true);
    
    try {
      const response = await fetch(`/api/v1/plan/generate-today?userId=${user.userId}`);
      const data = await response.json();
      
      console.log('=== 用药计划响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);
      console.log('==================');
      
      if (response.ok && data.code === 200 && data.data) {
        // 转换后端数据格式为前端需要的格式
        let plans = data.data.items.map((item, index) => ({
          id: item.planId || `temp_${index}_${Date.now()}`, // 使用planId或临时ID
          time: getTimeBySlot(item.timeSlot),
          period: item.timeSlotLabel,
          drug: item.drugName,
          dosage: item.dosageAtTime,
          taken: item.status === 'taken',
          missed: item.status === 'missed',
          planId: item.planId,
          timeSlot: item.timeSlot,
          status: item.status,
          boxItemId: item.boxItemId,
          remainingQuantity: item.remainingQuantity,
          boxDrugName: item.boxDrugName
        }));

        // 过滤掉已过期药品的用药计划（二次检查，确保数据安全）
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        let validPlans = plans.filter(plan => {
          // 如果有boxItemId，在drugList中查找对应药品
          const drug = drugList.find(d => d.boxItemId === plan.boxItemId);
          if (!drug) return true; // 找不到药品信息，保留该计划
          
          // 检查药品是否过期
          if (drug.expiryDate) {
            const expiryDate = new Date(drug.expiryDate);
            expiryDate.setHours(0, 0, 0, 0);
            const isExpired = expiryDate < today;
            
            if (isExpired) {
              console.warn(`用药计划中的药品已过期，已过滤 - 药品: ${drug.name}, 有效期: ${drug.expiryDate}`);
              return false; // 已过期，过滤掉
            }
          }
          
          return true; // 未过期，保留
        });

        // 合并 localStorage 中保存的本地服药状态
        validPlans = mergeLocalMedicationStatus(validPlans);
        
        setCalendarPlans(validPlans);
        // 更新已设置用药计划的药品ID集合
        const drugIds = new Set(validPlans.map(p => p.drugId).filter(Boolean));
        setDrugsWithPlan(drugIds);
        console.log('用药计划已更新，共', validPlans.length, '条记录（已过滤过期药品）');
        
        // 保存到本地缓存（用于断网可读）
        try {
          localStorage.setItem(TODAY_PLANS_CACHE_KEY, JSON.stringify({
            plans: validPlans,
            timestamp: Date.now()
          }));
          console.log('今日用药计划已缓存');
        } catch (cacheErr) {
          console.error('缓存用药计划失败:', cacheErr);
        }
      } else {
        console.error('获取用药计划失败:', data.message);
        // 尝试从缓存读取
        loadPlansFromCache();
      }
    } catch (err) {
      console.error('获取用药计划异常:', err);
      // 尝试从缓存读取
      loadPlansFromCache();
    } finally {
      setIsLoadingCalendar(false);
    }
  };

  // 从后端加载一周用药记录（包括已删除但在查询范围内的记录）
  const loadWeeklyMedication = async () => {
    if (!user || !user.userId) {
      console.warn('用户未登录，无法加载用药记录');
      setWeeklyMedicationData(null);
      return;
    }

    setIsLoadingCalendar(true);

    try {
      const response = await fetch(`/api/v1/plan/weekly?userId=${user.userId}`);
      const data = await response.json();

      console.log('=== 一周用药记录响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);
      console.log('==================');

      if (response.ok && data.code === 200 && data.data) {
        setWeeklyMedicationData(data.data);
      } else {
        console.error('获取一周用药记录失败:', data.message);
        setWeeklyMedicationData(null);
      }
    } catch (err) {
      console.error('获取一周用药记录异常:', err);
      setWeeklyMedicationData(null);
    } finally {
      setIsLoadingCalendar(false);
    }
  };

  // 获取今日的 localStorage key 前缀
  const getTodayStorageKey = () => {
    return `medication_status_${new Date().toISOString().split('T')[0]}`;
  };

  // 将本地服药状态保存到 localStorage
  const saveLocalMedicationStatus = (planId, status) => {
    try {
      const key = getTodayStorageKey();
      const savedStatus = JSON.parse(localStorage.getItem(key) || '{}');
      if (status == null) {
        delete savedStatus[planId];
      } else {
        savedStatus[planId] = {
          status: status, // 'taken' 或 null(撤销)
          timestamp: Date.now()
        };
      }
      localStorage.setItem(key, JSON.stringify(savedStatus));
      console.log(`已保存本地服药状态: planId=${planId}, status=${status}`);
    } catch (err) {
      console.error('保存本地服药状态失败:', err);
    }
  };

  // 合并 localStorage 中的服药状态到计划列表
  const mergeLocalMedicationStatus = (plans) => {
    try {
      const key = getTodayStorageKey();
      const savedStatus = JSON.parse(localStorage.getItem(key) || '{}');
      
      if (Object.keys(savedStatus).length > 0) {
        console.log('发现本地保存的服药状态，正在合并...', savedStatus);
        
        return plans.map(plan => {
          const localStatus = savedStatus[plan.id] || savedStatus[plan.planId];
          if (localStatus && localStatus.status === 'taken') {
            return { ...plan, taken: true, missed: false };
          }
          return plan;
        });
      }
      
      return plans;
    } catch (err) {
      console.error('合并本地服药状态失败:', err);
      return plans;
    }
  };

  // 根据时间段返回对应的时间字符串
  const getTimeBySlot = (timeSlot) => {
    switch (timeSlot) {
      case 'morning': return '08:00';
      case 'noon': return '12:00';
      case 'evening': return '18:00';
      case 'before_bed': return '21:00';
      default: return '08:00';
    }
  };

  // 打开添加到用药日历的弹窗
  const handleOpenAddToPlanModal = (drug, event) => {
    if (event) {
      event.stopPropagation(); // 阻止事件冒泡，避免触发药品详情
    }
    setSelectedDrugForPlan(drug);
    setShowAddToPlanModal(true);
  };

  // 关闭添加到用药日历的弹窗
  const handleCloseAddToPlanModal = () => {
    setShowAddToPlanModal(false);
    setSelectedDrugForPlan(null);
  };

  // 切换用药日历视图（今日/本周）
  const handleCalendarViewChange = (viewMode) => {
    setCalendarViewMode(viewMode);
    
    // 只在没有对应数据时才加载，避免重复请求
    if (viewMode === 'week' && !weeklyMedicationData) {
      loadWeeklyMedication();
    }
    // 今日视图的数据已经在登录时预加载了，不需要再次加载
  };

  // 提交添加到用药日历
  const handleSubmitAddToPlan = async (selectedTimeSlots) => {
    if (!selectedDrugForPlan || !selectedDrugForPlan.boxItemId) {
      showToast('请选择要添加的药品', 'warning');
      return;
    }

    if (!user || !user.userId) {
      showToast('用户未登录，请先登录', 'warning');
      return;
    }

    if (!selectedTimeSlots || selectedTimeSlots.length === 0) {
      showToast('请至少选择一个服药时间段', 'warning');
      return;
    }

    try {
      const response = await fetch('/api/v1/plan/add-from-box', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: user.userId,
          boxItemId: selectedDrugForPlan.boxItemId,
          timeSlots: selectedTimeSlots
        })
      });

      const data = await response.json();

      console.log('=== 添加到用药计划响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);

      if (response.ok && data.code === 200) {
        // 关闭弹窗
        handleCloseAddToPlanModal();

        // 更新已设置用药计划的药品集合
        setDrugsWithPlan(prev => new Set([...prev, selectedDrugForPlan.drugId]));

        // 显示成功提示
        showToast('已添加到用药日历！', 'success');

        // 刷新用药日历数据：今日 + 一周都刷
        loadCalendarPlans();
        if (typeof loadWeeklyMedication === 'function') {
          loadWeeklyMedication();
        }
      } else {
        showToast(data.message || '添加失败，请重试', 'error');
      }
    } catch (error) {
      console.error('添加到用药计划失败:', error);
      showToast('网络连接失败，请稍后重试', 'error');
    }
  };

  const handleLogin = (loginData) => {
    setUser({
      username: loginData.username,
      ...loginData
    });
    setIsLoggedIn(true);
    
    // 保存登录状态到 localStorage
    localStorage.setItem('user', JSON.stringify({
      username: loginData.username,
      ...loginData
    }));
    
    // 登录成功后加载药箱列表和紧急联系人
    if (loginData.userId) {
      loadMedicineBoxList(loginData.userId);
      // 不在登录时预加载日历，避免阻塞其他页面
      // 只有当用户真正切换到日历页面时才加载
      
      // 检查已过期且未丢弃的药品
      setTimeout(() => {
        checkTodayExpiredMedicines(loginData.userId);
      }, 500); // 延迟500ms执行，确保药箱列表已加载
    }
    if (loginData.id) {
      loadEmergencyContacts(loginData.id);
    }
    
    if (loginData.needProfile) {
      setShowProfileModal(true);
    }
  };

  const handleProfileComplete = (profileData) => {
    setUser(prev => ({
      ...prev,
      ...profileData
    }));
    setShowProfileModal(false);
  };

  const handleProfileUpdate = (profileData) => {
    setUser(prev => ({
      ...prev,
      ...profileData
    }));
    setShowProfileEdit(false);
    
    // 显示成功提示弹窗
    showToast('个人信息已更新！', 'success');
  };

  // 手动触发用药提醒（用于调试）
  const handleTriggerReminderManually = () => {
    console.log('手动触发用药提醒');
    
    // 获取当前超时的用药计划
    if (!calendarPlans || calendarPlans.length === 0) {
      showToast('当前没有用药计划', 'warning');
      return;
    }

    const now = new Date();
    const currentHours = now.getHours();
    const currentMinutes = now.getMinutes();
    const currentTimeInMinutes = currentHours * 60 + currentMinutes;

    // 找出所有超时需要服用但还未服用的用药计划
    const missed = calendarPlans.filter(reminder => {
      if (reminder.taken || reminder.missed) {
        return false;
      }
      const [hours, minutes] = reminder.time.split(':').map(Number);
      const reminderTimeInMinutes = hours * 60 + minutes;
      const timeDiff = currentTimeInMinutes - reminderTimeInMinutes;
      return timeDiff >= 0;
    });

    if (missed.length === 0) {
      showToast('当前没有需要服用的药物', 'info');
      // 显示所有待服用的药物，即使还没超时
      const pending = calendarPlans.filter(reminder => !reminder.taken && !reminder.missed);
      if (pending.length > 0) {
        setMissedReminders(pending);
        setShowMedicationReminder(true);
        lastReminderTimeRef.current = Date.now();
      }
      return;
    }

    setMissedReminders(missed);
    setShowMedicationReminder(true);
    lastReminderTimeRef.current = Date.now();
    console.log('手动触发用药提醒:', missed);
  };

  const handleAddContact = async () => {
    if (user && user.id) {
      await loadEmergencyContacts(user.id);
      showToast('紧急联系人已添加！', 'success');
    } else {
      console.error('用户ID为空，无法刷新联系人列表');
    }
  };

  const handleDeleteContact = async (contactId) => {
    if (user && user.id) {
      try {
        const response = await fetch(`/api/emergency/v1/contacts/${contactId}`, {
          method: 'DELETE'
        });
        const result = await response.json();
        
        if (result.code === 200) {
          await loadEmergencyContacts(user.id);
          showToast('联系人已删除！', 'success');
        } else {
          console.error('删除联系人失败:', result.message);
          showToast('删除失败：' + (result.message || '未知错误'), 'error');
        }
      } catch (error) {
        console.error('删除联系人失败:', error);
        showToast('删除失败，请检查网络连接', 'error');
      }
    } else {
      console.error('用户ID为空，无法删除联系人');
    }
  };

  // 打开药品详情弹窗
  const handleOpenDrugDetail = (drug) => {
    setSelectedDrug(drug);
    setShowDrugDetailModal(true);
  };

  // 关闭药品详情弹窗
  const handleCloseDrugDetail = () => {
    setShowDrugDetailModal(false);
    setSelectedDrug(null);
  };

  // 编辑药品
  const handleEditDrug = (drug) => {
    // 关闭药品详情弹窗
    setShowDrugDetailModal(false);
    // 打开编辑弹窗
    setShowEditDrugModal(true);
  };

  // 保存编辑药品
  const handleSaveEditDrug = async (updatedDrug) => {
    // 关闭编辑弹窗
    setShowEditDrugModal(false);
    
    // 更新选中的药品数据（保持详情弹窗打开）
    setSelectedDrug(updatedDrug);
    
    // 显示成功提示
    showToast('药品修改成功！', 'success');
    
    // 重新加载列表（后台刷新，不影响当前详情显示）
    if (user && user.userId) {
      await loadMedicineBoxList(user.userId);
    }
  };

  // 删除药品
  const handleDeleteDrug = (drug) => {
    // 打开确认删除弹窗
    setPendingDeleteDrug(drug);
    setShowConfirmDelete(true);
  };

  // 确认删除药品
  const handleConfirmDelete = async () => {
    if (!pendingDeleteDrug) return;
    
    try {
      const response = await fetch(`/api/v1/box/${pendingDeleteDrug.boxItemId}?userId=${user.userId}`, { method: 'DELETE' });
      const data = await response.json();
      
      console.log('=== 删除药品响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);
      console.log('==================');
      
      if (response.ok && data.code === 200) {
        // 关闭确认弹窗
        setShowConfirmDelete(false);
        setPendingDeleteDrug(null);
        
        // 关闭详情弹窗
        handleCloseDrugDetail();
        
        // 显示成功提示
        showToast(data.message || '药品删除成功！', 'success');
        
        // 重新加载列表（从数据库获取最新数据）
        if (user && user.userId) {
          await loadMedicineBoxList(user.userId);
        }
      } else {
        showToast(data.message || '删除失败，请重试', 'error');
      }
    } catch (err) {
      console.error('删除药品异常:', err);
      showToast('网络连接失败，请稍后重试', 'error');
    }
  };

  // 取消删除
  const handleCancelDelete = () => {
    setShowConfirmDelete(false);
    setPendingDeleteDrug(null);
  };

  // 新药入箱后自动检测冲突（只检测与新药相关的冲突）
  const checkConflictsForNewDrug = async (newDrugName, currentDrugList) => {
    if (!currentDrugList || currentDrugList.length === 0) return { noConflict: true, reason: 'empty' };
    
    try {
      // 获取药箱中除新药外的其他药品进行冲突检测
      const otherDrugs = currentDrugList.filter(drug => drug.name !== newDrugName);
      if (otherDrugs.length === 0) return { noConflict: true, reason: 'firstDrug' };
      
      // 只检测新药与药箱中其他药品的冲突
      const drugNames = [newDrugName, ...otherDrugs.map(drug => drug.name)];
      const response = await fetch('/api/conflict/check', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(drugNames)
      });
      const data = await response.json();
      if (data.code === 200 && data.data) {
        // 过滤出只与新药相关的冲突
        const newDrugConflicts = data.data.conflicts?.filter(
          conflict => conflict.drugA === newDrugName || conflict.drugB === newDrugName
        ) || [];
        
        // 如果没有冲突，返回标记
        if (newDrugConflicts.length === 0) {
          return { noConflict: true, reason: 'noConflictFound' };
        }
        
        // 返回只包含新药相关冲突的结果
        return {
          noConflict: false,
          conflicts: newDrugConflicts,
          statistics: {
            ...data.data.statistics,
            totalConflicts: newDrugConflicts.length,
            severeCount: newDrugConflicts.filter(c => c.severity === 'SEVERE').length,
            moderateCount: newDrugConflicts.filter(c => c.severity === 'MODERATE').length,
            mildCount: newDrugConflicts.filter(c => c.severity === 'MILD').length
          }
        };
      }
    } catch (error) {
      console.error('新药冲突检测失败:', error);
    }
    return null;
  };

  // 添加药品到药箱（由 AddDrugModal 组件内部调用 API）
  const handleAddDrug = async (drugData) => {
    // 检查是否是过期药品
    if (drugData.expired) {
      // 显示过期弹窗
      setExpiredDrugInfo({
        drugName: drugData.drugName || drugData.genericName || '未知药品',
        expiryDate: drugData.expiryDate || '未知日期'
      });
      setShowExpiredDrugModal(true);
      setShowAddDrugModal(false);
      return;
    }
    
    // 构造新药数据（用于冲突检测）
    const newDrug = {
      boxItemId: drugData.boxItemId,
      drugId: drugData.drugId,
      name: drugData.drugName || drugData.genericName,
      spec: drugData.spec || drugData.specification,
      manufacturer: drugData.manufacturer,
      dosage: drugData.dosage,
      frequency: drugData.frequency,
      startDate: drugData.startDate,
      endDate: drugData.endDate,
      expiryDate: drugData.expiryDate,
      totalQuantity: drugData.totalQuantity,
      remaining: drugData.totalQuantity,
      note: drugData.note
    };
    
    setShowAddDrugModal(false);
    
    // 显示成功提示
    showToast('药品添加成功！', 'success');
    
    // 重新加载药箱列表
    if (user && user.userId) {
      await loadMedicineBoxList(user.userId);
      
      // 如果当前在用药日历页面，同时刷新用药计划
      if (activeTab === 'calendar') {
        loadCalendarPlans();
      }
      
      // 新药入箱后自动触发冲突检测
      const conflictResult = await checkConflictsForNewDrug(newDrug.name, drugList);
      if (conflictResult) {
        if (conflictResult.noConflict) {
          // 没有冲突，不显示弹窗
        } else {
          // 检测到冲突，显示弹窗
          setConflictAlertResult(conflictResult);
          setShowConflictAlert(true);
        }
      }
      setConflictNeedsRecheck(true);
      setConflictReport(null);
    }
  };

  // 关闭用药提醒弹窗
  const handleCloseMedicationReminder = () => {
    // 关闭弹窗时自动停止播报
    stopSpeaking();
    // 重置提醒ID，允许下次打开时重新播报
    lastReminderIdRef.current = null;
    setShowMedicationReminder(false);
    setMissedReminders([]);
    // 不重置 lastReminderTimeRef，确保下次提醒要等3分钟后
  };

  // 从提醒弹窗中标记为已服用
  // 用于追踪是否需要重新显示弹窗
  const [shouldRefreshReminder, setShouldRefreshReminder] = useState(false);

  // 监听calendarPlans变化，当标记为已服用后自动刷新弹窗
  useEffect(() => {
    if (shouldRefreshReminder && calendarPlans.length > 0) {
      setShouldRefreshReminder(false);
      
      const now = new Date();
      const currentHours = now.getHours();
      const currentMinutes = now.getMinutes();
      const currentTimeInMinutes = currentHours * 60 + currentMinutes;
      
      // 从最新的calendarPlans中筛选未服用的药物
      const remainingMissed = calendarPlans.filter(p => 
        !p.taken && !p.missed && 
        (() => {
          const [hours, minutes] = p.time.split(':').map(Number);
          const reminderTimeInMinutes = hours * 60 + minutes;
          return currentTimeInMinutes - reminderTimeInMinutes >= 0;
        })()
      );
      
      console.log('刷新弹窗 - 剩余未服用的药物数量:', remainingMissed.length);
      
      // 如果还有未服用的药物，重新打开弹窗
      if (remainingMissed.length > 0) {
        console.log('还有未服用的药物，重新打开弹窗:', remainingMissed);
        setMissedReminders(remainingMissed);
        setShowMedicationReminder(true);
        lastReminderTimeRef.current = Date.now();
      } else {
        // 所有药物都已服用，关闭弹窗
        console.log('所有药物都已服用，关闭弹窗');
        handleCloseMedicationReminder();
      }
    }
  }, [calendarPlans, shouldRefreshReminder]);

  const handleMarkAsTakenFromReminder = async (reminder) => {
    console.log('标记为已服用:', reminder);

    // markAsTaken 内部已经 await API + reload 两个视图，这里不再重复 reload
    await markAsTaken(reminder.id, null);

    // 触发 useEffect 重新计算漏服提醒
    setShouldRefreshReminder(true);
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setUser(null);
    setShowProfileModal(false);
    setActiveTab('home');
    
    // 清除 localStorage 中的登录状态
    localStorage.removeItem('user');
  };

  // 打开过期药品弹窗
  const handleOpenExpiringDrugsModal = () => {
    setShowExpiringDrugsModal(true);
  };

  // 关闭过期药品弹窗
  const handleCloseExpiringDrugsModal = () => {
    setShowExpiringDrugsModal(false);
  };

  // 关闭添加药品时发现过期的弹窗
  const handleCloseExpiredDrugModal = () => {
    setShowExpiredDrugModal(false);
    setExpiredDrugInfo(null);
  };

  // 关闭已过期且未丢弃药品弹窗
  const handleCloseTodayExpiredModal = () => {
    setShowTodayExpiredModal(false);
    setTodayExpiredDrugs([]);
  };

  // 检查所有已过期且未丢弃的药品（status=active）
  const checkTodayExpiredMedicines = async (userId) => {
    if (!userId) return;
    
    try {
      console.log('=== 检查已过期且未丢弃的药品 ===');
      const response = await fetch(`/api/v1/box/expired/today?userId=${userId}`);
      const data = await response.json();
      
      console.log('响应数据:', data);
      console.log('================================');
      
      if (response.ok && data.code === 200 && data.data && data.data.length > 0) {
        // 有已过期且未丢弃的药品，显示弹窗
        setTodayExpiredDrugs(data.data);
        setShowTodayExpiredModal(true);
        console.log(`发现 ${data.data.length} 个已过期且未丢弃的药品`);
      }
    } catch (error) {
      console.error('检查已过期药品失败:', error);
      // 不显示错误提示，避免影响用户体验
    }
  };

  // 点击过期药品查看详情
  const handleExpiringDrugClick = (drug) => {
    setSelectedDrug(drug);
    setShowExpiringDrugsModal(false);
    setShowDrugDetailModal(true);
  };

  // 处理丢弃临期/过期药品
  const handleDiscardDrug = async () => {
    if (!selectedDrug || !selectedDrug.boxItemId) {
      showToast('缺少必要参数，无法丢弃', 'error');
      return;
    }

    // 从 localStorage 获取用户ID，确保一定能拿到
    const storedUser = localStorage.getItem('user');
    let currentUserId = null;
    
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        currentUserId = userData.userId || userData.id;
      } catch (e) {
        console.error('解析用户数据失败:', e);
      }
    }

    if (!currentUserId) {
      showToast('用户信息异常，请重新登录', 'error');
      return;
    }

    try {
      // 调用后端API更新status为stopped
      const response = await fetch(`/api/v1/box/${selectedDrug.boxItemId}?userId=${currentUserId}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          status: 'stopped'
        })
      });

      const data = await response.json();
      
      if (response.ok && data.code === 200) {
        showToast('已标记为丢弃，药品已移除', 'success');
        // 关闭详情弹窗
        setShowDrugDetailModal(false);
        // 重新加载药箱列表
        await loadMedicineBoxList(currentUserId);
      } else {
        showToast(data.message || '丢弃失败', 'error');
      }
    } catch (error) {
      console.error('丢弃药品失败:', error);
      showToast('丢弃失败，请稍后重试', 'error');
    }
  };

  // 处理从药箱卡片直接丢弃药品
  const handleDiscardDrugFromCard = async (drug) => {
    if (!drug || !drug.boxItemId) {
      showToast('缺少必要参数，无法丢弃', 'error');
      return;
    }

    // 从 localStorage 获取用户ID
    const storedUser = localStorage.getItem('user');
    let currentUserId = null;
    
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        currentUserId = userData.userId || userData.id;
      } catch (e) {
        console.error('解析用户数据失败:', e);
      }
    }

    if (!currentUserId) {
      showToast('用户信息异常，请重新登录', 'error');
      return;
    }

    try {
      // 调用后端API更新status为stopped
      const response = await fetch(`/api/v1/box/${drug.boxItemId}?userId=${currentUserId}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          status: 'stopped'
        })
      });

      const data = await response.json();
      
      if (response.ok && data.code === 200) {
        showToast('已标记为丢弃，药品已移除', 'success');
        // 重新加载药箱列表
        await loadMedicineBoxList(currentUserId);
      } else {
        showToast(data.message || '丢弃失败', 'error');
      }
    } catch (error) {
      console.error('丢弃药品失败:', error);
      showToast('丢弃失败，请稍后重试', 'error');
    }
  };

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      console.log('文件选择:', file.name, file.type);
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
    
    // 关键：阻止浏览器默认的文件打开行为
    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'copy';
    }
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    // 只有当鼠标真正离开上传区域时才设置为非拖拽状态
    const rect = e.currentTarget.getBoundingClientRect();
    const clientX = e.clientX;
    const clientY = e.clientY;
    if (clientX < rect.left || clientX > rect.right || clientY < rect.top || clientY > rect.bottom) {
      setIsDragging(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    
    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target.result);
        
        // 同时更新隐藏的文件输入（确保后续分析函数能正确获取文件）
        const dt = new DataTransfer();
        dt.items.add(file);
        fileInputRef.current.files = dt.files;
        
        console.log('拖拽文件已设置:', file.name, file.type);
        
        // 自动开始识别
        setTimeout(() => {
          analyzeImage();
        }, 100);
      };
      reader.readAsDataURL(file);
    } else {
      console.warn('拖拽的文件不是图片类型');
      showToast('请选择图片文件！', 'warning');
    }
    
    // 确保焦点回到窗口，关闭可能的文件对话框
    window.focus();
  };

  const handleDragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  // 应用初始化时检查 localStorage 中的登录状态
  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        const userData = JSON.parse(savedUser);
        if (userData && userData.userId) {
          setUser(userData);
          setIsLoggedIn(true);
          // 自动加载药箱列表和紧急联系人
          loadMedicineBoxList(userData.userId);
          if (userData.id) {
            loadEmergencyContacts(userData.id);
          }
          
          // 检查已过期且未丢弃的药品
          setTimeout(() => {
            checkTodayExpiredMedicines(userData.userId);
          }, 500); // 延迟500ms执行，确保药箱列表已加载
        }
      } catch (e) {
        console.error('解析用户数据失败:', e);
        localStorage.removeItem('user');
      }
    }
  }, []);

  // 全局阻止拖拽默认行为，防止文件被浏览器打开
  useEffect(() => {
    const preventDefault = (e) => {
      e.preventDefault();
      e.stopPropagation();
    };

    // 阻止所有拖拽的默认行为
    document.addEventListener('dragover', preventDefault);
    document.addEventListener('dragenter', preventDefault);
    document.addEventListener('drop', preventDefault);

    return () => {
      // 组件卸载时移除监听器
      document.removeEventListener('dragover', preventDefault);
      document.removeEventListener('dragenter', preventDefault);
      document.removeEventListener('drop', preventDefault);
    };
  }, []);

  // 将语音播报方法绑定到 window 对象，供 MedicationReminderModal 使用
  useEffect(() => {
    window.speakMedicationReminder = (text) => {
      speak(text);
    };
    
    return () => {
      delete window.speakMedicationReminder;
    };
  }, []); // 空依赖数组，只执行一次

  // 用药提醒：每3分钟检查一次是否有超时需要服用的药
  useEffect(() => {
    // 只要用户登录就启用提醒（不再限制在用药日历页面）
    if (!isLoggedIn) {
      return;
    }

    // 检查是否有超时需要服用的用药计划
    const checkMissedReminders = () => {
      if (!calendarPlans || calendarPlans.length === 0) {
        return;
      }

      const now = new Date();
      const currentHours = now.getHours();
      const currentMinutes = now.getMinutes();
      const currentTimeInMinutes = currentHours * 60 + currentMinutes;

      // 找出所有超时需要服用但还未服用的用药计划
      const missed = calendarPlans.filter(reminder => {
        // 跳过已经服用或标记为漏服的
        if (reminder.taken || reminder.missed) {
          return false;
        }

        // 解析用药时间
        const [hours, minutes] = reminder.time.split(':').map(Number);
        const reminderTimeInMinutes = hours * 60 + minutes;

        // 如果当前时间超过用药时间，视为超时
        const timeDiff = currentTimeInMinutes - reminderTimeInMinutes;
        return timeDiff >= 0;
      });

      // 如果有超时药物，且距离上次提醒已经超过3分钟，才再次提醒
      if (missed.length > 0) {
        const now = Date.now();
        const shouldRemind = !lastReminderTimeRef.current || 
                            (now - lastReminderTimeRef.current) >= 3 * 60 * 1000; // 3分钟间隔
        
        if (shouldRemind) {
          setMissedReminders(missed);
          setShowMedicationReminder(true);
          lastReminderTimeRef.current = now; // 记录本次提醒时间
          console.log('触发用药提醒:', missed);
        }
      }
    };

    // 立即检查一次
    checkMissedReminders();

    // 每3分钟检查一次并触发提醒（从60秒改为180秒）
    const intervalId = setInterval(checkMissedReminders, 3 * 60 * 1000);

    return () => {
      clearInterval(intervalId);
    };
  }, [isLoggedIn, calendarPlans]);

  // 监听弹窗显示，自动播报（只在弹窗首次打开时播报一次）
  const lastReminderIdRef = useRef(null);
  
  useEffect(() => {
    if (showMedicationReminder && missedReminders.length > 0) {
      // 使用第一个提醒的ID作为标识，避免重复播报
      const firstReminderId = missedReminders[0]?.id;
      
      // 如果是同一组提醒，不重复播报
      if (lastReminderIdRef.current === firstReminderId) {
        return;
      }
      
      // 记录本次提醒ID
      lastReminderIdRef.current = firstReminderId;
      
      // 弹窗打开时，延迟500ms后自动播报
      const timer = setTimeout(() => {
        const reminderTexts = missedReminders.map(reminder => {
          const drugName = reminder.drug || reminder.drugName || '未知药品';
          const time = reminder.time || reminder.scheduledTime || '未知时间';
          const dosage = reminder.dosage ? `，用量${reminder.dosage}` : '';
          return `${time}的${drugName}${dosage}`;
        });
        
        const speakText = `用药提醒！您有以下药物还没有服用：${reminderTexts.join('；')}。请及时服用。`;
        speak(speakText);
      }, 500);
      
      return () => clearTimeout(timer);
    }
  }, [showMedicationReminder, missedReminders]);

  const audioRef = useRef(null);

  const speak = async (text, rate = speechRate) => {
    if (!text || text.trim() === '') {
      console.log('没有可播放的内容');
      return;
    }

    // 先停止当前播放，避免中断错误
    stopSpeaking();

    // 优先尝试调用百度TTS API
    try {
      setIsSpeaking(true);

      // 将前端语速(0.6-1)映射到百度TTS语速(3-5)
      const baiduRate = rate === 0.6 ? 3 : 5;
      const response = await fetch(`/api/ai/tts?text=${encodeURIComponent(text)}&speechRate=${baiduRate}`);

      if (response.ok) {
        const result = await response.json();

        if (result.code === 200 && result.data) {
          // 播放百度返回的音频
          if (audioRef.current) {
            audioRef.current.src = result.data;
            // 添加错误处理，避免播放中断错误
            audioRef.current.play().catch(err => {
              console.error('音频播放失败:', err);
              setIsSpeaking(false);
            });
            return; // 成功则返回
          }
        }
      }

      // 如果百度TTS失败，使用浏览器原生语音（备用方案）
      console.warn('百度TTS不可用，使用浏览器原生语音');
      speakWithBrowser(text, rate);

    } catch (error) {
      console.error('百度TTS调用失败，使用备用方案:', error);
      speakWithBrowser(text, rate);
    }
  };

  // 浏览器原生语音（备用方案）
  const speakWithBrowser = (text, rate) => {
    if ('speechSynthesis' in window) {
      // 停止当前播放
      window.speechSynthesis.cancel();

      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'zh-CN';
      utterance.rate = rate;
      utterance.volume = 1;

      // 尝试选择最好的中文语音
      const voices = window.speechSynthesis.getVoices();
      const chineseVoice = voices.find(v => v.lang.includes('zh') && v.name.includes('Female'));
      if (chineseVoice) {
        utterance.voice = chineseVoice;
      }

      utterance.onend = () => setIsSpeaking(false);
      utterance.onerror = () => {
        console.error('浏览器语音播放失败');
        setIsSpeaking(false);
      };

      window.speechSynthesis.speak(utterance);
    } else {
      showToast('您的浏览器不支持语音播报功能', 'error');
      setIsSpeaking(false);
    }
  };

  const stopSpeaking = () => {
    // 停止百度TTS
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
    }
    // 停止浏览器语音
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsSpeaking(false);
  };

  // 监听音频播放结束
  useEffect(() => {
    const audio = audioRef.current;
    if (audio) {
      const handleEnded = () => setIsSpeaking(false);
      audio.addEventListener('ended', handleEnded);
      return () => audio.removeEventListener('ended', handleEnded);
    }
  }, []);

  // 监听activeTab变化，离开上传页面时清除图片
  useEffect(() => {
    if (activeTab !== 'upload') {
      // 离开上传页面时清除图片预览
      setImagePreview(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }

    // 切换到用药日历时，只在数据为空时才加载（避免每次切换都重新加载）
    if (activeTab === 'calendar' && isLoggedIn) {
      // 如果已经有数据，不重新加载，提升用户体验
      if (calendarViewMode === 'today' && calendarPlans.length === 0) {
        loadCalendarPlans();
      } else if (calendarViewMode === 'week' && !weeklyMedicationData) {
        loadWeeklyMedication();
      }
    }
  }, [activeTab, isLoggedIn]); // 移除calendarViewMode依赖，避免切换视图时重复触发

  const [ocrTaskId, setOcrTaskId] = useState(null);
  const [ocrPolling, setOcrPolling] = useState(false);
  const [elderlyGuide, setElderlyGuide] = useState(''); // 老年友好用药指导
  const [isLoadingGuide, setIsLoadingGuide] = useState(false); // 是否正在加载AI指导

  // 当selectedDrug变化时，自动调用AI生成老年友好指导
  useEffect(() => {
    if (selectedDrug && selectedDrug.name) {
      fetchElderlyGuide(selectedDrug);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedDrug?.name, selectedDrug?.usage, selectedDrug?.precautions]);

  // 调用AI生成老年友好用药指导（三层保障策略）
  const fetchElderlyGuide = async (drugInfo) => {
    if (!drugInfo || !drugInfo.name) return;
    
    setIsLoadingGuide(true);
    setElderlyGuide('');
    
    try {
      // 构造药品详细信息对象
      const drugDetail = {
        genericName: drugInfo.name,
        tradeName: drugInfo.tradeName || '',
        specification: drugInfo.spec || drugInfo.specification || '',
        manufacturer: drugInfo.manufacturer || '',
        category: drugInfo.category || '',
        ingredient: drugInfo.ingredient || '',
        indications: drugInfo.indications || '',
        usage: drugInfo.usage || drugInfo.dosage || '',
        precautions: drugInfo.precautions || '',
        adverseReactions: drugInfo.adverseReactions || '',
        description: drugInfo.description || ''
      };

      console.log('=== 尝试调用后端 AI 服务 ===');
      
      // 第一层：尝试调用后端 AI 服务
      const response = await fetch('/api/ai/elderly-guide', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(drugDetail)
      });

      const data = await response.json();
      
      if (data.code === 200 && data.data) {
        // 将<br/>标签替换为换行符，方便阅读
        const guideText = data.data.replace(/<br\/>/g, '\n');
        setElderlyGuide(guideText);
        console.log('✅ 后端 AI 服务成功:', guideText.substring(0, 100));
        return; // 成功则直接返回
      } else {
        console.error('❌ 后端 AI 服务失败:', data.message);
        throw new Error(data.message || '后端 AI 服务异常');
      }
    } catch (error) {
      console.error('️ 后端 AI 服务异常，尝试 DeepSeek 兜底:', error);
      
      // 第二层：使用 DeepSeek API 作为兜底
      try {
        await callDeepSeekForGuide(drugInfo);
        console.log('✅ DeepSeek 兜底成功');
        return;
      } catch (deepseekError) {
        console.error('❌ DeepSeek 兜底也失败，使用本地规则集:', deepseekError);
        
        // 第三层：使用本地扩充的规则集生成
        const fallbackGuide = generateFallbackGuide(drugInfo);
        setElderlyGuide(fallbackGuide);
        showToast('已使用本地用药指导（AI服务暂时不可用）', 'info');
      }
    } finally {
      setIsLoadingGuide(false);
    }
  };

  // DeepSeek 兜底函数
  const callDeepSeekForGuide = async (drugInfo) => {
    try {
      const prompt = `你是一位专业的药师，专门为老年人提供用药指导。请用通俗易懂、温暖亲切的语言，为一位老年患者解释以下药品的使用方法。

药品信息：
- 药品名称：${drugInfo.name}
- 商品名：${drugInfo.tradeName || '无'}
- 规格：${drugInfo.spec || drugInfo.specification || '无'}
- 生产厂家：${drugInfo.manufacturer || '无'}
- 药品类别：${drugInfo.category || '无'}
- 主要成分：${drugInfo.ingredient || '无'}
- 适应症：${drugInfo.indications || '无'}
- 用法用量：${drugInfo.usage || drugInfo.dosage || '无'}
- 注意事项：${drugInfo.precautions || '无'}
- 不良反应：${drugInfo.adverseReactions || '无'}
- 详细说明：${drugInfo.description || '无'}

请按照以下格式生成用药指导（每个部分都要有）：

1. 【这是什么药】：简单说明这个药是治什么病的
2. 【怎么吃】：详细说明用法用量，用老年人能听懂的话
3. 【什么时候吃】：说明最佳服用时间
4. 【特别要注意】：列出重要的注意事项和禁忌
5. 【可能出现的不舒服】：说明可能的不良反应，但要让老人不要过度担心
6. 【保存方法】：如何正确保存药品
7. 【温馨提醒】：给老人的贴心建议

要求：
- 语言要通俗易懂，避免专业术语
- 语气要温暖亲切，像跟家里长辈说话一样
- 重点内容要用强调的语气
- 适当使用表情符号增加亲和力
- 控制在500字以内
- 用中文回答`;

      console.log('正在调用 DeepSeek API...');
      
      const response = await fetch('/api/deepseek/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          messages: [{ role: 'user', content: prompt }],
          model: 'deepseek-chat',
          temperature: 0.7,
          max_tokens: 800
        })
      });

      const data = await response.json();
      
      if (data.choices && data.choices[0]) {
        const aiResponse = data.choices[0].message.content;
        setElderlyGuide(aiResponse);
        console.log('DeepSeek 生成用药指导成功，长度:', aiResponse.length);
      } else {
        throw new Error('DeepSeek API 返回异常');
      }
    } catch (error) {
      console.error('DeepSeek API 调用失败:', error);
      throw error; // 向上抛出错误，让外层捕获并使用本地规则集
    }
  };

  // 生成备用老年友好指导（当AI不可用时使用）
  // 采用三层保障策略：后端AI -> DeepSeek兜底 -> 本地规则集
  const generateFallbackGuide = (drugInfo) => {
    // ========== 配置区域 ==========
    
    // 1. 药品分类关键词映射
    const drugCategories = {
      '降压药': ['硝苯地平', '氨氯地平', '沙坦', '厄贝沙坦', '卡托普利', '缬沙坦', '氯沙坦'],
      '降糖药': ['二甲双', '格列美脲', '阿卡波糖', '胰岛素', '格列齐特', '罗格列酮'],
      '抗生素': ['阿莫西林', '头孢', '罗红霉素', '左氧氟沙星', '青霉素', '阿奇霉素'],
      '止痛药': ['布洛芬', '对乙酰氨基酚', '阿司匹林', '双氯芬酸', '塞来昔布'],
      '感冒药': ['感康', '白加黑', '泰诺', '999感冒灵', '快克', '新康泰克'],
      '胃药': ['奥美拉唑', '雷尼替丁', '铝碳酸镁', '多潘立酮', '泮托拉唑'],
      '安眠药': ['艾司唑仑', '佐匹克隆', '劳拉西泮', '地西泮'],
      '心脏病药': ['硝酸甘油', '速效救心丸', '地高辛', '单硝酸异山梨酯']
    };
    
    // 2. 服用时间关键词规则
    const timingRules = [
      { keywords: ['空腹', '饭前1小时', '清晨'], text: '建议在早上起床后空腹吃，这样吸收效果最好' },
      { keywords: ['饭前', '餐前', '吃饭前半小时'], text: '建议在吃饭前半个小时吃，这样药效会更好' },
      { keywords: ['饭后', '餐后', '吃完饭'], text: '建议在吃完饭半小时后吃，这样可以减少对胃的刺激' },
      { keywords: ['睡前', '临睡', '晚上睡觉前'], text: '建议在晚上睡觉前30分钟吃' },
      { keywords: ['晨起', '早晨'], text: '建议在早上起床后吃' },
      { keywords: ['中午', '午餐'], text: '建议在中午午饭后吃' }
    ];
    
    // 3. 禁忌事项关键词库（扩展版）
    const contraindications = {
      '饮酒': {
        keywords: ['酒', '酒精', '啤酒', '白酒', '红酒', '黄酒'],
        warning: '服药期间千万不要喝酒，包括啤酒、白酒、红酒等所有含酒精的饮料',
        severity: 'high'
      },
      '驾驶': {
        keywords: ['开车', '驾驶', '操作机械', '高空作业'],
        warning: '吃完药后最好不要开车或操作机器，因为可能会犯困或头晕',
        severity: 'high'
      },
      '孕妇禁用': {
        keywords: ['孕妇禁用', '妊娠禁用', '孕妇不宜'],
        warning: '如果您怀孕了，这个药千万不能吃，一定要告诉医生',
        severity: 'critical'
      },
      '哺乳期禁用': {
        keywords: ['哺乳期禁用', '哺乳禁用'],
        warning: '如果您正在喂奶，这个药不能吃，会影响宝宝',
        severity: 'critical'
      },
      '肝肾功能': {
        keywords: ['肝功能', '肾功能', '肝肾损害'],
        warning: '如果您的肝脏或肾脏不好，吃这个药要小心，最好先问问医生',
        severity: 'medium'
      },
      '过敏': {
        keywords: ['过敏', '过敏反应'],
        warning: '如果您对这种药或以前吃过类似的药有过敏反应，千万不能再吃',
        severity: 'critical'
      },
      '儿童禁用': {
        keywords: ['儿童禁用', '小儿禁用', '18岁以下禁用'],
        warning: '这个药不适合小孩子吃，如果是给孩子用药一定要咨询医生',
        severity: 'high'
      },
      '老人慎用': {
        keywords: ['老年', '老人慎用', '65岁以上'],
        warning: '年纪大了吃这个药要特别小心，剂量可能需要调整',
        severity: 'medium'
      },
      '相互作用': {
        keywords: ['药物相互作用', '不能同服', '避免合用'],
        warning: '这个药不能和其他某些药一起吃，如果您同时在吃别的药，一定要告诉医生',
        severity: 'high'
      }
    };
    
    // 4. 常见不良反应模板
    const commonSideEffects = {
      '胃肠道反应': ['恶心', '呕吐', '胃痛', '腹泻', '便秘', '腹胀'],
      '神经系统': ['头晕', '头痛', '嗜睡', '失眠', '乏力'],
      '皮肤反应': ['皮疹', '瘙痒', '红肿', '荨麻疹'],
      '心血管': ['心慌', '心跳加快', '血压变化'],
      '其他': ['口干', '口苦', '食欲下降', '体重变化']
    };
    
    // ========== 生成逻辑 ==========
    
    const usage = drugInfo.usage || drugInfo.dosage || '';
    const precautions = drugInfo.precautions || '';
    const adverseReactions = drugInfo.adverseReactions || '';
    const indications = drugInfo.indications || '';
    const category = drugInfo.category || '';
    
    let guide = `您好，您查询的药品是【${drugInfo.name}】。\n\n`;
    
    // 1. 药品用途说明
    if (indications) {
      guide += `【治什么病】：${indications}\n\n`;
    } else if (category) {
      guide += `【药品类型】：这是${category}类药物\n\n`;
    }
    
    // 2. 用法用量
    if (usage) {
      guide += `【怎么吃】：${usage}\n\n`;
    } else {
      guide += `【怎么吃】：请按照医生说的剂量来吃，不要自己随便增减\n\n`;
    }
    
    // 3. 服用时间智能判断
    let timingFound = false;
    for (const rule of timingRules) {
      if (rule.keywords.some(kw => usage.includes(kw))) {
        guide += `【什么时候吃】：${rule.text}\n\n`;
        timingFound = true;
        break;
      }
    }
    if (!timingFound) {
      guide += `【什么时候吃】：按照医生说的时间吃就好，一般是早中晚各一次\n\n`;
    }
    
    // 4. 检测药品类别并给出针对性建议
    let detectedCategory = null;
    for (const [catName, keywords] of Object.entries(drugCategories)) {
      if (keywords.some(kw => drugInfo.name.includes(kw) || (drugInfo.genericName && drugInfo.genericName.includes(kw)))) {
        detectedCategory = catName;
        break;
      }
    }
    
    if (detectedCategory) {
      guide += `【药品类别】：这是${detectedCategory}\n\n`;
      
      // 根据类别给出特殊提醒
      switch(detectedCategory) {
        case '降压药':
          guide += `【特别提醒】：降压药要坚持每天吃，不能随便停药。最好在固定时间吃，比如每天早上起床后。记得定期量血压哦。\n\n`;
          break;
        case '降糖药':
          guide += `【特别提醒】：降糖药一定要按时吃，特别是饭前吃的药，要在吃饭前半小时就准备好。平时要注意监测血糖。\n\n`;
          break;
        case '抗生素':
          guide += `【特别提醒】：抗生素要吃够疗程，即使症状好了也不能提前停药，否则容易复发。一般要吃5-7天。\n\n`;
          break;
        case '止痛药':
          guide += `【特别提醒】：止痛药不要长期大量吃，连续吃超过3天就要去看医生了。饭后吃可以减少对胃的刺激。\n\n`;
          break;
        case '安眠药':
          guide += `【特别提醒】：安眠药要在睡前30分钟吃，吃了之后就不要再做别的事情了，直接准备睡觉。第二天早上起来可能会觉得有点晕，这是正常的。\n\n`;
          break;
        case '心脏病药':
          guide += `【特别提醒】：心脏病的药一定要随身携带，特别是硝酸甘油这类急救药。如果出现胸痛胸闷要立即含服。\n\n`;
          break;
      }
    }
    
    // 5. 注意事项和禁忌（扩展版）
    const importantWarnings = [];
    const mediumWarnings = [];
    const criticalWarnings = [];
    
    for (const [key, data] of Object.entries(contraindications)) {
      if (data.keywords.some(kw => precautions.includes(kw) || indications.includes(kw))) {
        if (data.severity === 'critical') {
          criticalWarnings.push(data.warning);
        } else if (data.severity === 'high') {
          importantWarnings.push(data.warning);
        } else {
          mediumWarnings.push(data.warning);
        }
      }
    }
    
    if (criticalWarnings.length > 0) {
      guide += `️ 【重要警告】：\n`;
      criticalWarnings.forEach(w => guide += `• ${w}\n`);
      guide += `\n`;
    }
    
    if (importantWarnings.length > 0) {
      guide += `【特别注意】：\n`;
      importantWarnings.forEach(w => guide += `• ${w}\n`);
      guide += `\n`;
    }
    
    if (mediumWarnings.length > 0) {
      guide += `【温馨提示】：\n`;
      mediumWarnings.forEach(w => guide += `• ${w}\n`);
      guide += `\n`;
    }
    
    // 6. 不良反应说明（更友好）
    if (adverseReactions && adverseReactions !== '暂无详细信息') {
      guide += `【可能出现的不舒服】：\n`;
      
      // 尝试匹配常见不良反应
      let matchedEffects = [];
      for (const [catName, effects] of Object.entries(commonSideEffects)) {
        const found = effects.filter(effect => adverseReactions.includes(effect));
        if (found.length > 0) {
          matchedEffects.push({ category: catName, effects: found });
        }
      }
      
      if (matchedEffects.length > 0) {
        matchedEffects.forEach(({ category, effects }) => {
          guide += `• ${category}：可能会有${effects.join('、')}等情况\n`;
        });
      } else {
        guide += `${adverseReactions}\n`;
      }
      
      guide += `\n如果出现这些情况不要太紧张，大多数都是轻微的。但如果感觉很难受或者持续不好转，一定要去找医生看看。\n\n`;
    }
    
    // 7. 保存和丢弃提醒
    guide += `【保存方法】：放在阴凉干燥的地方，避免阳光直射。注意看有效期，过期的药就不能吃了。\n\n`;
    
    // 8. 结尾关怀语
    guide += `【最后提醒您】：\n`;
    guide += `• 一定要按照医生说的剂量和时间来吃药\n`;
    guide += `• 不要自己随便停药或换药\n`;
    guide += `• 如果同时吃好几种药，要问清楚能不能一起吃\n`;
    guide += `• 吃药后感觉不舒服要及时告诉家人或医生\n`;
    guide += `• 定期去医院复查，让医生看看药效怎么样\n\n`;
    
    guide += `祝您早日康复，身体健康！🙏`;
    
    return guide;
  };

  // 将图片转换为JPEG格式
  const convertToJpeg = (file) => {
    return new Promise((resolve) => {
      console.log('=== 图片转换 ===');
      console.log('原始文件名:', file.name);
      console.log('原始文件类型:', file.type);
      console.log('原始文件大小:', file.size);
      
      if (!file.type.startsWith('image/')) {
        console.log('不是图片类型，直接返回');
        resolve(file);
        return;
      }

      // 如果不是WebP格式，直接返回
      if (!file.type.includes('webp') && !file.name.toLowerCase().endsWith('.webp')) {
        console.log('不是WebP格式，直接返回');
        resolve(file);
        return;
      }

      console.log('开始转换WebP到JPEG...');
      
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          const canvas = document.createElement('canvas');
          canvas.width = img.width;
          canvas.height = img.height;
          const ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0);
          
          canvas.toBlob((blob) => {
            if (blob) {
              const jpegFile = new File([blob], file.name.replace(/\.webp$/i, '.jpg'), { type: 'image/jpeg' });
              console.log('转换成功，新文件大小:', jpegFile.size);
              resolve(jpegFile);
            } else {
              console.log('转换失败，返回原始文件');
              resolve(file);
            }
          }, 'image/jpeg', 0.9);
        };
        img.onerror = () => {
          console.log('图片加载失败，返回原始文件');
          resolve(file);
        };
        img.src = e.target.result;
      };
      reader.onerror = () => {
        console.log('FileReader失败，返回原始文件');
        resolve(file);
      };
      reader.readAsDataURL(file);
    });
  };

  // 触发批量选择文件
  const handleBatchSelectFiles = () => {
    batchFileInputRef.current?.click();
  };

  // 处理批量文件选择
  const handleBatchFileSelect = async (e) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    // 将WebP图片转换为JPEG格式
    const convertedFiles = await Promise.all(files.map(async (file) => {
      if (!file.type.includes('webp')) return file;
      
      return new Promise((resolve) => {
        const reader = new FileReader();
        reader.onload = (evt) => {
          const img = new Image();
          img.onload = () => {
            const canvas = document.createElement('canvas');
            canvas.width = img.width;
            canvas.height = img.height;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);
            canvas.toBlob((blob) => {
              if (blob) {
                const converted = new File([blob], file.name.replace('.webp', '.jpg'), { type: 'image/jpeg' });
                resolve(converted);
              } else {
                resolve(file);
              }
            }, 'image/jpeg', 0.95);
          };
          img.onerror = () => resolve(file);
          img.src = evt.target.result;
        };
        reader.onerror = () => resolve(file);
        reader.readAsDataURL(file);
      });
    }));

    // 创建预览URL
    const newItems = convertedFiles.map((file, index) => ({
      id: Date.now() + index,
      file: file,
      previewUrl: URL.createObjectURL(file),
      status: 'pending',
      result: null
    }));

    setBatchRecognizeItems(prev => [...prev, ...newItems]);
    e.target.value = '';
  };

  // 批量识别所有图片
  const handleBatchRecognize = async () => {
    if (batchRecognizeItems.length === 0) {
      showToast('请先选择要识别的图片', 'warning');
      return;
    }

    // 更新状态为识别中
    setBatchRecognizeItems(prev => prev.map(item => ({ ...item, status: 'recognizing' })));

    try {
      const files = batchRecognizeItems.map(item => item.file);
      const formData = new FormData();
      files.forEach(file => formData.append('files', file));

      const response = await fetch('/api/v1/drug/recognize/batch-upload', {
        method: 'POST',
        headers: { 'X-User-Id': user?.userId || '1' },
        body: formData
      });

      const data = await response.json();

      if (data.code === 200 && data.data) {
        const results = data.data.items || [];

        // 更新图片状态和结果
        setBatchRecognizeItems(prev => prev.map((item, index) => {
          const result = results[index] || {};
          return {
            ...item,
            status: result.status === 'matched' ? 'success' : 'failed',
            result: {
              drugId: result.matchedDrugId,
              drugName: result.matchedDrugName,
              rawText: result.rawText,
              matchScore: result.matchScore,
              message: result.message
            }
          };
        }));

        // 自动选中识别成功的项目
        const newSelected = new Set();
        const matchedDrugs = [];
        batchRecognizeItems.forEach((item, index) => {
          const result = results[index] || {};
          if (result.status === 'matched' && result.matchedDrugId) {
            newSelected.add(item.id);
            matchedDrugs.push({
              id: result.matchedDrugId,
              name: result.matchedDrugName,
              spec: result.matchedDrugSpec || '',
              manufacturer: '',
              matchScore: result.matchScore ? Math.round(result.matchScore * 100) : 0
            });
          }
        });
        setBatchSelectedForAdd(newSelected);

        // 同步命中药品到识别结果列表，并跳转到识别结果页查看
        if (matchedDrugs.length > 0) {
          setRecognizedDrugs(matchedDrugs);
          setActiveTab('recognition');
        }

        showToast(`识别完成！成功: ${data.data.successCount}, 失败: ${data.data.failedCount}`,
          data.data.failedCount > 0 ? 'warning' : 'success');
      } else {
        showToast(data.message || '批量识别失败', 'error');
        setBatchRecognizeItems(prev => prev.map(item => ({ ...item, status: 'pending' })));
      }
    } catch (error) {
      console.error('批量识别失败:', error);
      showToast('批量识别失败，请检查网络连接', 'error');
      setBatchRecognizeItems(prev => prev.map(item => ({ ...item, status: 'pending' })));
    }
  };

  // 批量添加到药箱
  const handleBatchAddToMedicineBox = async () => {
    if (batchSelectedForAdd.size === 0) {
      showToast('请选择要添加到药箱的药品', 'warning');
      return;
    }

    setIsBatchAdding(true);

    try {
      const selectedItems = batchRecognizeItems.filter(item => batchSelectedForAdd.has(item.id) && item.result?.drugId);
      let successCount = 0;
      let failCount = 0;

      for (const item of selectedItems) {
        try {
          const response = await fetch(`/api/v1/box?userId=${user?.userId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              drugId: item.result.drugId,
              dosage: '1片',
              frequency: '每日一次',
              startDate: new Date().toISOString().split('T')[0],
              endDate: new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
              expiryDate: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
              totalQuantity: 30,
              status: 'active'
            })
          });

          const data = await response.json();
          if (data.code === 200) {
            successCount++;
          } else {
            failCount++;
          }
        } catch (err) {
          console.error('添加到药箱失败:', err);
          failCount++;
        }
      }

      if (successCount > 0) {
        showToast(`成功添加 ${successCount} 个药品到药箱`, 'success');
        loadMedicineBoxList(user?.userId);
        // 清空批量识别列表
        batchRecognizeItems.forEach(item => URL.revokeObjectURL(item.previewUrl));
        setBatchRecognizeItems([]);
        setBatchSelectedForAdd(new Set());
      } else if (failCount > 0) {
        showToast(`添加失败 ${failCount} 个药品`, 'error');
      }
    } finally {
      setIsBatchAdding(false);
    }
  };

  const analyzeImage = async () => {
    console.log('=== 分析图片 ===');
    console.log('fileInputRef.current:', fileInputRef.current);
    console.log('fileInputRef.current?.files:', fileInputRef.current?.files);
    console.log('fileInputRef.current?.files[0]:', fileInputRef.current?.files[0]);
    
    if (!fileInputRef.current?.files[0]) {
      showToast('请先选择图片', 'warning');
      return;
    }

    const file = fileInputRef.current.files[0];
    const MAX_SIZE = 10 * 1024 * 1024; // 10MB
    const MIN_PX = 15;
    const MAX_PX = 4096;

    if (file.size === 0) {
      showToast('上传文件为空，请选择图片后再上传', 'error');
      return;
    }

    if (file.size > MAX_SIZE) {
      showToast('图片大小不能超过 10MB', 'error');
      return;
    }

    let imgUrl = null;
    try {
      imgUrl = URL.createObjectURL(file);
      const imgObj = new Image();
      const sizeOk = await new Promise((resolve) => {
        imgObj.onload = () => {
          if (imgObj.width < MIN_PX || imgObj.height < MIN_PX) {
            showToast('图片太小，无法识别（最小 15×15 像素）', 'error');
            resolve(false);
          } else if (imgObj.width > MAX_PX || imgObj.height > MAX_PX) {
            showToast('图片尺寸过大，请使用不超过 4096×4096 像素的图片', 'error');
            resolve(false);
          } else {
            resolve(true);
          }
        };
        imgObj.onerror = () => {
          showToast('无法读取图片，请换一张试试', 'error');
          resolve(false);
        };
        imgObj.src = imgUrl;
      });
      if (!sizeOk) return;
    } catch (e) {
      console.error('图片校验失败', e);
      showToast('图片校验失败，请换一张试试', 'error');
      setIsLoading(false);
      return;
    } finally {
      if (imgUrl) {
        URL.revokeObjectURL(imgUrl);
      }
    }

    setIsLoading(true);
    setOcrTaskId(null);

    try {
      // 将WebP图片转换为JPEG格式
      const convertedFile = await convertToJpeg(fileInputRef.current.files[0]);
      
      console.log('=== 准备上传 ===');
      console.log('文件名:', convertedFile.name);
      console.log('文件类型:', convertedFile.type);
      console.log('文件大小:', convertedFile.size);
      
      const formData = new FormData();
      formData.append('file', convertedFile);
      
      // 调试：检查FormData内容
      console.log('FormData内容:');
      for (let pair of formData.entries()) {
        console.log('  键:', pair[0], ', 值:', pair[1]);
      }

      // 不设置Content-Type，让浏览器自动处理
      // 通过代理转发到后端
      const response = await fetch('/api/v1/drug/recognize/upload', {
        method: 'POST',
        headers: {
          'X-User-Id': user?.userId || '1'
          // 注意：不要设置Content-Type，浏览器会自动设置multipart/form-data及boundary
        },
        body: formData
      });
      
      // 调试：查看实际发送的请求头
      console.log('响应状态:', response.status);
      console.log('响应头:', [...response.headers.entries()]);

      const data = await response.json();

      console.log('=== 上传响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);

      if (data.code === 200 && data.data?.taskId) {
        setOcrTaskId(data.data.taskId);
        pollOcrResult(data.data.taskId);
      } else {
        showToast(data.message || '上传失败', 'error');
        setIsLoading(false);
      }
    } catch (error) {
      console.error('=== 上传失败 ===', error);
      showToast('上传失败，请检查网络连接', 'error');
      setIsLoading(false);
    }
  };

  const pollOcrResult = async (taskId) => {
    setOcrPolling(true);
    let pollingCount = 0;
    const maxPollingCount = 30;

    const poll = async () => {
      try {
        const response = await fetch(`/api/v1/drug/recognize/result/${taskId}`);
        const data = await response.json();

        console.log('查询结果:', data);

        if (data.code === 200 && data.data) {
          const result = data.data;
          
          if (result.status === 'matched' || result.status === 'unmatched' || result.status === 'failed') {
            setOcrPolling(false);
            
            if (result.status === 'matched' && result.matchedDrugName) {
              const drug = {
                id: Date.now(),
                name: result.matchedDrugName,
                spec: result.matchedDrugSpec || '',
                manufacturer: '',
                matchScore: result.matchScore ? Math.round(result.matchScore * 100) : 0
              };
              setRecognizedDrugs([drug]);
              
              // 显示完成状态
              setShowComplete(true);
              
              // 延迟跳转，让用户看到完成动画
              setTimeout(() => {
                // 调用统一的药品信息获取函数
                fetchDrugDetail(result.matchedDrugName, drug, {
                  showLoading: false,
                  onComplete: () => {
                    // 只有在成功跳转到说明页面后，才清除加载状态
                    setIsLoading(false);
                    setShowComplete(false);
                    console.log('药品信息获取完成，已跳转到说明页面');
                  }
                });
              }, 1500);
            } else if (result.status === 'unmatched') {
              setIsLoading(false);
              showToast('未能识别出匹配的药品，请尝试手动输入', 'warning');
            } else if (result.status === 'failed') {
              setIsLoading(false);
              const detail = result.rawText && result.rawText.trim() ? result.rawText : '识别失败，请重试';
              showToast(detail, 'error');
            }
          } else if (pollingCount < maxPollingCount) {
            pollingCount++;
            setTimeout(poll, 1000);
          } else {
            setOcrPolling(false);
            setIsLoading(false);
            showToast('识别超时，请重试', 'error');
          }
        } else {
          setOcrPolling(false);
          setIsLoading(false);
          showToast(data.message || '查询失败', 'error');
        }
      } catch (error) {
        console.error('查询失败:', error);
        setOcrPolling(false);
        setIsLoading(false);
        showToast('查询失败，请检查网络连接', 'error');
      }
    };

    poll();
  };

  const addToMedicineBox = async (drug) => {
    if (!user || !user.userId) {
      showToast('请先登录', 'warning');
      return;
    }

    try {
      // 先查询药品基础库，获取药品ID和详细信息
      const searchResponse = await fetch(`/api/v1/drug/list?keyword=${encodeURIComponent(drug.name)}`);
      const searchData = await searchResponse.json();
      
      let drugId = null;
      let drugDetail = null;
      if (searchData.code === 200 && searchData.data && searchData.data.length > 0) {
        drugId = searchData.data[0].id;
        drugDetail = searchData.data[0];
      }

      // 准备药品信息，用于预填充弹窗
      // 优先使用数据库中的信息，如果数据库没有则使用AI搜索或手动输入的信息
      const drugInfo = {
        drugId: drugId,  // 如果为null，后端需要处理
        name: drug.name || drugDetail?.drugName,
        genericName: drugDetail?.drugName || drug.genericName || drug.name,
        specification: drugDetail?.specification || drug.spec || '',
        frequency: drug.frequency || drugDetail?.frequency || '',
        dosage: drug.dosage || drugDetail?.dosage || '',
        usage: drug.usage || drugDetail?.usage || '',
        totalQuantity: drug.totalQuantity || 30,
        manufacturer: drug.manufacturer || drugDetail?.manufacturer || ''
      };

      // 弹出确认弹窗
      setPendingDrugInfo(drugInfo);
      setShowConfirmDrugModal(true);
    } catch (error) {
      console.error('查询药品信息失败:', error);
      showToast('添加失败，请稍后重试', 'error');
    }
  };

  // 确认添加药品到药箱
  const confirmAddToMedicineBox = async (drugData) => {
    // 关闭确认弹窗
    setShowConfirmDrugModal(false);
    
    // 显示正在添加的反馈
    showToast('正在添加药品...', 'info');
    
    try {
      const addResponse = await fetch(`/api/v1/box?userId=${user.userId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(drugData)
      });

      const addData = await addResponse.json();

      if (addResponse.ok && addData.code === 200) {
        // 添加成功后重新加载药箱列表
        await loadMedicineBoxList(user.userId);
        showToast('药品已加入药箱！正在检测冲突...', 'success');

        // 等待状态更新后获取最新药箱列表
        // 使用 setTimeout 确保状态已更新
        await new Promise(resolve => setTimeout(resolve, 100));
        
        // 新药入箱后自动触发冲突检测（传入当前药箱列表）
        const conflictResult = await checkConflictsForNewDrug(drugData.name, drugList);
        
        if (conflictResult) {
          if (conflictResult.noConflict) {
            // 没有冲突，根据原因显示不同提示
            if (conflictResult.reason === 'empty' || conflictResult.reason === 'firstDrug') {
              showToast('药品已加入药箱！药箱中暂无其他药品', 'success');
            } else {
              showToast('药品已加入药箱！未检测到冲突', 'success');
            }
          } else {
            // 检测到冲突
            showToast('检测到冲突！', 'warning');
            setConflictAlertResult(conflictResult);
            setShowConflictAlert(true);
          }
        }
        
        // 标记冲突检测页面需要重新检测
        setConflictNeedsRecheck(true);
        setConflictReport(null); // 清除之前的检测结果
      } else {
        showToast(addData.message || '添加失败，请重试', 'error');
      }
    } catch (error) {
      showToast('添加失败，请稍后重试', 'error');
      console.error('添加药品失败:', error);
    }
  };

  const parseDosageToNumber = (dosageStr) => {
    if (!dosageStr) return 1;
    
    const match = dosageStr.match(/(\d+\.?\d*)/);
    if (match) {
      return parseInt(match[1], 10) || 1;
    }
    
    return 1;
  };

  const markAsTaken = async (id, event) => {
    if (event) {
      const rect = event.target.getBoundingClientRect();
      const x = rect.left + rect.width / 2;
      const y = rect.top + rect.height / 2;
      const newParticles = Array.from({ length: 12 }, (_, i) => ({
        id: Date.now() + i,
        x,
        y,
        color: i % 2 === 0 ? 'var(--mint-green)' : 'var(--tech-blue)',
        tx: (Math.random() - 0.5) * 200,
        ty: (Math.random() - 0.5) * 200 - 100
      }));
      setParticles(newParticles);
      setTimeout(() => setParticles([]), 1000);
      setTakenButtons(prev => ({ ...prev, [id]: true }));
    }

    // 取出目标 plan（提前，避免在 setState 闭包里取不到更新后的 calendarPlans）
    const targetPlan = calendarPlans.find(r => r.id === id);
    const targetPlanId = targetPlan?.planId;
    const targetDosage = targetPlan?.dosage;

    // 乐观更新本地 state
    setReminders(reminders.map(r =>
      r.id === id ? { ...r, taken: true, missed: false } : r
    ));
    setCalendarPlans(calendarPlans.map(r => {
      if (r.id === id) {
        saveLocalMedicationStatus(id, 'taken');
        if (r.planId) {
          saveLocalMedicationStatus(r.planId, 'taken');
        }
        if (r.boxItemId && r.remainingQuantity !== undefined) {
          const amount = parseDosageToNumber(r.dosage);
          const newRemaining = Math.max(0, (r.remainingQuantity || 0) - amount);
          return { ...r, taken: true, missed: false, remainingQuantity: newRemaining };
        }
        return { ...r, taken: true, missed: false };
      }
      // 同一药品的其他时间段同步扣减库存
      if (r.boxItemId && targetPlan && r.boxItemId === targetPlan.boxItemId) {
        const amount = parseDosageToNumber(targetPlan.dosage);
        const newRemaining = Math.max(0, (r.remainingQuantity || 0) - amount);
        return { ...r, remainingQuantity: newRemaining };
      }
      return r;
    }));

    setShowCelebration(true);
    setTimeout(() => setShowCelebration(false), 2500);

    // 等后端真正接受这次操作，再用后端数据做一次校验，避免本地乐观更新和后端脱节
    if (targetPlanId && user?.userId) {
      try {
        await executeMedicationActionWithAPI(targetPlanId, user.userId, 'confirm', targetDosage);
      } catch (err) {
        // executeMedicationActionWithAPI 内部已经 toast 报错；继续 reload 让 UI 与后端对齐
        console.error('确认服药 API 调用异常:', err);
      }
    }

    // API 已落定，删掉 localStorage 里这条临时记录，让后端成为唯一真相源
    // 不论成功失败都清：成功时没必要再缓存，失败时也要避免下次刷新把旧乐观值覆盖回来
    if (targetPlanId) {
      saveLocalMedicationStatus(targetPlanId, null);
    }

    // 不论成功失败都拉后端做最终校验：今日 + 一周都刷
    // 成功时：本地就是 taken、后端也是 completed，状态对齐
    // 失败时：后端仍是 pending，reload 后 UI 自动回退到"待吃"，消除本地和后端的不一致
    await Promise.all([
      loadCalendarPlans(),
      typeof loadWeeklyMedication === 'function' ? loadWeeklyMedication() : Promise.resolve()
    ]);
  };

  // 调用后端统一幂等用药操作接口
  const executeMedicationActionWithAPI = async (planId, userId, action, dosage = '') => {
    if (!userId) {
      return { success: false, error: '用户未登录' };
    }
    try {
      const response = await fetch(`/api/v1/plan/${planId}/action`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId,
          action
        })
      });

      const result = await response.json();

      if (!response.ok || result.code !== 200) {
        const errorMsg = result.message || '操作失败';
        // 显示失败 Toast 提示
        showToast(errorMsg, 'error');
        return { success: false, error: errorMsg };
      }

      return { success: true, data: result.data };
    } catch (err) {
      const errorMsg = '网络错误，请检查连接';
      showToast(errorMsg, 'error');
      return { success: false, error: errorMsg };
    }
  };

  // 周视图：把 weeklyMedicationData 中某条 item 的状态按 action 乐观更新
  const patchWeeklyItemStatus = (date, planId, nextStatus) => {
    setWeeklyMedicationData(prev => {
      if (!prev || !prev.dailyRecords) return prev;
      return {
        ...prev,
        dailyRecords: prev.dailyRecords.map(day => {
          if (day.date !== date) return day;
          return {
            ...day,
            items: day.items.map(item =>
              item.planId === planId ? { ...item, status: nextStatus } : item
            )
          };
        })
      };
    });
  };

  // 周视图：补打 / 我吃了（confirm 动作）
  const handleWeekItemConfirm = async (date, item) => {
    if (!item || !item.planId) {
      showToast('该计划无法补打', 'warning');
      return;
    }
    if (!user || !user.userId) {
      showToast('请先登录', 'warning');
      return;
    }
    // 乐观更新：confirm 之后一定是 taken
    patchWeeklyItemStatus(date, item.planId, 'taken');
    const result = await executeMedicationActionWithAPI(item.planId, user.userId, 'confirm');
    if (!result.success) {
      // 回滚：重新拉一次
      loadWeeklyMedication();
    }
  };

  // 周视图：撤销（undo 动作）
  // 撤销后的状态后端说了算（pending 或 missed），不在前端猜
  const handleWeekItemUndo = async (date, item) => {
    if (!item || !item.planId) {
      showToast('该计划无法撤销', 'warning');
      return;
    }
    if (!user || !user.userId) {
      showToast('请先登录', 'warning');
      return;
    }
    const result = await executeMedicationActionWithAPI(item.planId, user.userId, 'undo');
    if (result.success) {
      // 重拉当周数据，让后端的真实状态覆盖本地
      loadWeeklyMedication();
      // 如果撤销的是今天那一格，顺便刷新今日视图数据
      const todayKey = toDateKey(new Date());
      if (date === todayKey) {
        loadCalendarPlans();
      }
    }
  };

  // 周视图：切换某天就地展开
  const handleWeekDayToggle = (date) => {
    setSelectedWeekDay(prev => (prev === date ? null : date));
  };

  // 更新药箱剩余数量（仅用于乐观更新 UI，实际由后端处理）
  const updateMedicineBoxQuantity = async (boxItemId, currentRemaining, dosage, isRestore = false) => {
    if (!boxItemId || !user || !user.userId) {
      console.warn('缺少必要参数，无法更新药箱库存');
      return false;
    }

    const amount = parseDosageToNumber(dosage);
    const newRemaining = isRestore 
      ? (currentRemaining || 0) + amount  // 恢复时增加数量
      : Math.max(0, (currentRemaining || 0) - amount);  // 正常时减少数量

    // 仅更新前端 UI 状态，不再单独调用后端（后端统一处理）
    setDrugList(prevList => prevList.map(drug => 
      drug.boxItemId === boxItemId 
        ? { ...drug, remaining: newRemaining } 
        : drug
    ));
    
    return true;
  };

  const undoMarkAsTaken = (id) => {
    setPendingUndoId(id);
    setShowUndoConfirmModal(true);
  };

  const confirmUndo = async () => {
    const id = pendingUndoId;
    if (!id) return;

    setShowUndoConfirmModal(false);
    setPendingUndoId(null);

    // 取出目标 plan（提前，避免在 setState 闭包里取不到更新后的 calendarPlans）
    const targetPlan = calendarPlans.find(r => r.id === id);
    const targetPlanId = targetPlan?.planId;
    const targetDosage = targetPlan?.dosage;

    // 乐观更新本地 state
    setReminders(reminders.map(r =>
      r.id === id ? { ...r, taken: false } : r
    ));
    setCalendarPlans(calendarPlans.map(r => {
      if (r.id === id) {
        if (r.boxItemId && r.remainingQuantity !== undefined) {
          const amount = parseDosageToNumber(r.dosage);
          const newRemaining = (r.remainingQuantity || 0) + amount;
          updateMedicineBoxQuantity(r.boxItemId, r.remainingQuantity, r.dosage, true);
          return { ...r, taken: false, remainingQuantity: newRemaining };
        }
        return { ...r, taken: false };
      }
      if (r.boxItemId && targetPlan && r.boxItemId === targetPlan.boxItemId) {
        const amount = parseDosageToNumber(targetPlan.dosage);
        const newRemaining = (r.remainingQuantity || 0) + amount;
        return { ...r, remainingQuantity: newRemaining };
      }
      return r;
    }));

    setTakenButtons(prev => {
      const newState = { ...prev };
      delete newState[id];
      return newState;
    });

    // 等后端真正接受这次撤销操作
    if (targetPlanId && user?.userId) {
      try {
        await executeMedicationActionWithAPI(targetPlanId, user.userId, 'undo', targetDosage);
      } catch (err) {
        console.error('撤销服药 API 调用异常:', err);
      }
    }

    // 清掉 localStorage + reload 两个视图，让后端做最终校验
    if (targetPlanId) {
      saveLocalMedicationStatus(targetPlanId, null);
    }
    await Promise.all([
      loadCalendarPlans(),
      typeof loadWeeklyMedication === 'function' ? loadWeeklyMedication() : Promise.resolve()
    ]);
  };

  const takenCount = calendarPlans.filter(r => r.taken).length;
  const totalCount = calendarPlans.length;
  const progressPercent = totalCount > 0 ? (takenCount / totalCount) * 440 : 0;
  const isFullProgress = takenCount === totalCount && totalCount > 0;

  const renderHeader = () => (
    <header className="header">
      <div className="header-content">
        <div className="header-left">
          <span className="header-logo">💊</span>
          <div className="header-brand">
            <h1 className="header-title">AI 药管家</h1>
            <p className="header-subtitle">您身边贴心的用药安全小助手</p>
          </div>
        </div>
        <div className="header-right">
          <div className="user-actions">
            <button className="header-btn profile-btn" onClick={() => setShowProfileEdit(true)}>
              <span className="btn-icon">👤</span>
              <span className="btn-label">个人档案</span>
            </button>
            <button className="header-btn contact-btn" onClick={() => setShowAddContact(true)}>
              <span className="btn-icon">📞</span>
              <span className="btn-label">紧急联系人</span>
            </button>
          </div>
          <span className="virtual-pharmacist">👨‍⚕️</span>
          <div className="user-greeting">
            <p className="user-name">您好，{user?.realName || user?.username || '用户'}！</p>
            <button className="logout-btn" onClick={handleLogout}>退出登录</button>
          </div>
        </div>
      </div>
    </header>
  );

  const renderHomeTab = () => (
    <div>
      <div className="dashboard-grid">
        <div className="dashboard-card" onClick={() => setActiveTab('upload')}>
          <div className="upload-card-icon">
            <span>📷</span>
          </div>
          <h3 className="dashboard-card-title">查药品</h3>
          <div className="dashboard-card-desc">
            <p>上传药盒照片，AI帮您识别</p>
            <button className="btn btn-primary btn-large" style={{ marginTop: '20px', width: '100%' }}>
              📷 上传药盒照片
            </button>
          </div>
        </div>

        <div className="dashboard-card" onClick={() => setActiveTab('calendar')}>
          <h3 className="dashboard-card-title">今日用药</h3>
          <div className="dashboard-card-desc">
            {totalCount > 0 ? (
              <div className="progress-ring-container">
                <svg className="progress-ring" viewBox="0 0 180 180">
                  <defs>
                    <linearGradient id="progressGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#4A90E2" />
                      <stop offset="100%" stopColor="#98D4BB" />
                    </linearGradient>
                  </defs>
                  <circle className="progress-ring-circle-bg" cx="90" cy="90" r="80" />
                  {isFullProgress ? (
                    <circle
                      className="progress-ring-circle full"
                      cx="90" cy="90" r="80"
                    />
                  ) : (
                    <circle
                      className="progress-ring-circle"
                      cx="90" cy="90" r="80"
                      strokeDasharray={`${progressPercent} 440`}
                    />
                  )}
                </svg>
                <div className="progress-ring-text">
                  <div className="progress-ring-value">{takenCount}/{totalCount}</div>
                  <div className="progress-ring-label">已完成</div>
                </div>
              </div>
            ) : (
              <div className="empty-plan-hint">
                <div className="empty-plan-icon">💊</div>
                <div className="empty-plan-text">暂无用药计划</div>
                <div className="empty-plan-subtext">请先添加药品到药箱</div>
              </div>
            )}
          </div>
        </div>

        <div className="dashboard-card" onClick={() => setActiveTab('drugs')}>
          <h3 className="dashboard-card-title">我的药箱</h3>
          <div className="dashboard-card-desc">
            <div className="medicine-box-stats">
              <div className="medicine-box-count">{drugList.length}</div>
              <div className="medicine-box-unit">种药品</div>
            </div>
            {(() => {
              const { expiredDrugs, expiringDrugs } = expiringDrugsResult;
              const expiredCount = expiredDrugs.length;
              const expiringCount = expiringDrugs.length;
              const totalExpiring = expiredCount + expiringCount;
              
              if (totalExpiring > 0) {
                // 生成提示文本
                let alertText = '⚠️ ';
                if (expiringCount > 0 && expiredCount > 0) {
                  alertText += `${expiringCount}盒药品即将过期，${expiredCount}盒药品已过期，点击处理`;
                } else if (expiringCount > 0) {
                  alertText += `${expiringCount}盒药品即将过期，点击处理`;
                } else {
                  alertText += `${expiredCount}盒药品已过期，点击处理`;
                }
                
                const isAllExpired = totalExpiring === expiredCount;
                return (
                  <button
                    className="btn"
                    style={{
                      marginTop: '20px',
                      width: '100%',
                      padding: '14px 16px',
                      fontSize: '15px',
                      fontWeight: 'bold',
                      background: isAllExpired
                        ? 'linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)'
                        : 'linear-gradient(135deg, #f39c12 0%, #e67e22 100%)',
                      color: 'white',
                      border: 'none',
                      borderRadius: '10px',
                      boxShadow: isAllExpired
                        ? '0 4px 15px rgba(231, 76, 60, 0.4)'
                        : '0 4px 15px rgba(243, 156, 18, 0.4)',
                      animation: 'pulse 2s infinite'
                    }}
                    onClick={(e) => {
                      e.stopPropagation();
                      // 直接跳转到药箱管理模块
                      setActiveTab('drugs');
                    }}
                  >
                    {alertText}
                  </button>
                );
              }
              return null;
            })()}
          </div>
        </div>
      </div>

      {(() => {
        const { expiredDrugs, expiringDrugs } = expiringDrugsResult;
        const expiredCount = expiredDrugs.length;
        const expiringCount = expiringDrugs.length;
        const totalExpiring = expiredCount + expiringCount;
        
        if (totalExpiring > 0) {
          // 生成提示文本
          let alertText = ' 您有';
          if (expiringCount > 0 && expiredCount > 0) {
            alertText += `${expiringCount}盒药品即将过期，${expiredCount}盒药品已过期，点击查看详情`;
          } else if (expiringCount > 0) {
            alertText += `${expiringCount}盒药品即将过期，点击查看详情`;
          } else {
            alertText += `${expiredCount}盒药品已过期，点击查看详情`;
          }
          
          const isAllExpired = totalExpiring === expiredCount;
          return (
            <div
              onClick={handleOpenExpiringDrugsModal}
              style={{
                cursor: 'pointer',
                background: isAllExpired
                  ? 'linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)'
                  : 'linear-gradient(135deg, #f39c12 0%, #e67e22 100%)',
                color: 'white',
                padding: '16px 20px',
                borderRadius: '12px',
                marginTop: '20px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '10px',
                boxShadow: isAllExpired
                  ? '0 4px 20px rgba(231, 76, 60, 0.5)'
                  : '0 4px 20px rgba(243, 156, 18, 0.5)',
                animation: 'pulse 2s infinite',
                fontWeight: 'bold',
                fontSize: '15px'
              }}
            >
              <span style={{ fontSize: '20px' }}>{alertText.includes('即将过期') && alertText.includes('已过期') ? '🔔' : alertText.includes('已过期') ? '⚠️' : '🔔'}</span>
              <span>
                {alertText.replace('🔔 您有', '')}
              </span>
            </div>
          );
        }
        return null;
      })()}
    </div>
  );

  const renderUploadTab = () => (
    <div className="card">
      <h2 className="card-title">
        <span className="card-title-icon">📷</span>
        上传药品照片
      </h2>

      {/* 批量识别区域 */}
      {batchRecognizeItems.length > 0 ? (
        <div className="batch-recognize-section">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '20px', color: '#4A90E2', margin: 0 }}>
              📸 批量识别结果 ({batchRecognizeItems.filter(i => i.status === 'success').length} 成功)
            </h3>
            <button
              onClick={() => {
                // 释放所有预览URL，避免内存泄漏
                batchRecognizeItems.forEach(item => URL.revokeObjectURL(item.previewUrl));
                setBatchRecognizeItems([]);
                setBatchSelectedForAdd(new Set());
              }}
              style={{
                padding: '8px 16px',
                fontSize: '14px',
                border: '2px solid #E0E0E0',
                borderRadius: '8px',
                background: 'white',
                color: '#6B6B6B',
                cursor: 'pointer'
              }}
            >
              清空
            </button>
          </div>

          <div className="batch-preview-grid">
            {batchRecognizeItems.map((item) => (
              <div
                key={item.id}
                className={`batch-preview-item ${item.status === 'success' ? 'success' : item.status === 'failed' ? 'failed' : 'pending'} ${item.status === 'success' && item.result?.drugId && batchSelectedForAdd.has(item.id) ? 'selected' : ''}`}
                onClick={() => {
                  if (item.status === 'success' && item.result?.drugId) {
                    setBatchSelectedForAdd(prev => {
                      const newSet = new Set(prev);
                      if (newSet.has(item.id)) {
                        newSet.delete(item.id);
                      } else {
                        newSet.add(item.id);
                      }
                      return newSet;
                    });
                  }
                }}
              >
                <img src={item.previewUrl} alt="预览" className="batch-preview-image" />
                <div className="batch-preview-status">
                  {item.status === 'success' ? '✓' : item.status === 'failed' ? '✗' : '⏳'}
                </div>
                {item.result?.drugName && (
                  <div className="batch-preview-name">{item.result.drugName}</div>
                )}
                {item.status === 'success' && item.result?.drugId && batchSelectedForAdd.has(item.id) && (
                  <div className="batch-preview-check">✓</div>
                )}
              </div>
            ))}
          </div>

          <div className="batch-actions">
            <span className="batch-count">
              已选择 <strong>{batchSelectedForAdd.size}</strong> 个药品
            </span>
            <button
              className="btn btn-primary"
              onClick={handleBatchRecognize}
              disabled={batchRecognizeItems.some(item => item.status === 'recognizing')}
              style={{ marginRight: '12px' }}
            >
              {batchRecognizeItems.some(item => item.status === 'recognizing') ? '⏳ 识别中...' : '🔍 开始识别'}
            </button>
            <button
              className={`btn ${batchSelectedForAdd.size > 0 ? 'btn-success' : 'btn-disabled'}`}
              onClick={handleBatchAddToMedicineBox}
              disabled={batchSelectedForAdd.size === 0 || isBatchAdding}
            >
              {isBatchAdding ? '⏳ 添加中...' : `✅ 全部加入药箱 (${batchSelectedForAdd.size})`}
            </button>
          </div>
        </div>
      ) : (
        <>
          <div
            className={`upload-area ${isDragging ? 'dragging' : ''} ${imagePreview ? 'has-image' : ''}`}
            onClick={(e) => {
              if (!imagePreview) {
                fileInputRef.current?.click();
              }
            }}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onDragEnter={handleDragEnter}
          >
            <input
              ref={fileInputRef}
              id="file-input"
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleFileUpload}
            />
            
            {/* 加载动画覆盖层 */}
            {(isLoading || showComplete) && (
              <div className={`loading-overlay ${showComplete ? 'loading-complete' : ''}`}>
                {!showComplete ? (
                  <>
                    <div className="loading-spinner-container">
                      <div className="loading-spinner"></div>
                      <div className="loading-spinner-ring"></div>
                    </div>
                    <div className="loading-progress-bar">
                      <div className="loading-progress-fill"></div>
                    </div>
                  </>
                ) : (
                  <div className="loading-complete-icon">✅</div>
                )}
                <p className="loading-text">
                  {showComplete ? '🎉 识别完成！正在跳转到结果页面...' : 
                   (ocrPolling ? '⏳ 正在查询识别结果，请稍候...' : '🔍 AI正在识别药品，请稍候...')}
                </p>
                {ocrTaskId && !showComplete && (
                  <p className="loading-task-id">
                    任务ID: {ocrTaskId}
                  </p>
                )}
                <div className="loading-steps">
                  <div className="loading-step completed">
                    <span className="step-icon">📤</span>
                    <span className="step-text">上传图片</span>
                  </div>
                  <div className="loading-step-arrow">→</div>
                  <div className={`loading-step ${showComplete ? 'completed' : 'active'}`}>
                    <span className="step-icon">🔍</span>
                    <span className="step-text">{showComplete ? '识别完成' : (ocrPolling ? '查询结果' : '识别中')}</span>
                  </div>
                  <div className="loading-step-arrow">→</div>
                  <div className={`loading-step ${showComplete ? 'completed' : 'pending'}`}>
                    <span className="step-icon">✅</span>
                    <span className="step-text">完成</span>
                  </div>
                </div>
              </div>
            )}
            
            {/* 图片预览 */}
            {imagePreview && !isLoading && !showComplete && (
              <div className="upload-preview-container">
                <img src={imagePreview} alt="药品预览" className="upload-preview-image" />
                <button 
                  className="upload-clear-btn" 
                  onClick={(e) => {
                    e.stopPropagation();
                    setImagePreview(null);
                    if (fileInputRef.current) {
                      fileInputRef.current.value = '';
                    }
                  }}
                >
                  ✕
                </button>
              </div>
            )}
            
            {/* 默认提示内容 */}
            {!imagePreview && !isLoading && !showComplete && (
              <>
                <span className="upload-icon">💊</span>
                <p className="upload-text">拖拽药盒图片到此处，或点击上传</p>
                <p className="upload-hint">支持 JPG、PNG 格式，文件小于10MB</p>
              </>
            )}
          </div>

          <div className="btn-group">
            <button
              className={`btn btn-primary btn-large ${isLoading ? 'btn-loading' : ''}`}
              onClick={analyzeImage}
              disabled={!imagePreview || isLoading}
            >
              {isLoading ? (
                <>
                  <span className="btn-spinner"></span>
                  识别中...
                </>
              ) : (
                '🔍 开始识别'
              )}
            </button>
            <button
              className="btn btn-secondary btn-large"
              onClick={handleBatchSelectFiles}
              style={{ marginLeft: '16px' }}
            >
              📸 批量拍照
            </button>
          </div>
        </>
      )}

      {/* 隐藏的批量文件输入 */}
      <input
        type="file"
        ref={batchFileInputRef}
        id="batch-file-input"
        accept="image/*"
        multiple
        onChange={handleBatchFileSelect}
        style={{ display: 'none' }}
      />

      <div style={{ marginTop: '48px' }}>
        <h3 style={{ fontSize: '26px', fontWeight: 'bold', color: 'var(--tech-blue)', marginBottom: '24px' }}>
          ✍️ 手动搜索药品
        </h3>
        <div className="manual-search-wrapper">
          <ManualDrugSearch 
            onSelectDrug={(drug, callbacks = {}) => {
              const { onComplete, onProgress } = callbacks;
              
              // 使用统一的药品信息获取函数
              const drugInfo = {
                id: drug.id || Date.now(),
                spec: drug.spec || '',
                name: drug.name,
                manufacturer: drug.manufacturer || '',
                matchScore: drug.matchScore || 0,
                genericName: drug.genericName || drug.name,
                tradeName: drug.tradeName || '',
                category: drug.category || ''
              };
              
              // 更新进度消息
              if (onProgress) {
                onProgress('正在获取药品详情...');
              }
              
              fetchDrugDetail(drug.name, drugInfo, {
                showLoading: false,
                onComplete: () => {
                  // 更新进度消息
                  if (onProgress) {
                    onProgress('正在跳转到用药说明页面...');
                  }
                  // 延迟执行完成回调，确保页面跳转完成
                  setTimeout(() => {
                    if (onComplete) {
                      onComplete();
                    }
                  }, 500);
                }
              });
            }}
          />
        </div>
        <p style={{ 
          fontSize: '14px', 
          color: '#9E9E9E', 
          marginTop: '16px', 
          textAlign: 'center',
          lineHeight: '1.6'
        }}>
          💡 支持输入药品名称、别名、商品名或类别（如：感冒药、止痛药）进行智能搜索
        </p>
      </div>
    </div>
  );

  const renderRecognitionTab = () => (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 className="card-title" style={{ margin: 0 }}>
          <span className="card-title-icon">✅</span>
          识别结果
          {recognizedDrugs.length > 0 && (
            <span style={{ fontSize: '16px', color: '#6B6B6B', marginLeft: '12px', fontWeight: 'normal' }}>
              （共 {recognizedDrugs.length} 个药品）
            </span>
          )}
        </h2>
        {recognizedDrugs.length > 0 && (
          <button
            className="btn btn-secondary"
            onClick={() => setActiveTab('upload')}
          >
            ← 返回继续识别
          </button>
        )}
      </div>

      {recognizedDrugs.length > 0 ? (
        <div className="drug-list">
          {recognizedDrugs.map((drug, index) => (
            <div key={index} className="drug-card" style={{ animationDelay: `${index * 0.15}s` }}>
              <span className="drug-card-icon">💊</span>
              <h4 className="drug-name">{drug.name}</h4>
              <p className="drug-info">规格：{drug.spec}</p>
              <p className="drug-info">生产厂家：{drug.manufacturer}</p>
              <p className="drug-info">匹配度：<span className="drug-match">{drug.matchScore}%</span></p>
              <button
                className="btn btn-primary"
                style={{ marginTop: '20px', width: '100%', minHeight: '56px' }}
                onClick={() => {
                  // 使用统一的药品信息获取函数
                  fetchDrugDetail(drug.name, drug, {
                    showLoading: false
                  });
                }}
              >
                📖 查看用药说明
              </button>
              <button
                className="btn btn-success"
                style={{ marginTop: '12px', width: '100%', minHeight: '56px' }}
                onClick={() => addToMedicineBox(drug)}
              >
                ➕ 加入我的药箱
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div className="loading-container">
          <p style={{ fontSize: '22px', color: 'var(--text-light)' }}>请先上传药品照片进行识别</p>
          <button className="btn btn-primary" style={{ marginTop: '24px' }} onClick={() => setActiveTab('upload')}>
            去上传
          </button>
        </div>
      )}
    </div>
  );

  const [showDrugList, setShowDrugList] = useState(false);
  const [isFetchingDrug, setIsFetchingDrug] = useState(false);

  // 统一的药品信息获取函数
  const fetchDrugDetail = (drugName, drugInfo = {}, options = {}) => {
    const { 
      showLoading = true,  // 是否显示加载状态
      onComplete = null    // 完成后的回调函数
    } = options;
    
    if (showLoading) {
      setIsFetchingDrug(true);
    }
    
    fetch(`/api/v1/drug/detail?drugName=${encodeURIComponent(drugName)}`)
      .then(res => res.json())
      .then(data => {
        if (data.code === 200 && data.data) {
          const drugDetail = data.data;
          // 合并基础信息和详细信息，确保字段完整性
          const fullDrugInfo = {
            id: drugInfo.id || Date.now(),
            name: drugInfo.name || drugDetail.genericName || drugName,
            spec: drugInfo.spec || drugDetail.specification || '',
            manufacturer: drugInfo.manufacturer || drugDetail.manufacturer || '',
            matchScore: drugInfo.matchScore || 0,
            genericName: drugDetail.genericName || drugName,
            tradeName: drugDetail.tradeName || '',
            approvalNumber: drugDetail.approvalNumber || '',
            category: drugDetail.category || '',
            ingredient: drugDetail.ingredient || '',
            indications: drugDetail.indications || '',
            usage: drugDetail.usage || '',
            precautions: drugDetail.precautions || '',
            adverseReactions: drugDetail.adverseReactions || '',
            description: drugDetail.description || '',
            dosage: drugDetail.usage || drugInfo.dosage || '',
            note: drugInfo.note || ''
          };
          // 先设置药品数据，然后立即切换标签
          setSelectedDrug(fullDrugInfo);
          setActiveTab('explanation');
          // 确保回调被调用
          setTimeout(() => {
            if (onComplete) onComplete();
          }, 0);
        } else {
          // 如果API返回数据不完整，使用现有信息并标记为不完整
          const fallbackDrug = {
            ...drugInfo,
            name: drugInfo.name || drugName,
            genericName: drugInfo.genericName || drugName,
            ingredient: drugInfo.ingredient || '暂无详细信息',
            indications: drugInfo.indications || '暂无详细信息',
            usage: drugInfo.usage || drugInfo.dosage || '暂无详细信息',
            precautions: drugInfo.precautions || '暂无详细信息',
            adverseReactions: drugInfo.adverseReactions || '暂无详细信息',
            description: drugInfo.description || ''
          };
          setSelectedDrug(fallbackDrug);
          setActiveTab('explanation');
          setTimeout(() => {
            if (onComplete) onComplete();
          }, 0);
        }
      })
      .catch(error => {
        console.error('获取药品详情失败:', error);
        // API调用失败时使用现有信息
        const fallbackDrug = {
          ...drugInfo,
          name: drugInfo.name || drugName,
          genericName: drugInfo.genericName || drugName,
          ingredient: drugInfo.ingredient || '暂无详细信息',
          indications: drugInfo.indications || '暂无详细信息',
          usage: drugInfo.usage || drugInfo.dosage || '暂无详细信息',
          precautions: drugInfo.precautions || '暂无详细信息',
          adverseReactions: drugInfo.adverseReactions || '暂无详细信息',
          description: drugInfo.description || ''
        };
        setSelectedDrug(fallbackDrug);
        setActiveTab('explanation');
        setTimeout(() => {
          if (onComplete) onComplete();
        }, 0);
      })
      .finally(() => {
        if (showLoading) {
          setIsFetchingDrug(false);
        }
      });
  };

  const renderExplanationTab = () => {
    // 初始状态：没有选中的药品时保持空白
    const hasSelectedDrug = selectedDrug !== null && selectedDrug !== undefined;
    
    if (!hasSelectedDrug) {
      return (
        <div className="card explanation-card empty-state">
          <div className="empty-state-icon">💊</div>
          <h2 className="card-title">
            <span className="card-title-icon">📖</span>
            用药说明（老年友好版）
          </h2>
          
          <div className="empty-state-content">
            <p className="empty-state-text">
              请先选择或识别一种药品，获取详细的用药指导
            </p>
            
            <div className="empty-state-actions">
              <button 
                className="btn btn-primary btn-large"
                onClick={() => setShowDrugList(true)}
              >
                📦 从药箱选择药品
              </button>
              <button 
                className="btn btn-secondary btn-large"
                onClick={() => setActiveTab('upload')}
              >
                📷 上传图片识别
              </button>
            </div>
          </div>
        </div>
      );
    }

    // 根据选中的药品获取用药说明数据
    const drugInfo = selectedDrug;

    // 使用从API获取的真实药品详细信息，如果没有则使用默认值
    const drugDetails = {
      ingredient: drugInfo.ingredient || '暂无详细信息',
      indications: drugInfo.indications || '暂无详细信息',
      usage: drugInfo.usage || drugInfo.dosage || '暂无详细信息',
      precautions: drugInfo.precautions || '暂无详细信息',
      adverseReactions: drugInfo.adverseReactions || '暂无详细信息'
    };

    // 将老年友好指导转换为HTML格式显示
    const formatGuideForDisplay = (guideText) => {
      if (!guideText) return '';
      
      // 将换行符转换为HTML换行，并添加高亮样式
      return guideText
        .split('\n')
        .filter(line => line.trim())
        .map((line, index) => {
          // 为【】中的内容添加高亮
          let formattedLine = line.replace(
            /【([^】]+)】/g,
            '<span class="highlight">【$1】</span>'
          );
          return `<div key={${index}} style="margin-bottom: 12px; line-height: 1.8;">${formattedLine}</div>`;
        })
        .join('');
    };

    // 生成用于显示的HTML内容
    const displayGuideHtml = formatGuideForDisplay(elderlyGuide);

    return (
      <div className="card explanation-card">
        <div className="herb-pattern"></div>
        
        {/* 头部区域：标题和操作按钮 */}
        <div className="explanation-header">
          <h2 className="card-title">
            <span className="card-title-icon">📖</span>
            用药说明（老年友好版）
          </h2>
          
          <div className="explanation-actions">
            <button 
              className="btn btn-secondary btn-medium"
              onClick={() => setShowDrugList(true)}
            >
              📦 切换药品
            </button>
            <button 
              className="btn btn-primary btn-medium"
              onClick={() => setActiveTab('upload')}
            >
              📷 上传识别
            </button>
          </div>
        </div>

        <div className="explanation-layout">
          {/* 上方区域：虚拟药剂师 */}
          <div className="pharmacist-section">
            <div className="chat-section">
              <div className="chat-header">
                <div className="speaker-avatar">👨‍⚕️</div>
                <div className="speaker-info">
                  <p className="speaker-name">虚拟药剂师</p>
                  <p className="speaker-title">老年友好用药指导</p>
                </div>
              </div>

              <div className="chat-bubble-wrapper">
                <div className="chat-bubble-avatar">👨‍⚕️</div>
                <div className="chat-bubble-content">
                  {isLoadingGuide ? (
                    <div style={{ textAlign: 'center', padding: '20px', color: '#666' }}>
                      <div className="loading-dna" style={{ marginBottom: '10px' }}>
                        <div className="dna-dot"></div>
                        <div className="dna-dot"></div>
                        <div className="dna-dot"></div>
                      </div>
                      <p style={{ fontSize: '14px' }}>正在生成老年友好用药指导...</p>
                    </div>
                  ) : elderlyGuide ? (
                    <div 
                      className="chat-bubble-text elderly-guide"
                      style={{ 
                        whiteSpace: 'pre-wrap', 
                        lineHeight: '2',
                        fontSize: '16px'
                      }}
                      dangerouslySetInnerHTML={{ __html: displayGuideHtml }}
                    />
                  ) : (
                    <p className="chat-bubble-text" style={{ color: '#999' }}>
                      正在加载用药指导...
                    </p>
                  )}
                  <div className="voice-wave-animation">
                    <span className="wave-bar"></span>
                    <span className="wave-bar"></span>
                    <span className="wave-bar"></span>
                    <span className="wave-bar"></span>
                    <span className="wave-bar"></span>
                  </div>
                </div>
              </div>

              <div className="voice-controls">
                <button
                  className={`btn ${isSpeaking ? 'btn-secondary' : 'btn-primary'}`}
                  onClick={() => isSpeaking ? stopSpeaking() : speak(elderlyGuide || '您好，正在加载用药指导')}
                  disabled={!elderlyGuide}
                >
                  {isSpeaking ? '⏸️ 停止播放' : '🔊 播放语音'}
                </button>
                <div className="speed-controls">
                  <span>语速：</span>
                  <button
                    className={`speed-btn ${speechRate === 0.6 ? 'active' : ''}`}
                    onClick={() => setSpeechRate(0.6)}
                  >
                    慢速
                  </button>
                  <button
                    className={`speed-btn ${speechRate === 1 ? 'active' : ''}`}
                    onClick={() => setSpeechRate(1)}
                  >
                    正常
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* 下方区域：药品信息 */}
          <div className="drug-details-section">
            <div className="left-drug-column">
              <div className="drug-info-panel">
                <div className="drug-info-card">
                  <div className="drug-info-icon">💊</div>
                  <h3 className="drug-info-name">{drugInfo.name}</h3>
                  <p className="drug-info-spec">规格：{drugInfo.spec}</p>
                  <p className="drug-info-mfr">{drugInfo.manufacturer}</p>
                  <div className="drug-info-divider"></div>
                  <p className="drug-info-dosage">💉 {drugInfo.dosage}</p>
                  <p className="drug-info-note">📌 {drugInfo.note}</p>
                </div>

                {/* 药品基本信息卡片 */}
                <div className="drug-details-card basic-info">
                  <div className="detail-section">
                    <h4 className="detail-title">🧪 药品成分</h4>
                    <p className="detail-content">{drugDetails.ingredient}</p>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-title">🎯 适应症</h4>
                    <p className="detail-content">{drugDetails.indications}</p>
                  </div>
                </div>
              </div>

              {/* 突出的加入药箱按钮 - 放在药品信息卡片下方 */}
              <div className="add-to-box-prominent">
                <button
                  className="btn btn-success btn-extra-large"
                  onClick={() => addToMedicineBox(drugInfo)}
                >
                  <span className="btn-icon">➕</span>
                  <span className="btn-text">加入我的药箱</span>
                </button>
              </div>
            </div>

            <div className="right-panel">
              {/* 核心用药信息 - 突出显示 */}
              <div className="key-info-card">
                <div className="key-info-header">
                  <div className="key-info-icon">📋</div>
                  <h3 className="key-info-title">用法用量</h3>
                </div>
                <div className="key-info-content">
                  <p className="key-info-text">{drugDetails.usage}</p>
                </div>
              </div>

              {/* 重要注意事项 */}
              <div className="warning-info-card">
                <div className="warning-info-header">
                  <div className="warning-info-icon">⚠️</div>
                  <h3 className="warning-info-title">注意事项</h3>
                </div>
                <div className="warning-info-content">
                  <p className="warning-info-text">{drugDetails.precautions}</p>
                </div>
              </div>

              {/* 不良反应 */}
              <div className="side-effect-card">
                <div className="side-effect-header">
                  <div className="side-effect-icon">🤒</div>
                  <h3 className="side-effect-title">不良反应</h3>
                </div>
                <div className="side-effect-content">
                  <p className="side-effect-text">{drugDetails.adverseReactions}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="warning-box">
          <h4 className="warning-title">
            ⚠️ 重要提醒
          </h4>
          <p className="warning-text">以上内容由AI生成，用药请遵医嘱</p>
        </div>
      </div>
    );
  };

  const renderConflictTab = () => {
    const hasConflict = drugList.length > 1;

    // 冲突规则缓存 key
    const CONFLICT_RULES_CACHE_KEY = 'conflict_rules_cache';

    // 从本地缓存加载冲突规则（断网时使用）
    const loadConflictFromCache = () => {
      try {
        const cached = localStorage.getItem(CONFLICT_RULES_CACHE_KEY);
        if (cached) {
          const { report, drugNames, timestamp } = JSON.parse(cached);
          console.log('从本地缓存加载冲突规则，缓存时间:', new Date(timestamp).toLocaleString());
          
          // 检查缓存的药品列表是否与当前药箱匹配
          const currentDrugNames = drugList.map(d => d.name).sort();
          const cachedDrugNames = [...drugNames].sort();
          const isCacheValid = JSON.stringify(currentDrugNames) === JSON.stringify(cachedDrugNames);
          
          if (isCacheValid) {
            setConflictReport(report);
            showToast('已加载缓存的冲突规则（离线模式）', 'info');
            return true;
          } else {
            showToast('缓存的冲突规则与当前药箱不匹配，请联网后重新检测', 'warning');
          }
        }
      } catch (err) {
        console.error('从缓存加载冲突规则失败:', err);
      }
      return false;
    };

    // 调用冲突检测API
    const handleCheckConflicts = async () => {
      if (drugList.length === 0) {
        showToast('请先添加药品到药箱', 'warning');
        return;
      }

      setIsCheckingConflicts(true);
      setConflictError(null);

      try {
        // 获取药箱中的药品名称列表
        const drugNames = drugList.map(drug => drug.name);
        
        console.log('=== 开始冲突检测 ===');
        console.log('药品列表:', drugNames);

        const response = await fetch('/api/conflict/check', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(drugNames)
        });

        const data = await response.json();
        console.log('冲突检测响应:', data);

        if (data.code === 200 && data.data) {
          setConflictReport(data.data);
          console.log('冲突检测成功:', data.data);
          
          // 保存到本地缓存（用于断网可读）
          try {
            localStorage.setItem(CONFLICT_RULES_CACHE_KEY, JSON.stringify({
              report: data.data,
              drugNames: drugNames,
              timestamp: Date.now()
            }));
            console.log('冲突规则已缓存');
          } catch (cacheErr) {
            console.error('缓存冲突规则失败:', cacheErr);
          }
        } else {
          setConflictError(data.message || '冲突检测失败');
          // 尝试从缓存读取
          loadConflictFromCache();
        }
      } catch (error) {
        console.error('冲突检测异常:', error);
        setConflictError('网络错误，请稍后重试');
        // 尝试从缓存读取
        loadConflictFromCache();
      } finally {
        setIsCheckingConflicts(false);
      }
    };

    // 生成冲突报告卡片
    const handleGenerateReport = async (action) => {
      if (!conflictReport || !conflictReport.conflicts || conflictReport.conflicts.length === 0) {
        return;
      }
      
      if (action === 'screenshot') {
        // 截图/图片合成 - 使用隐藏容器
        try {
          // 1. 显示隐藏容器
          setShowScreenshotContainer(true);
          
          // 2. 等待渲染完成
          await new Promise(resolve => setTimeout(resolve, 150));
          
          // 3. 对隐藏容器截图
          if (screenshotContainerRef.current) {
            const canvas = await html2canvas(screenshotContainerRef.current, {
              backgroundColor: '#ffffff',
              scale: 2,
              useCORS: true
            });
            
            // 4. 下载图片
            const link = document.createElement('a');
            link.download = `用药冲突报告_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.png`;
            link.href = canvas.toDataURL('image/png');
            link.click();
          }
          
          // 5. 隐藏容器
          setShowScreenshotContainer(false);
        } catch (error) {
          console.error('截图失败:', error);
          setShowScreenshotContainer(false);
        }
      } else if (action === 'copy') {
        // 显示弹窗并复制文本
        setShowConflictReport(true);
        await new Promise(resolve => setTimeout(resolve, 100));
        
        const textContent = [
          ` 用药冲突检测报告`,
          `检测时间: ${new Date(conflictReport.checkTime).toLocaleString('zh-CN')}`,
          `检测药品数: ${conflictReport.drugsChecked?.length || 0} 种`,
          '',
          `🔴 严重: ${conflictReport.statistics.severeCount} | 🟡 中度: ${conflictReport.statistics.moderateCount} |  轻微: ${conflictReport.statistics.mildCount}`,
          '',
          '冲突详情:',
          ...conflictReport.conflicts.map((c, i) => {
            let level = c.severity === 'SEVERE' ? '🔴 严重冲突' : c.severity === 'MODERATE' ? '🟡 中等冲突' : '🔵 轻微注意';
            let text = `${i + 1}. ${level}: ${c.drugA} ⚡ ${c.drugB}`;
            if (c.conflictExplanation) text += `\n   说明: ${c.conflictExplanation}`;
            if (c.riskWarning) text += `\n   ⚠️ ${c.riskWarning}`;
            return text;
          }),
          '',
          conflictReport.generalAdvice ? `💊 总体建议: ${conflictReport.generalAdvice}` : ''
        ].join('\n');

        try {
          await navigator.clipboard.writeText(textContent);
        } catch (error) {
          console.error('复制失败:', error);
        }
      }
    };

    return (
      <div className="card">
        <h2 className="card-title">
          <span className="card-title-icon">⚠️</span>
          用药安全检查
        </h2>

        {drugList.length === 0 ? (
          <div className="safe-display">
            <span className="shield-icon">📦</span>
            <h3 className="safe-title">药箱为空</h3>
            <p className="safe-subtitle">请先在"药箱管理"中添加药品</p>
            <button
              className="btn btn-primary btn-large"
              style={{ marginTop: '24px' }}
              onClick={() => setActiveTab('drugs')}
            >
              🏠 去添加药品
            </button>
          </div>
        ) : isCheckingConflicts ? (
          <div className="conflict-section">
            <div style={{ textAlign: 'center', padding: '48px' }}>
              <div className="loading-spinner-container" style={{ margin: '0 auto 20px' }}>
                <div className="loading-spinner"></div>
              </div>
              <p style={{ fontSize: '18px', color: 'var(--text-primary)' }}>
                🔍 正在调用AI分析药品冲突，请稍候...
              </p>
              <p style={{ fontSize: '14px', color: 'var(--text-light)', marginTop: '12px' }}>
                系统正在检测 {drugList.length} 种药品之间的相互作用
              </p>
            </div>
          </div>
        ) : conflictError ? (
          <div className="conflict-section">
            <div className="safe-display">
              <span className="shield-icon">❌</span>
              <h3 className="safe-title">检测失败</h3>
              <p className="safe-subtitle">{conflictError}</p>
              <button
                className="btn btn-primary btn-large"
                style={{ marginTop: '24px' }}
                onClick={handleCheckConflicts}
              >
                🔄 重新检测
              </button>
            </div>
          </div>
        ) : conflictReport && conflictReport.conflicts && conflictReport.conflicts.length > 0 ? (
          <div className="conflict-section">
            {/* 统计信息 */}
            <div style={{ 
              display: 'flex', 
              gap: '16px', 
              marginBottom: '24px',
              justifyContent: 'center'
            }}>
              {conflictReport.statistics.severeCount > 0 && (
                <div style={{
                  padding: '12px 20px',
                  background: 'linear-gradient(135deg, #dc2626, #b91c1c)',
                  borderRadius: '12px',
                  color: 'white',
                  textAlign: 'center',
                  boxShadow: '0 4px 12px rgba(220, 38, 38, 0.3)'
                }}>
                  <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{conflictReport.statistics.severeCount}</div>
                  <div style={{ fontSize: '12px' }}>严重冲突</div>
                </div>
              )}
              {conflictReport.statistics.moderateCount > 0 && (
                <div style={{
                  padding: '12px 20px',
                  background: 'linear-gradient(135deg, #ea580c, #c2410c)',
                  borderRadius: '12px',
                  color: 'white',
                  textAlign: 'center',
                  boxShadow: '0 4px 12px rgba(234, 88, 12, 0.3)'
                }}>
                  <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{conflictReport.statistics.moderateCount}</div>
                  <div style={{ fontSize: '12px' }}>中度冲突</div>
                </div>
              )}
              {conflictReport.statistics.mildCount > 0 && (
                <div style={{
                  padding: '12px 20px',
                  background: 'linear-gradient(135deg, #ca8a04, #a16207)',
                  borderRadius: '12px',
                  color: 'white',
                  textAlign: 'center',
                  boxShadow: '0 4px 12px rgba(202, 138, 4, 0.3)'
                }}>
                  <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{conflictReport.statistics.mildCount}</div>
                  <div style={{ fontSize: '12px' }}>轻微注意</div>
                </div>
              )}
            </div>

            {/* 冲突列表 */}
            {conflictReport.conflicts.map((conflict, index) => (
              <div 
                key={index}
                className={`conflict-item conflict-level-${conflict.severity?.toLowerCase() || 'mild'}`}
                style={{ marginBottom: '16px' }}
              >
                <span className={`conflict-badge ${conflict.severity?.toLowerCase() || 'mild'}`}>
                  {conflict.severity === 'SEVERE' && '🔴 严重冲突'}
                  {conflict.severity === 'MODERATE' && '🟡 中等冲突'}
                  {conflict.severity === 'MILD' && '🔵 轻微注意'}
                  {(!conflict.severity || conflict.severity === 'NONE') && '🟢 安全'}
                </span>
                <div className="drug-connection">
                  <div className="drug-node">{conflict.drugA}</div>
                  <span className="drug-connector">⚡</span>
                  <div className="drug-node">{conflict.drugB}</div>
                </div>
                <div className="conflict-explanation">
                  {conflict.conflictExplanation && (
                    <p className="conflict-explanation-text">
                      {conflict.conflictExplanation}
                    </p>
                  )}
                  {conflict.riskWarning && (
                    <p className="conflict-explanation-text" style={{ 
                      color: conflict.severity === 'SEVERE' ? '#dc2626' : 
                             conflict.severity === 'MODERATE' ? '#ea580c' : '#856404',
                      fontWeight: 'bold',
                      marginTop: '8px'
                    }}>
                      ⚠️ {conflict.riskWarning}
                    </p>
                  )}
                  {conflict.alternatives && conflict.alternatives.length > 0 && (
                    <div style={{ marginTop: '12px' }}>
                      <p style={{ fontSize: '14px', fontWeight: 'bold', marginBottom: '8px' }}>💡 建议:</p>
                      {conflict.alternatives.map((alt, altIndex) => (
                        <p key={altIndex} style={{ fontSize: '13px', color: '#666', marginLeft: '12px' }}>
                          • {alt}
                        </p>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ))}

            {/* 总体建议 */}
            {conflictReport.generalAdvice && (
              <div className="warning-box" style={{ marginTop: '24px', background: '#e0f2fe' }}>
                <h4 className="warning-title" style={{ color: '#0369a1' }}>
                  💊 总体用药建议
                </h4>
                <p className="warning-text" style={{ color: '#075985' }}>
                  {conflictReport.generalAdvice}
                </p>
              </div>
            )}

            <div style={{ display: 'flex', gap: '16px', marginTop: '32px' }}>
              <button 
                className="btn btn-primary btn-large" 
                style={{ flex: 1 }}
                onClick={handleCheckConflicts}
              >
                🔄 重新检测
              </button>
              {conflictReport && conflictReport.conflicts && conflictReport.conflicts.length > 0 && (
                <>
                  <button 
                    className="btn btn-primary btn-large" 
                    style={{ flex: 1 }}
                    onClick={() => handleGenerateReport('screenshot')}
                  >
                    📷 截图报告
                  </button>
                  <button 
                    className="btn btn-primary btn-large" 
                    style={{ flex: 1 }}
                    onClick={() => handleGenerateReport('copy')}
                  >
                    📋 复制文本
                  </button>
                </>
              )}
            </div>
          </div>
        ) : conflictReport && conflictReport.conflicts && conflictReport.conflicts.length === 0 ? (
          <div className="safe-display">
            <span className="shield-icon">🛡️</span>
            <h3 className="safe-title">未发现明显冲突</h3>
            <p className="safe-subtitle">您的用药方案在AI分析后未发现明显冲突</p>
            <p style={{ fontSize: '14px', color: '#666', marginTop: '12px' }}>
              ✅ 检测了 {drugList.length} 种药品，未发现问题
            </p>
          </div>
        ) : (!conflictReport && !isCheckingConflicts && !conflictError && drugList.length > 0) || conflictNeedsRecheck ? (
          <div className="conflict-section">
            <div style={{ textAlign: 'center', padding: '32px' }}>
              {conflictNeedsRecheck ? (
                <>
                  <div style={{ fontSize: '64px', marginBottom: '16px' }}>⚠️</div>
                  <h3 style={{ fontSize: '20px', marginBottom: '12px', color: '#e65100' }}>
                    药品已更新，建议重新检测
                  </h3>
                  <p style={{ fontSize: '14px', color: '#666', marginBottom: '24px' }}>
                    您最近添加了新药品，药箱中共有 {drugList.length} 种药品
                  </p>
                </>
              ) : (
                <>
                  <div style={{ fontSize: '64px', marginBottom: '16px' }}>🔍</div>
                  <h3 style={{ fontSize: '20px', marginBottom: '12px' }}>
                    AI智能冲突检测
                  </h3>
                  <p style={{ fontSize: '14px', color: '#666', marginBottom: '24px' }}>
                    基于DeepSeek大模型，分析您的 {drugList.length} 种药品之间可能存在的相互作用
                  </p>
                </>
              )}
              <button 
                className="btn btn-primary btn-large"
                onClick={handleCheckConflicts}
                style={{ minHeight: '56px', fontSize: '18px' }}
              >
                {conflictNeedsRecheck ? '🔬 重新检测冲突' : ' 开始检测'}
              </button>
            </div>
          </div>
        ) : null}

      </div>
    );
  };

  const renderCalendarTab = () => (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 className="card-title" style={{ marginBottom: 0 }}>
          <span className="card-title-icon">📅</span>
          {calendarViewMode === 'today' ? '今日用药时间轴' : '一周用药记录'}
        </h2>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          {/* 调试按钮：手动触发提醒 */}
          <button
            className="btn btn-warning"
            onClick={handleTriggerReminderManually}
            style={{ 
              minHeight: '40px',
              fontSize: '14px',
              background: '#ff9800',
              color: 'white',
              border: 'none'
            }}
            title="手动触发用药提醒（调试用）"
          >
            🔔 测试提醒
          </button>
          <button
            className={`btn ${calendarViewMode === 'today' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => handleCalendarViewChange('today')}
          >
            今日
          </button>
          <button
            className={`btn ${calendarViewMode === 'week' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => handleCalendarViewChange('week')}
          >
            一周
          </button>
        </div>
      </div>

      {calendarViewMode === 'today' ? renderTodayCalendar() : renderWeekCalendar()}
    </div>
  );

  const renderTodayCalendar = () => (
    <>
      {/* 只在首次加载且没有数据时显示加载动画 */}
      {isLoadingCalendar && calendarPlans.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '48px', color: 'var(--text-light)' }}>
          <div className="loading-spinner-container" style={{ margin: '0 auto 20px' }}>
            <div className="loading-spinner"></div>
          </div>
          <p style={{ fontSize: '18px' }}>正在从家庭药箱生成用药计划...</p>
        </div>
      ) : calendarPlans.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '48px', color: 'var(--text-light)' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>📋</div>
          <p style={{ fontSize: '22px', marginBottom: '12px' }}>今日暂无用药计划</p>
          <p style={{ fontSize: '16px', marginTop: '12px' }}>请先在"药箱管理"中添加需要服用的药品</p>
          <button
            className="btn btn-primary btn-large"
            style={{ marginTop: '24px' }}
            onClick={() => setActiveTab('drugs')}
          >
            🏠 去添加药品
          </button>
        </div>
      ) : (
        <>
          <div className="timeline-container">
            <div className="timeline-line"></div>
            {calendarPlans.map((reminder) => (
              <div
                key={reminder.id}
                className={`timeline-item ${reminder.taken ? 'taken' : reminder.missed ? 'missed' : 'pending'}`}
              >
                <div className="timeline-header">
                  <div className="timeline-time">
                    {reminder.time}
                    <span className="timeline-period">（{reminder.period}）</span>
                  </div>
                </div>
                <p className="timeline-drug">💊 {reminder.drug}{reminder.boxDrugName ? `（${reminder.boxDrugName}）` : ''}</p>
                {reminder.dosage && (
                  <p className="timeline-dosage" style={{ fontSize: '14px', color: 'var(--text-light)', marginLeft: '8px' }}>
                    用量：{reminder.dosage}
                  </p>
                )}
                <div className="timeline-status">
                  {reminder.taken ? (
                    <>
                      <span className="status-taken">✓ 已吃</span>
                      <button
                        className="btn btn-secondary btn-undo"
                        onClick={() => undoMarkAsTaken(reminder.id)}
                        style={{ minHeight: '44px', marginTop: '12px' }}
                      >
                        ↩️ 撤销
                      </button>
                    </>
                  ) : reminder.missed ? (
                    <>
                      <span className="status-missed">⏰ 漏服</span>
                      <div className="reminder-suggestion">
                        <p className="reminder-suggestion-text">
                          ⏱️ 现在可以补服，但下次请按时服药哦
                        </p>
                      </div>
                    </>
                  ) : (
                    <>
                      <span className="status-pending">🔔 待吃</span>
                      <button
                        className={`btn btn-success btn-ripple taken-confirmed ${takenButtons[reminder.id] ? 'confirmed' : ''}`}
                        onClick={(e) => markAsTaken(reminder.id, e)}
                        style={{ minHeight: '52px' }}
                        disabled={!reminder.planId}
                        title={reminder.planId ? '' : '该计划为自动生成，暂无法标记'}
                      >
                        <span className="btn-text">✓ 我吃了</span>
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>

          </>
      )}
    </>
  );

  // 周视图辅助：把任意日期归一成 YYYY-MM-DD 字符串（本地时区）
  const toDateKey = (d) => {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  };

  // 周视图辅助：归一化后端 status
  // MedicationPlan.status 写的是 "completed"/"cancelled"；
  // MedicationLog.status 写的是 "taken"/"skipped"；
  // 周视图走的是前者，这里统一映射到 UI 关心的语义。
  const normalizeWeekStatus = (raw) => {
    const s = (raw || '').toLowerCase();
    if (s === 'taken' || s === 'completed') return 'taken';
    if (s === 'skipped' || s === 'cancelled') return 'skipped';
    if (s === 'missed' || s === 'deleted') return 'missed';
    return 'pending';
  };

  // 周视图辅助：根据 day.date 与今天的相对位置返回 'past' | 'today' | 'future'
  const getDayPhase = (dateStr) => {
    const todayKey = toDateKey(new Date());
    if (dateStr === todayKey) return 'today';
    return dateStr < todayKey ? 'past' : 'future';
  };

  // 周视图辅助：状态点颜色映射
  const getStatusDotClass = (rawStatus) => {
    const s = normalizeWeekStatus(rawStatus);
    if (s === 'taken') return 'dot-taken';
    if (s === 'missed') return 'dot-missed';
    if (s === 'skipped') return 'dot-skipped';
    return 'dot-pending';
  };

  const renderWeekCalendar = () => {
    // 首次加载且没数据：保留 loading
    if (isLoadingCalendar && (!weeklyMedicationData || weeklyMedicationData.dailyRecords.length === 0)) {
      return (
        <div style={{ textAlign: 'center', padding: '48px', color: 'var(--text-light)' }}>
          <div className="loading-spinner-container" style={{ margin: '0 auto 20px' }}>
            <div className="loading-spinner"></div>
          </div>
          <p style={{ fontSize: '18px' }}>正在加载一周用药记录...</p>
        </div>
      );
    }

    // 没有任何 7 天数据：空态
    if (!weeklyMedicationData || weeklyMedicationData.dailyRecords.length === 0) {
      return (
        <div style={{ textAlign: 'center', padding: '48px', color: 'var(--text-light)' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>📋</div>
          <p style={{ fontSize: '22px', marginBottom: '12px' }}>一周内暂无用药记录</p>
          <p style={{ fontSize: '16px', marginTop: '12px' }}>请先在"药箱管理"中添加需要服用的药品</p>
          <button
            className="btn btn-primary btn-large"
            style={{ marginTop: '24px' }}
            onClick={() => setActiveTab('drugs')}
          >
            🏠 去添加药品
          </button>
        </div>
      );
    }

    // 取选中的那一天
    const selectedDay = selectedWeekDay
      ? weeklyMedicationData.dailyRecords.find(d => d.date === selectedWeekDay)
      : null;

    return (
      <div className="week-view">
        {/* 7 格日历 */}
        <div className="week-grid" role="grid">
          {weeklyMedicationData.dailyRecords.map((day) => {
            const phase = getDayPhase(day.date);
            const isToday = phase === 'today';
            const isSelected = selectedWeekDay === day.date;
            const items = day.items || [];
            const total = items.length;
            const taken = items.filter(i => normalizeWeekStatus(i.status) === 'taken').length;
            const weekday = ['日', '一', '二', '三', '四', '五', '六'][new Date(day.date).getDay()];
            const dayNum = new Date(day.date).getDate();
            // 状态点：最多 5 个，超出折叠为 +N
            const dots = items.slice(0, 5);
            const overflow = items.length - dots.length;
            const allTaken = total > 0 && taken === total;

            return (
              <button
                key={day.date}
                type="button"
                aria-pressed={isSelected}
                aria-label={`${formatDate(day.date)}，共 ${total} 项，已服 ${taken} 项`}
                className={[
                  'week-day-cell',
                  isToday ? 'is-today' : '',
                  isSelected ? 'is-selected' : '',
                  phase === 'past' ? 'is-past' : '',
                  phase === 'future' ? 'is-future' : '',
                  total === 0 ? 'is-empty' : ''
                ].filter(Boolean).join(' ')}
                onClick={() => handleWeekDayToggle(day.date)}
              >
                <div className="week-day-cell__weekday">周{weekday}</div>
                <div className="week-day-cell__num">{dayNum}</div>
                <div className={`week-day-cell__completion ${allTaken ? 'is-all-taken' : ''}`}>
                  {total === 0 ? '—' : `${taken}/${total}`}
                </div>
                <div className="week-day-cell__dots" aria-hidden="true">
                  {total === 0 ? <span className="week-day-cell__empty">无</span> : (
                    <>
                      {dots.map((item, idx) => (
                        <span key={`${item.planId}_${idx}`} className={`week-dot ${getStatusDotClass(item.status)}`} />
                      ))}
                      {overflow > 0 && <span className="week-dot-overflow">+{overflow}</span>}
                    </>
                  )}
                </div>
                <div className="week-day-cell__count">{total === 0 ? '无计划' : `${total} 项`}</div>
              </button>
            );
          })}
        </div>

        {/* 就地下钻的日详情 */}
        {selectedDay && (
          <div className="week-day-detail" role="region" aria-label={`${formatDate(selectedDay.date)}用药详情`}>
            <div className="week-day-detail__header">
              <div className="week-day-detail__title">
                <span className="week-day-detail__date">{formatDate(selectedDay.date)}</span>
                <span className="week-day-detail__phase">
                  {getDayPhase(selectedDay.date) === 'today' && '今天'}
                  {getDayPhase(selectedDay.date) === 'past' && '已过'}
                  {getDayPhase(selectedDay.date) === 'future' && '未来'}
                </span>
              </div>
              <button
                type="button"
                className="week-day-detail__close"
                aria-label="收起"
                onClick={() => setSelectedWeekDay(null)}
              >
                收起 ✕
              </button>
            </div>

            {selectedDay.items.length === 0 ? (
              <p className="week-day-detail__empty">这一天没有用药计划</p>
            ) : (
              <div className="timeline-container">
                <div className="timeline-line"></div>
                {selectedDay.items.map((item, index) => {
                  const phase = getDayPhase(selectedDay.date);
                  const ns = normalizeWeekStatus(item.status);
                  const isTaken = ns === 'taken';
                  const isMissed = ns === 'missed';
                  const isSkipped = ns === 'skipped';
                  // 软删除的药：补打/撤销一律禁用
                  const isDeleted = item.deleted === true;
                  // 过去日期：按钮文案换成"补打"；当天保持"我吃了"；未来 / 软删除：禁用
                  const confirmDisabled = phase === 'future' || !item.planId || isDeleted;
                  const confirmLabel = phase === 'past' ? '⏰ 补打' : '✓ 我吃了';
                  const showConfirm = !isTaken && !isSkipped;
                  const confirmTitle = isDeleted
                    ? '该药品已从药箱移除，无法补打'
                    : phase === 'future'
                      ? '该计划尚未到时点'
                      : phase === 'past'
                        ? '补打：补录这次服药记录'
                        : '我吃了';

                  return (
                    <div
                      key={`${item.planId}_${index}`}
                      className={`timeline-item ${isTaken ? 'taken' : isMissed ? 'missed' : isSkipped ? 'skipped' : 'pending'}`}
                      style={item.deleted ? { opacity: 0.7 } : {}}
                    >
                      <div className="timeline-header">
                        <div className="timeline-time">
                          {getTimeBySlot(item.timeSlot)}
                          <span className="timeline-period">（{item.timeSlotLabel}）</span>
                        </div>
                        {item.deleted && (
                          <span style={{ fontSize: '12px', color: '#999', marginLeft: '8px' }}>（已删除）</span>
                        )}
                      </div>
                      <p className="timeline-drug">💊 {item.drugName}{item.boxDrugName ? `（${item.boxDrugName}）` : ''}</p>
                      {item.dosageAtTime && (
                        <p className="timeline-dosage" style={{ fontSize: '14px', color: 'var(--text-light)', marginLeft: '8px' }}>
                          用量：{item.dosageAtTime}
                        </p>
                      )}
                      <div className="timeline-status">
                        {isTaken ? (
                          <>
                            <span className="status-taken">✓ 已吃</span>
                            <button
                              className="btn btn-secondary btn-undo"
                              style={{ minHeight: '44px', marginTop: '12px' }}
                              onClick={() => handleWeekItemUndo(selectedDay.date, item)}
                              disabled={!item.planId}
                              title="撤销这次服药记录"
                            >
                              ↩️ 撤销
                            </button>
                          </>
                        ) : isSkipped ? (
                          <span className="status-missed">➖ 已跳过</span>
                        ) : (
                          <>
                            <span className={isMissed ? 'status-missed' : 'status-pending'}>
                              {isMissed ? '⏰ 漏服' : '🔔 待吃'}
                            </span>
                            {showConfirm && (
                              <button
                                className="btn btn-success btn-ripple"
                                style={{ minHeight: '52px' }}
                                onClick={() => handleWeekItemConfirm(selectedDay.date, item)}
                                disabled={confirmDisabled}
                                title={confirmTitle}
                              >
                                <span className="btn-text">{confirmLabel}</span>
                              </button>
                            )}
                            {isDeleted && (
                              <span className="week-day-detail__hint week-day-detail__hint--danger">
                                该药品已从药箱移除
                              </span>
                            )}
                            {!isDeleted && phase === 'past' && isMissed && (
                              <span className="week-day-detail__hint">过去日期，可补打</span>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </div>
    );
  };

  const formatDate = (dateStr) => {
    const date = new Date(dateStr);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return '今天';
    } else if (date.toDateString() === yesterday.toDateString()) {
      return '昨天';
    } else {
      return `${date.getMonth() + 1}月${date.getDate()}日 ${['日', '一', '二', '三', '四', '五', '六'][date.getDay()]}`;
    }
  };

  const handleSearchDrugs = async (keyword) => {
    setSearchQuery(keyword);
    
    // 如果搜索框为空，显示完整列表
    if (!keyword.trim()) {
      setFilteredDrugList([]);
      return;
    }
    
    try {
      setIsSearching(true);
      const response = await fetch(`/api/v1/box/search?userId=${user.userId}&keyword=${encodeURIComponent(keyword)}&status=active`);
      const data = await response.json();
      
      console.log('=== 搜索药箱响应 ===');
      console.log('状态码:', response.status);
      console.log('响应数据:', data);
      console.log('==================');
      
      if (response.ok && data.code === 200) {
        // 转换后端数据格式为前端需要的格式
        const drugs = data.data.map(item => ({
          boxItemId: item.boxItemId,
          drugId: item.drugId,
          name: item.drugName,
          spec: item.specification,
          dosage: item.dosage,
          frequency: item.frequency,
          startDate: item.startDate,
          endDate: item.endDate,
          expiryDate: item.expiryDate,
          totalQuantity: item.totalQuantity,
          remaining: item.remainingQuantity || item.totalQuantity,
          note: item.note,
          status: item.status,
          createdAt: item.createdAt
        }));
        setFilteredDrugList(drugs);
      } else {
        console.error('搜索失败:', data.message);
        setFilteredDrugList([]);
      }
    } catch (err) {
      console.error('搜索异常:', err);
      setFilteredDrugList([]);
    } finally {
      setIsSearching(false);
    }
  };

  const renderDrugsTab = () => {
    return (
      <DrugListView
        drugList={drugList}
        searchQuery={searchQuery}
        isSearching={isSearching}
        filteredDrugList={filteredDrugList}
        calendarPlans={calendarPlans}
        onSearch={handleSearchDrugs}
        onAddDrug={() => setShowAddDrugModal(true)}
        onOpenDrugDetail={handleOpenDrugDetail}
        onOpenAddToPlanModal={handleOpenAddToPlanModal}
        onDiscardDrug={handleDiscardDrugFromCard}
        onDeleteDrug={handleDeleteDrug}
        onReloadDrugList={() => loadMedicineBoxList(user?.userId)}
      />
    );
  };

  return (
    <>
      {/* 百度TTS音频播放器 */}
      <audio ref={audioRef} style={{ display: 'none' }} />

      <div className="watermark-bg"></div>
      {showRegister ? (
        <Register onRegister={handleRegister} />
      ) : !isLoggedIn ? (
        <Login onLogin={handleLogin} onShowRegister={() => setShowRegister(true)} />
      ) : (
        <div className="app-container">
          {renderHeader()}

          <div className="main-content">
            <div className="nav-tabs">
              <button className={`nav-tab ${activeTab === 'home' ? 'active' : ''}`} onClick={() => setActiveTab('home')}>
                🏠 首页
              </button>
              <button className={`nav-tab ${activeTab === 'upload' ? 'active' : ''}`} onClick={() => setActiveTab('upload')}>
                📷 识别药品
              </button>
              <button className={`nav-tab ${activeTab === 'explanation' ? 'active' : ''}`} onClick={() => setActiveTab('explanation')}>
                📖 用药说明
              </button>
              <button className={`nav-tab ${activeTab === 'conflict' ? 'active' : ''}`} onClick={() => setActiveTab('conflict')}>
                ⚠️ 冲突检测
              </button>
              <button className={`nav-tab ${activeTab === 'calendar' ? 'active' : ''}`} onClick={() => setActiveTab('calendar')}>
                📅 用药日历
              </button>
              <button className={`nav-tab ${activeTab === 'drugs' ? 'active' : ''}`} onClick={() => setActiveTab('drugs')}>
                🏠 药箱管理
              </button>
              <button className={`nav-tab ${activeTab === 'emergency' ? 'active' : ''}`} onClick={() => setActiveTab('emergency')}>
                🚨 紧急助手
              </button>
            </div>

            {activeTab === 'home' && renderHomeTab()}
            {activeTab === 'upload' && renderUploadTab()}
            {activeTab === 'recognition' && renderRecognitionTab()}
            {activeTab === 'explanation' && renderExplanationTab()}
            {activeTab === 'conflict' && renderConflictTab()}
            {activeTab === 'calendar' && renderCalendarTab()}
            {activeTab === 'drugs' && renderDrugsTab()}
            {activeTab === 'emergency' && (
              <div className="card emergency-card">
                <EmergencyAssistant emergencyContacts={emergencyContacts} />
              </div>
            )}
          </div>

        </div>
      )}

      {showProfileModal && (
        <ProfileModal
          onComplete={handleProfileComplete}
          onClose={() => setShowProfileModal(false)}
          userId={user?.userId}
        />
      )}

      {showCelebration && (
        <div className="celebration-overlay">
          <div className="celebration-card">
            <span className="celebration-icon">🎉</span>
            <p className="celebration-text">真棒！</p>
          </div>
        </div>
      )}

      {/* 撤销确认弹窗 */}
      {showUndoConfirmModal && (
        <div className="modal-overlay" onClick={() => setShowUndoConfirmModal(false)}>
          <div className="modal-content undo-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">⚠️ 确认撤销</h3>
              <button className="modal-close-btn" onClick={() => setShowUndoConfirmModal(false)}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              <p style={{ textAlign: 'center', fontSize: '16px', color: 'var(--text-primary)', lineHeight: '1.8' }}>
                确定要撤销吗？这将标记为未服药状态，并恢复药箱中的剩余数量。
              </p>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowUndoConfirmModal(false)}>
                取消
              </button>
              <button className="btn btn-danger" onClick={confirmUndo}>
                确定撤销
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 药品选择弹窗 */}
      {showDrugList && (
        <div className="modal-overlay" onClick={() => setShowDrugList(false)}>
          <div className="modal-content drug-list-modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">📦 选择药品</h3>
              <button className="modal-close-btn" onClick={() => setShowDrugList(false)}>
                ✕
              </button>
            </div>
            
            <div className="modal-body">
              {drugList.length > 0 ? (
                <div className="drug-list-container">
                  {drugList.map((drug, index) => (
                    <div 
                      key={index}
                      className={`drug-list-item ${isFetchingDrug ? 'disabled' : ''}`}
                      onClick={() => {
                        if (!isFetchingDrug) {
                          // 先关闭弹窗，然后获取药品信息
                          setShowDrugList(false);
                          setIsFetchingDrug(true);
                          // 使用统一的药品信息获取函数
                          fetchDrugDetail(drug.name, drug, {
                            showLoading: false,
                            onComplete: () => {
                              setIsFetchingDrug(false);
                            }
                          });
                        }
                      }}
                    >
                      <div className="drug-item-icon">💊</div>
                      <div className="drug-item-info">
                        <p className="drug-item-name">{drug.name}</p>
                        <p className="drug-item-spec">{drug.spec}</p>
                      </div>
                      <div className="drug-item-arrow">→</div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-drug-list">
                  <div className="empty-drug-icon">📭</div>
                  <p className="empty-drug-text">您的药箱是空的</p>
                  <p className="empty-drug-hint">请先添加药品到药箱，或直接上传图片识别新药品</p>
                </div>
              )}
            </div>
            
            <div className="modal-footer">
              <button 
                className="btn btn-primary btn-large"
                onClick={() => {
                  setShowDrugList(false);
                  setActiveTab('upload');
                }}
              >
                📷 上传图片识别
              </button>
              {drugList.length > 0 && (
                <button 
                  className="btn btn-secondary btn-large"
                  onClick={() => setShowDrugList(false)}
                >
                  取消
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {showProfileEdit && (
        <ProfileEdit
          user={user}
          onSave={handleProfileUpdate}
          onClose={() => setShowProfileEdit(false)}
        />
      )}

      {showAddContact && (
        <EmergencyContacts
          contacts={emergencyContacts}
          onAdd={handleAddContact}
          onDelete={handleDeleteContact}
          onClose={() => setShowAddContact(false)}
          userId={user?.id}
        />
      )}

      {showAddDrugModal && (
        <AddDrugModal
          onClose={() => setShowAddDrugModal(false)}
          onAdd={handleAddDrug}
          userId={user?.userId}
        />
      )}

      {showConfirmDrugModal && pendingDrugInfo && (
        <ConfirmDrugModal
          onClose={() => setShowConfirmDrugModal(false)}
          onConfirm={confirmAddToMedicineBox}
          drugInfo={pendingDrugInfo}
          userId={user?.userId}
        />
      )}

      {/* 冲突报告卡片弹窗 - 放在顶层确保居中显示 */}
      {showConflictReport && conflictReport && (
        <div className="modal-overlay" onClick={() => setShowConflictReport(false)}>
          <div className="modal-content" ref={conflictReportRef} onClick={e => e.stopPropagation()} style={{ maxWidth: '600px', maxHeight: '80vh', overflowY: 'auto' }}>
            <div className="modal-header">
              <h3 className="modal-title">📋 用药冲突检测报告</h3>
              <button className="modal-close-btn" onClick={() => setShowConflictReport(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div style={{ marginBottom: '20px' }}>
                <p style={{ fontSize: '14px', color: '#666' }}>
                  检测时间: {new Date(conflictReport.checkTime).toLocaleString('zh-CN')}
                </p>
                <p style={{ fontSize: '14px', color: '#666' }}>
                  检测药品数: {conflictReport.drugsChecked?.length || 0} 种
                </p>
              </div>
              
              {/* 统计信息 */}
              <div style={{ 
                display: 'flex', 
                gap: '12px', 
                marginBottom: '20px',
                flexWrap: 'wrap'
              }}>
                <div style={{
                  padding: '8px 16px',
                  background: '#fee2e2',
                  borderRadius: '8px',
                  color: '#dc2626',
                  fontSize: '14px'
                }}>
                  🔴 严重: {conflictReport.statistics.severeCount}
                </div>
                <div style={{
                  padding: '8px 16px',
                  background: '#ffedd5',
                  borderRadius: '8px',
                  color: '#ea580c',
                  fontSize: '14px'
                }}>
                  🟡 中度: {conflictReport.statistics.moderateCount}
                </div>
                <div style={{
                  padding: '8px 16px',
                  background: '#fef9c3',
                  borderRadius: '8px',
                  color: '#ca8a04',
                  fontSize: '14px'
                }}>
                  🔵 轻微: {conflictReport.statistics.mildCount}
                </div>
              </div>

              {/* 冲突列表 */}
              {conflictReport.conflicts.map((conflict, index) => (
                <div key={index} style={{
                  padding: '16px',
                  background: conflict.severity === 'SEVERE' ? '#fee2e2' :
                             conflict.severity === 'MODERATE' ? '#ffedd5' : '#fef9c3',
                  borderRadius: '12px',
                  marginBottom: '12px'
                }}>
                  <div style={{ fontWeight: 'bold', marginBottom: '8px' }}>
                    {conflict.severity === 'SEVERE' && '🔴 严重冲突'}
                    {conflict.severity === 'MODERATE' && '🟡 中等冲突'}
                    {conflict.severity === 'MILD' && '🔵 轻微注意'}
                  </div>
                  <p style={{ fontSize: '14px', marginBottom: '4px' }}>
                    <strong>{conflict.drugA}</strong> ⚡ <strong>{conflict.drugB}</strong>
                  </p>
                  {conflict.conflictExplanation && (
                    <p style={{ fontSize: '13px', color: '#666', marginTop: '8px' }}>
                      {conflict.conflictExplanation}
                    </p>
                  )}
                  {conflict.riskWarning && (
                    <p style={{ 
                      fontSize: '13px', 
                      color: '#dc2626', 
                      fontWeight: 'bold',
                      marginTop: '8px' 
                    }}>
                      ⚠️ {conflict.riskWarning}
                    </p>
                  )}
                </div>
              ))}

              {/* 总体建议 */}
              {conflictReport.generalAdvice && (
                <div style={{
                  padding: '16px',
                  background: '#e0f2fe',
                  borderRadius: '12px',
                  marginTop: '20px'
                }}>
                  <h4 style={{ fontSize: '16px', marginBottom: '8px', color: '#0369a1' }}>
                    💊 总体建议
                  </h4>
                  <p style={{ fontSize: '14px', color: '#075985' }}>
                    {conflictReport.generalAdvice}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {particles.length > 0 && (
        <div className="particle-container">
          {particles.map(p => (
            <div
              key={p.id}
              className="particle"
              style={{
                left: p.x,
                top: p.y,
                background: p.color,
                '--tx': `${p.tx}px`,
                '--ty': `${p.ty}px`
              }}
            />
          ))}
        </div>
      )}

      {/* 新药入箱冲突检测结果弹窗 - 高危动作强制提示 */}
      {showConflictAlert && conflictAlertResult && (
        <div className="modal-overlay" onClick={() => setShowConflictAlert(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '520px' }}>
            <div className="modal-header" style={{ background: 'linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%)', borderRadius: '24px 24px 0 0' }}>
              <div style={{ textAlign: 'center', width: '100%', padding: '20px 0 10px' }}>
                <div style={{ fontSize: '56px', marginBottom: '8px' }}>⚠️</div>
                <h3 className="modal-title" style={{ color: '#e65100', fontSize: '24px' }}>
                  已为您检测冲突
                </h3>
              </div>
              <button className="modal-close-btn" onClick={() => setShowConflictAlert(false)} style={{ position: 'absolute', top: '16px', right: '16px' }}>✕</button>
            </div>
            <div className="modal-body" style={{ padding: '24px' }}>
              {/* 冲突统计 */}
              <div style={{ 
                background: 'linear-gradient(135deg, #ffebee 0%, #ffcdd2 100%)',
                borderRadius: '16px',
                padding: '20px',
                marginBottom: '20px',
                textAlign: 'center'
              }}>
                <div style={{ fontSize: '48px', fontWeight: 'bold', color: '#c62828' }}>
                  {conflictAlertResult.conflicts?.length || 0}
                </div>
                <div style={{ fontSize: '16px', color: '#c62828', fontWeight: '600' }}>
                  条警告
                </div>
              </div>
              
              {/* 统计信息 */}
              <div style={{ 
                display: 'flex', 
                gap: '10px', 
                marginBottom: '20px',
                flexWrap: 'wrap'
              }}>
                <div style={{
                  flex: 1,
                  minWidth: '80px',
                  padding: '10px 12px',
                  background: '#ffcdd2',
                  borderRadius: '10px',
                  color: '#b71c1c',
                  fontSize: '13px',
                  textAlign: 'center'
                }}>
                  🔴 严重: {conflictAlertResult.statistics?.severeCount || 0}
                </div>
                <div style={{
                  flex: 1,
                  minWidth: '80px',
                  padding: '10px 12px',
                  background: '#ffe0b2',
                  borderRadius: '10px',
                  color: '#e65100',
                  fontSize: '13px',
                  textAlign: 'center'
                }}>
                  🟡 中度: {conflictAlertResult.statistics?.moderateCount || 0}
                </div>
                <div style={{
                  flex: 1,
                  minWidth: '80px',
                  padding: '10px 12px',
                  background: '#fff9c4',
                  borderRadius: '10px',
                  color: '#f9a825',
                  fontSize: '13px',
                  textAlign: 'center'
                }}>
                  🔵 轻微: {conflictAlertResult.statistics?.mildCount || 0}
                </div>
              </div>

              {/* 冲突列表预览（只显示前2条） */}
              {conflictAlertResult.conflicts && conflictAlertResult.conflicts.length > 0 && (
                <div style={{ marginBottom: '20px' }}>
                  <h4 style={{ fontSize: '15px', color: '#333', marginBottom: '12px' }}>冲突详情：</h4>
                  {conflictAlertResult.conflicts.slice(0, 2).map((conflict, index) => (
                    <div key={index} style={{
                      padding: '14px',
                      background: conflict.severity === 'SEVERE' ? '#ffebee' :
                                 conflict.severity === 'MODERATE' ? '#fff3e0' : '#fffde7',
                      borderRadius: '12px',
                      marginBottom: '10px',
                      borderLeft: `4px solid ${
                        conflict.severity === 'SEVERE' ? '#f44336' :
                        conflict.severity === 'MODERATE' ? '#ff9800' : '#ffc107'
                      }`
                    }}>
                      <div style={{ fontWeight: 'bold', fontSize: '14px', marginBottom: '6px' }}>
                        {conflict.drugA} ⚡ {conflict.drugB}
                      </div>
                      {conflict.conflictExplanation && (
                        <p style={{ fontSize: '13px', color: '#666', margin: 0, lineHeight: '1.5' }}>
                          {conflict.conflictExplanation}
                        </p>
                      )}
                    </div>
                  ))}
                  {conflictAlertResult.conflicts.length > 2 && (
                    <p style={{ fontSize: '13px', color: '#999', textAlign: 'center', margin: '8px 0 0' }}>
                      还有 {conflictAlertResult.conflicts.length - 2} 条冲突未显示...
                    </p>
                  )}
                </div>
              )}
              
              {/* 操作按钮 */}
              <div style={{ display: 'flex', gap: '12px' }}>
                <button
                  onClick={() => {
                    setShowConflictAlert(false);
                    // 用户已知晓，清除弹窗结果，但保留重新检测标记
                  }}
                  style={{
                    flex: 1,
                    padding: '14px 20px',
                    fontSize: '15px',
                    fontWeight: '600',
                    border: '2px solid #e0e0e0',
                    borderRadius: '12px',
                    background: 'white',
                    color: '#666',
                    cursor: 'pointer'
                  }}
                >
                  我已知晓
                </button>
                <button
                  onClick={() => {
                    // 将冲突结果传递给冲突检测页面显示
                    if (conflictAlertResult) {
                      setConflictReport(conflictAlertResult);
                    }
                    setShowConflictAlert(false);
                    setConflictNeedsRecheck(false); // 清除重新检测标记
                    setActiveTab('conflict');
                  }}
                  style={{
                    flex: 1,
                    padding: '14px 20px',
                    fontSize: '15px',
                    fontWeight: '600',
                    border: 'none',
                    borderRadius: '12px',
                    background: 'linear-gradient(135deg, #ff9800 0%, #f57c00 100%)',
                    color: 'white',
                    cursor: 'pointer'
                  }}
                >
                  查看全部
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 过期药品弹窗 */}
      {showExpiringDrugsModal && (
        <div className="modal-overlay" onClick={handleCloseExpiringDrugsModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h3 className="modal-title">⚠️ 药品过期提醒</h3>
              <button className="modal-close-btn" onClick={handleCloseExpiringDrugsModal}>✕</button>
            </div>
            <div className="modal-body">
              {(() => {
                const { expiredDrugs, expiringDrugs } = expiringDrugsResult;
                return (
                  <div>
                    {expiredDrugs.length > 0 && (
                      <div style={{ marginBottom: '20px' }}>
                        <h4 style={{ color: '#e74c3c', marginBottom: '12px', fontSize: '16px' }}>
                          🔴 已过期药品（{expiredDrugs.length}种）
                        </h4>
                        {expiredDrugs.map((drug, index) => (
                          <div
                            key={index}
                            className="drug-item"
                            onClick={() => handleExpiringDrugClick(drug)}
                            style={{
                              padding: '12px',
                              borderBottom: '1px solid #eee',
                              cursor: 'pointer',
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center'
                            }}
                          >
                            <div>
                              <span style={{ fontWeight: 'bold', color: '#333' }}>
                                {drug.name}
                                {drug.tradeName || drug.commonName ? `（${drug.tradeName || drug.commonName}）` : ''}
                              </span>
                              <span style={{ color: '#e74c3c', fontSize: '14px', marginLeft: '8px' }}>
                                已过期{Math.abs(drug.daysUntilExpiry)}天
                              </span>
                            </div>
                            <span style={{ color: '#999', fontSize: '12px' }}>点击查看详情 →</span>
                          </div>
                        ))}
                      </div>
                    )}
                    {expiringDrugs.length > 0 && (
                      <div>
                        <h4 style={{ color: '#f39c12', marginBottom: '12px', fontSize: '16px' }}>
                          🟡 即将过期药品（{expiringDrugs.length}种）
                        </h4>
                        {expiringDrugs.map((drug, index) => (
                          <div
                            key={index}
                            className="drug-item"
                            onClick={() => handleExpiringDrugClick(drug)}
                            style={{
                              padding: '12px',
                              borderBottom: '1px solid #eee',
                              cursor: 'pointer',
                              display: 'flex',
                              justifyContent: 'space-between',
                              alignItems: 'center'
                            }}
                          >
                            <div>
                              <span style={{ fontWeight: 'bold', color: '#333' }}>
                                {drug.name}
                                {drug.tradeName || drug.commonName ? `（${drug.tradeName || drug.commonName}）` : ''}
                              </span>
                              <span style={{ color: '#f39c12', fontSize: '14px', marginLeft: '8px' }}>
                                {drug.daysUntilExpiry === 0 ? '今天过期' : `还有${drug.daysUntilExpiry}天过期`}
                              </span>
                            </div>
                            <span style={{ color: '#999', fontSize: '12px' }}>点击查看详情 →</span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })()}
            </div>
          </div>
        </div>
      )}

      {/* 添加药品时发现过期的弹窗 */}
      {showExpiredDrugModal && expiredDrugInfo && (
        <div className="modal-overlay" onClick={handleCloseExpiredDrugModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '450px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: '#ef4444' }}>️ 药品过期提示</h3>
              <button className="modal-close-btn" onClick={handleCloseExpiredDrugModal}>✕</button>
            </div>
            <div className="modal-body" style={{ textAlign: 'center', padding: '30px 20px' }}>
              <div style={{ 
                fontSize: '64px', 
                marginBottom: '20px',
                lineHeight: '1'
              }}>
                🗑️
              </div>
              <div style={{ marginBottom: '20px' }}>
                <p style={{ 
                  fontSize: '18px', 
                  fontWeight: '600', 
                  color: '#1f2937',
                  marginBottom: '12px'
                }}>
                  药品添加失败
                </p>
                <p style={{ 
                  fontSize: '15px', 
                  color: '#6b7280',
                  lineHeight: '1.6'
                }}>
                  您添加的 <span style={{ fontWeight: '600', color: '#ef4444' }}>{expiredDrugInfo.drugName}</span> 已过期
                </p>
                <p style={{ 
                  fontSize: '14px', 
                  color: '#9ca3af',
                  marginTop: '8px'
                }}>
                  有效期：{expiredDrugInfo.expiryDate}
                </p>
              </div>
              <div style={{ 
                background: '#fef2f2', 
                borderLeft: '4px solid #ef4444',
                padding: '12px 16px',
                borderRadius: '8px',
                marginBottom: '20px'
              }}>
                <p style={{ 
                  fontSize: '14px', 
                  color: '#991b1b',
                  margin: 0
                }}>
                  该药品已自动删除，请勿使用过期药品
                </p>
              </div>
              <button 
                className="btn btn-large" 
                onClick={handleCloseExpiredDrugModal}
                style={{ 
                  width: '100%',
                  background: '#ef4444',
                  color: 'white',
                  border: 'none',
                  borderRadius: '12px',
                  padding: '14px 20px',
                  fontSize: '16px',
                  fontWeight: '600',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#dc2626';
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(239, 68, 68, 0.4)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = '#ef4444';
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 2px 8px rgba(239, 68, 68, 0.3)';
                }}
              >
                我知道了
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 已过期且未丢弃药品弹窗 */}
      {showTodayExpiredModal && todayExpiredDrugs.length > 0 && (
        <div className="modal-overlay" onClick={handleCloseTodayExpiredModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '550px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: '#ef4444' }}>⚠️ 药品过期提醒</h3>
              <button className="modal-close-btn" onClick={handleCloseTodayExpiredModal}>✕</button>
            </div>
            <div className="modal-body" style={{ padding: '24px 20px' }}>
              <div style={{ textAlign: 'center', marginBottom: '20px' }}>
                <div style={{ 
                  fontSize: '64px', 
                  marginBottom: '16px',
                  lineHeight: '1'
                }}>
                  🗑️
                </div>
                <p style={{ 
                  fontSize: '17px', 
                  fontWeight: '600', 
                  color: '#1f2937',
                  marginBottom: '8px'
                }}>
                  检测到 {todayExpiredDrugs.length} 个药品已过期
                </p>
                <p style={{ 
                  fontSize: '14px', 
                  color: '#6b7280'
                }}>
                  这些药品仍在药箱中显示，请及时处理
                </p>
              </div>
              
              {/* 过期药品列表 */}
              <div style={{ 
                background: '#fef2f2', 
                borderRadius: '12px',
                padding: '16px',
                marginBottom: '20px',
                maxHeight: '300px',
                overflowY: 'auto'
              }}>
                <p style={{ 
                  fontSize: '14px', 
                  fontWeight: '600',
                  color: '#991b1b',
                  marginBottom: '12px'
                }}>
                  📋 过期药品清单：
                </p>
                {todayExpiredDrugs.map((drug, index) => (
                  <div key={drug.boxItemId || index} style={{
                    padding: '10px 12px',
                    marginBottom: index < todayExpiredDrugs.length - 1 ? '8px' : '0',
                    background: 'white',
                    borderRadius: '8px',
                    borderLeft: '4px solid #ef4444'
                  }}>
                    <p style={{ 
                      fontSize: '15px', 
                      fontWeight: '600', 
                      color: '#1f2937',
                      margin: '0 0 4px 0'
                    }}>
                      💊 {drug.drugName || drug.name || '未知药品'}
                    </p>
                    <p style={{ 
                      fontSize: '13px', 
                      color: '#9ca3af',
                      margin: 0
                    }}>
                      有效期至：{drug.expiryDate}
                    </p>
                  </div>
                ))}
              </div>
              
              <div style={{ 
                background: '#fffbeb', 
                borderLeft: '4px solid #f59e0b',
                padding: '12px 16px',
                borderRadius: '8px'
              }}>
                <p style={{ 
                  fontSize: '14px', 
                  color: '#92400e',
                  margin: 0,
                  lineHeight: '1.6'
                }}>
                  💡 温馨提示：<br/>
                  过期药品可能失效或产生有害物质，请勿继续使用。<br/>
                  您可以在药箱中找到这些药品，点击“我已丢弃”按钮进行清理。
                </p>
              </div>
              
              <button 
                className="btn btn-large" 
                onClick={handleCloseTodayExpiredModal}
                style={{ 
                  width: '100%',
                  marginTop: '20px',
                  background: '#ef4444',
                  color: 'white',
                  border: 'none',
                  borderRadius: '12px',
                  padding: '14px 20px',
                  fontSize: '16px',
                  fontWeight: '600',
                  cursor: 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#dc2626';
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = '0 4px 12px rgba(239, 68, 68, 0.4)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = '#ef4444';
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0 2px 8px rgba(239, 68, 68, 0.3)';
                }}
              >
                我知道了
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 药品详情弹窗 */}
      {showDrugDetailModal && selectedDrug && (
        <div className="modal-overlay" onClick={handleCloseDrugDetail}>
          <div className="modal-content drug-detail-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">💊 药品详情</h3>
              <button className="modal-close-btn" onClick={handleCloseDrugDetail}>✕</button>
            </div>

            <div className="modal-body">
              <div className="drug-detail-info">
                <div className="detail-item">
                  <label className="detail-label">药品名称</label>
                  <p className="detail-value">
                    {selectedDrug.name}
                    {(() => {
                      const otherNames = [];
                      if (selectedDrug.tradeName) otherNames.push(selectedDrug.tradeName);
                      if (selectedDrug.commonName) otherNames.push(selectedDrug.commonName);
                      return otherNames.length > 0 ? `（${otherNames.join('，')}）` : '';
                    })()}
                  </p>
                </div>
                <div className="detail-item">
                  <label className="detail-label">规格</label>
                  <p className="detail-value">{selectedDrug.spec || '-'}</p>
                </div>
                <div className="detail-item">
                  <label className="detail-label">用法用量</label>
                  <p className="detail-value">{selectedDrug.dosage || '-'}</p>
                </div>
                <div className="detail-item">
                  <label className="detail-label">用药频率</label>
                  <p className="detail-value">{selectedDrug.frequency || '-'}</p>
                </div>
                <div className="detail-item">
                  <label className="detail-label">库存数量</label>
                  <p className="detail-value">
                    剩余 {selectedDrug.remaining || 0} / {selectedDrug.totalQuantity || 0} 片
                  </p>
                </div>
                <div className="detail-item">
                  <label className="detail-label">有效期</label>
                  <p className="detail-value">{selectedDrug.expiryDate || '-'}</p>
                </div>
                {selectedDrug.startDate && (
                  <div className="detail-item">
                    <label className="detail-label">开始日期</label>
                    <p className="detail-value">{selectedDrug.startDate}</p>
                  </div>
                )}
                {selectedDrug.endDate && (
                  <div className="detail-item">
                    <label className="detail-label">结束日期</label>
                    <p className="detail-value">{selectedDrug.endDate}</p>
                  </div>
                )}
                {selectedDrug.note && (
                  <div className="detail-item">
                    <label className="detail-label">备注</label>
                    <p className="detail-value">{selectedDrug.note}</p>
                  </div>
                )}
              </div>
            </div>

            <div className="modal-footer" style={{
              display: 'flex',
              gap: '12px',
              justifyContent: 'center',
              padding: '20px 0 24px 0',
              borderTop: '1px solid #e5e7eb',
              marginTop: '20px'
            }}>
              {(() => {
                // 判断药品是否已过期
                if (!selectedDrug.expiryDate) return null;
                const today = new Date();
                today.setHours(0, 0, 0, 0);
                const expiryDate = new Date(selectedDrug.expiryDate);
                expiryDate.setHours(0, 0, 0, 0);
                const daysUntilExpiry = Math.ceil((expiryDate - today) / (1000 * 60 * 60 * 24));
                const isExpired = daysUntilExpiry < 0;
                            
                // 已过期药品不显示修改和删除按钮
                if (isExpired) return null;
                            
                return (
                  <>
                    <button 
                      className="btn btn-large" 
                      onClick={() => handleEditDrug(selectedDrug)}
                      style={{ 
                        flex: '1 1 0',
                        background: '#f59e0b',
                        color: 'white',
                        border: 'none',
                        borderRadius: '12px',
                        padding: '14px 20px',
                        fontSize: '15px',
                        fontWeight: '600',
                        cursor: 'pointer',
                        transition: 'all 0.2s ease',
                        boxShadow: '0 2px 8px rgba(245, 158, 11, 0.3)'
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.background = '#d97706';
                        e.currentTarget.style.transform = 'translateY(-2px)';
                        e.currentTarget.style.boxShadow = '0 4px 12px rgba(245, 158, 11, 0.4)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = '#f59e0b';
                        e.currentTarget.style.transform = 'translateY(0)';
                        e.currentTarget.style.boxShadow = '0 2px 8px rgba(245, 158, 11, 0.3)';
                      }}
                    >
                      ️ 修改
                    </button>
                    <button 
                      className="btn btn-large" 
                      onClick={() => handleDeleteDrug(selectedDrug)}
                      style={{ 
                        flex: '1 1 0',
                        background: '#ef4444',
                        color: 'white',
                        border: 'none',
                        borderRadius: '12px',
                        padding: '14px 20px',
                        fontSize: '15px',
                        fontWeight: '600',
                        cursor: 'pointer',
                        transition: 'all 0.2s ease',
                        boxShadow: '0 2px 8px rgba(239, 68, 68, 0.3)'
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.background = '#dc2626';
                        e.currentTarget.style.transform = 'translateY(-2px)';
                        e.currentTarget.style.boxShadow = '0 4px 12px rgba(239, 68, 68, 0.4)';
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.background = '#ef4444';
                        e.currentTarget.style.transform = 'translateY(0)';
                        e.currentTarget.style.boxShadow = '0 2px 8px rgba(239, 68, 68, 0.3)';
                      }}
                    >
                      🗑️ 删除
                    </button>
                  </>
                );
              })()}
            </div>
          </div>
        </div>
      )}

      {/* 编辑药品弹窗 */}
      {showEditDrugModal && selectedDrug && (
        <EditDrugModal
          onClose={() => {
            setShowEditDrugModal(false);
            setSelectedDrug(null);
          }}
          onSave={handleSaveEditDrug}
          drug={selectedDrug}
          userId={user?.userId}
        />
      )}

      {/* 确认删除弹窗 */}
      {showConfirmDelete && pendingDeleteDrug && (
        <ConfirmDeleteModal
          drugName={pendingDeleteDrug.name}
          onConfirm={handleConfirmDelete}
          onCancel={handleCancelDelete}
        />
      )}

      {/* 添加到用药日历弹窗 */}
      {showAddToPlanModal && selectedDrugForPlan && (
        <AddToPlanModal
          drug={selectedDrugForPlan}
          onClose={handleCloseAddToPlanModal}
          onSubmit={handleSubmitAddToPlan}
        />
      )}

      {/* 用药提醒弹窗 */}
      {showMedicationReminder && missedReminders.length > 0 && (
        <MedicationReminderModal
          reminders={missedReminders}
          onClose={handleCloseMedicationReminder}
          onMarkAsTaken={handleMarkAsTakenFromReminder}
        />
      )}

      {/* 隐藏的截图容器 - 用于完整截图冲突报告 */}
      {showScreenshotContainer && conflictReport && (
        <div
          ref={screenshotContainerRef}
          style={{
            position: 'absolute',
            left: '-9999px',
            top: '0',
            width: '600px',
            background: '#ffffff',
            padding: '40px',
            fontFamily: 'inherit'
          }}
        >
          {/* 标题 */}
          <div style={{ textAlign: 'center', marginBottom: '30px', borderBottom: '3px solid #e5e7eb', paddingBottom: '20px' }}>
            <h2 style={{ fontSize: '28px', margin: '0 0 10px 0', color: '#1f2937' }}> 用药冲突检测报告</h2>
          </div>

          {/* 基本信息 */}
          <div style={{ marginBottom: '24px' }}>
            <p style={{ fontSize: '14px', color: '#6b7280', marginBottom: '6px' }}>
              检测时间: {new Date(conflictReport.checkTime).toLocaleString('zh-CN')}
            </p>
            <p style={{ fontSize: '14px', color: '#6b7280', marginBottom: '0' }}>
              检测药品数: {conflictReport.drugsChecked?.length || 0} 种
            </p>
          </div>
          
          {/* 统计信息 */}
          <div style={{ 
            display: 'flex', 
            gap: '12px', 
            marginBottom: '24px',
            flexWrap: 'wrap'
          }}>
            <div style={{
              padding: '10px 18px',
              background: '#fee2e2',
              borderRadius: '10px',
              color: '#dc2626',
              fontSize: '15px',
              fontWeight: '600'
            }}>
               严重: {conflictReport.statistics.severeCount}
            </div>
            <div style={{
              padding: '10px 18px',
              background: '#ffedd5',
              borderRadius: '10px',
              color: '#ea580c',
              fontSize: '15px',
              fontWeight: '600'
            }}>
              🟡 中度: {conflictReport.statistics.moderateCount}
            </div>
            <div style={{
              padding: '10px 18px',
              background: '#fef9c3',
              borderRadius: '10px',
              color: '#ca8a04',
              fontSize: '15px',
              fontWeight: '600'
            }}>
               轻微: {conflictReport.statistics.mildCount}
            </div>
          </div>

          {/* 冲突列表 */}
          {conflictReport.conflicts.map((conflict, index) => (
            <div key={index} style={{
              padding: '18px',
              background: conflict.severity === 'SEVERE' ? '#fee2e2' :
                         conflict.severity === 'MODERATE' ? '#ffedd5' : '#fef9c3',
              borderRadius: '14px',
              marginBottom: '16px',
              border: `2px solid ${
                conflict.severity === 'SEVERE' ? '#fecaca' :
                conflict.severity === 'MODERATE' ? '#fed7aa' : '#fde68a'
              }`
            }}>
              <div style={{ fontWeight: 'bold', marginBottom: '10px', fontSize: '16px' }}>
                {conflict.severity === 'SEVERE' && '🔴 严重冲突'}
                {conflict.severity === 'MODERATE' && ' 中等冲突'}
                {conflict.severity === 'MILD' && '🔵 轻微注意'}
              </div>
              <p style={{ fontSize: '15px', marginBottom: '8px' }}>
                <strong>{conflict.drugA}</strong> ⚡ <strong>{conflict.drugB}</strong>
              </p>
              {conflict.conflictExplanation && (
                <p style={{ fontSize: '14px', color: '#4b5563', marginTop: '10px', lineHeight: '1.6' }}>
                  {conflict.conflictExplanation}
                </p>
              )}
              {conflict.riskWarning && (
                <p style={{ 
                  fontSize: '14px', 
                  color: '#dc2626', 
                  fontWeight: 'bold',
                  marginTop: '10px'
                }}>
                  ⚠️ {conflict.riskWarning}
                </p>
              )}
            </div>
          ))}

          {/* 总体建议 */}
          {conflictReport.generalAdvice && (
            <div style={{
              padding: '18px',
              background: '#e0f2fe',
              borderRadius: '14px',
              marginTop: '24px',
              border: '2px solid #bae6fd'
            }}>
              <h4 style={{ fontSize: '17px', marginBottom: '10px', color: '#0369a1', margin: '0 0 10px 0' }}>
                💊 总体建议
              </h4>
              <p style={{ fontSize: '14px', color: '#075985', lineHeight: '1.6', margin: 0 }}>
                {conflictReport.generalAdvice}
              </p>
            </div>
          )}
        </div>
      )}
    </>
  );
}

export default App;
