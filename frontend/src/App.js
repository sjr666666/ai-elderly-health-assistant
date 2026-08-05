import React, { useState, useRef, useEffect, useMemo } from 'react';
import html2canvas from 'html2canvas';
import DOMPurify from 'dompurify';
import './App.css';
import AuthGate from './components/AuthGate';
import ProfileModal from './components/ProfileModal';
import ProfileEdit from './components/ProfileEdit';
import EmergencyContacts from './components/EmergencyContacts';
import MyGuardiansModal from './components/MyGuardiansModal';
import AddDrugModal from './components/AddDrugModal';
import EditDrugModal from './components/EditDrugModal';
import ConfirmDeleteModal from './components/ConfirmDeleteModal';
import ManualDrugSearch from './components/ManualDrugSearch';
import AddToPlanModal from './components/AddToPlanModal';
import ConfirmDrugModal from './components/ConfirmDrugModal';
import MedicationReminderModal from './components/MedicationReminderModal';
import DrugManagementTab from './components/DrugManagementTab';
import DailyLessonCard from './components/DailyLessonCard';
import GuardianApp from './components/guardian/GuardianApp';
import ElderNotificationPanel from './components/ElderNotificationPanel';
import WeeklyReport from './components/WeeklyReport';
import EmergencyTab from './components/EmergencyTab';
import { useToast } from './components/Toast';
import FloatingMicButton from './components/FloatingMicButton';
import { clearAuth, getToken } from './utils/elderApi';
import { formatDateTime } from './utils/timeUtils';
import { useTTS } from './hooks/useTTS';
import RecognitionHistoryModal from './components/RecognitionHistoryModal';

function App() {
  const { showToast } = useToast();

  // 语音播报 Hook（百度TTS + 浏览器原生语音）
  const {
    isSpeaking, speechRate, setSpeechRate,
    isFollowUpSpeaking, speakingFollowUpIdx,
    audioRef, followUpAudioRef, followUpMessagesRef, speakRef,
    setAuthFetch,
    speak, stopSpeaking, stopFollowUpSpeaking, toggleFollowUpSpeech,
  } = useTTS({ showToast });
  const loadWeeklyMedicationRef = useRef(null);
  const recognitionHistoryModalRef = useRef(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [registerSuccess, setRegisterSuccess] = useState(''); // 注册成功提示，返回登录页时展示
  const [loginMode, setLoginMode] = useState('elder'); // 'elder' | 'guardian'
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
  const [showWeeklyReport, setShowWeeklyReport] = useState(false); // AI周报显示状态
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
  const [showMyGuardians, setShowMyGuardians] = useState(false);
  const [showAddDrugModal, setShowAddDrugModal] = useState(false);
  const [showEditDrugModal, setShowEditDrugModal] = useState(false); // 编辑药品弹窗
  const [showConfirmDelete, setShowConfirmDelete] = useState(false); // 确认删除弹窗
  const [pendingDeleteDrug, setPendingDeleteDrug] = useState(null); // 待删除的药品
  const [showConfirmDrugModal, setShowConfirmDrugModal] = useState(false); // 确认药品弹窗
  const [pendingDrugInfo, setPendingDrugInfo] = useState(null); // 待确认的药品信息
  const [showDrugDetailModal, setShowDrugDetailModal] = useState(false); // 药品详情弹窗
  const [selectedDrug, setSelectedDrug] = useState(null); // 选中的药品
  const [, setDrugsWithPlan] = useState(new Set()); // 已设置用药计划的药品ID集合
  const [showExpiringDrugsModal, setShowExpiringDrugsModal] = useState(false); // 过期药品弹窗
  const [showExpiredDrugModal, setShowExpiredDrugModal] = useState(false); // 添加药品时发现过期的弹窗
  const [expiredDrugInfo, setExpiredDrugInfo] = useState(null); // 过期药品信息
  const [showTodayExpiredModal, setShowTodayExpiredModal] = useState(false); // 已过期且未丢弃药品弹窗
  const [todayExpiredDrugs, setTodayExpiredDrugs] = useState([]); // 已过期且未丢弃药品列表
  const [showUndoConfirmModal, setShowUndoConfirmModal] = useState(false); // 撤销确认弹窗
  const [pendingUndoId, setPendingUndoId] = useState(null); // 待撤销的计划ID
  const [showMedicationReminder, setShowMedicationReminder] = useState(false); // 用药提醒弹窗
  const [missedReminders, setMissedReminders] = useState([]); // 超时未服用的用药计划
  const lastReminderTimeRef = useRef(null); // 上次弹窗提醒的时间，避免频繁提醒
  const lastShownStageRef = useRef(null); // 上次已展示的最高阶段，只有阶段升级才再次提醒
  // 获取本地日期 key（YYYY-MM-DD），避免 toISOString() 返回 UTC 日期导致凌晨跨日判断错误
  const getLocalDateKey = () => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  };
  const lastDateRef = useRef(getLocalDateKey()); // 跨日检测：记录当前日期(YYYY-MM-DD)
  const loadCalendarPlansRef = useRef(null); // 跨日检测：持有最新的 loadCalendarPlans 引用，避免闭包陈旧
  
  // 药品冲突检测相关状态
  const [conflictReport, setConflictReport] = useState(null); // 冲突检测报告
  const [isCheckingConflicts, setIsCheckingConflicts] = useState(false); // 是否正在检测冲突
  const [conflictError, setConflictError] = useState(null); // 冲突检测错误
  const [showConflictReport, setShowConflictReport] = useState(false); // 是否显示冲突报告卡片

  // 综合冲突场景化选项状态
  const [selectedScenarios, setSelectedScenarios] = useState([]); // 已选场景标签
  const [customFoodInput, setCustomFoodInput] = useState(''); // 自定义食物输入
  const [scenarioConflictReport, setScenarioConflictReport] = useState(null); // 综合冲突报告
  const [isCheckingScenario, setIsCheckingScenario] = useState(false); // 综合冲突检测中
  const [showScenarioPanel, setShowScenarioPanel] = useState(false); // 综合冲突面板折叠状态
  const [showOriginalText, setShowOriginalText] = useState(false); // 医学原文折叠状态
  
  // 新药入箱冲突检测弹窗相关状态
  const [showConflictAlert, setShowConflictAlert] = useState(false); // 新药入箱冲突检测结果弹窗
  const [conflictAlertResult, setConflictAlertResult] = useState(null); // 新药入箱冲突检测结果
  const [conflictNeedsRecheck, setConflictNeedsRecheck] = useState(false); // 冲突检测页面是否需要重新检测

  // 自动快速检测相关状态（进入页面时自动本地规则检测）
  const [autoCheckResult, setAutoCheckResult] = useState(null); // 自动快速检测结果
  const [isAutoChecking, setIsAutoChecking] = useState(false); // 是否正在自动检测
  
  // 批量识别相关状态
  const [batchRecognizeItems, setBatchRecognizeItems] = useState([]); // 批量识别的图片列表
  const [batchSelectedForAdd, setBatchSelectedForAdd] = useState(new Set()); // 选中的要添加的药品
  const [isBatchAdding, setIsBatchAdding] = useState(false); // 是否正在添加到药箱
  const batchFileInputRef = useRef(null); // 批量文件输入引用
  
  // 批量确认弹窗相关状态
  const [showBatchConfirmModal, setShowBatchConfirmModal] = useState(false); // 批量确认弹窗
  const [batchDrugIndex, setBatchDrugIndex] = useState(0); // 当前正在确认的药品索引
  const [batchConfirmedDrugs, setBatchConfirmedDrugs] = useState([]); // 已确认的药品信息列表
  const [, setIsBatchAddingAll] = useState(false); // 是否正在批量添加所有药品
  const batchConfirmModalRef = useRef(null); // 批量确认弹窗容器引用
  
  const fileInputRef = useRef(null);
  const conflictReportRef = useRef(null); // 冲突报告卡片引用（用于弹窗显示）
  const screenshotContainerRef = useRef(null); // 隐藏的截图容器引用
  const [showScreenshotContainer, setShowScreenshotContainer] = useState(false); // 控制隐藏截图容器显示

  // 今日一课相关状态
  const [dailyLesson, setDailyLesson] = useState(null);
  const [dailyLessonLoading, setDailyLessonLoading] = useState(false);

  // 缺药预警相关状态
  const [shortageWarnings, setShortageWarnings] = useState([]); // 缺药预警列表
  const [showShortageDetail, setShowShortageDetail] = useState(false); // 缺药预警详情弹窗

  // 老人端通知相关状态
  const [showNotificationPanel, setShowNotificationPanel] = useState(false);
  const [notificationUnreadCount, setNotificationUnreadCount] = useState(0);
  const [wsConnected, setWsConnected] = useState(false); // WebSocket连接状态
  const wsRef = useRef(null); // WebSocket引用

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

  const handleRegister = (registerData, successMsg) => {
    setShowRegister(false);
    // 注册成功后返回登录页，不自动登录；successMsg 用于在登录页展示成功提示
    if (successMsg) {
      setRegisterSuccess(successMsg);
    }
  };

  // 带认证的fetch helper - 自动添加JWT token到请求头
  const authFetch = async (url, options = {}) => {
    const token = getToken();
    const headers = {
      ...options.headers,
      'Content-Type': 'application/json;charset=UTF-8',
      'Accept': 'application/json',
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    const controller = options.signal ? null : new AbortController();
    const timeoutId = controller ? window.setTimeout(() => controller.abort(), 15000) : null;
    let response;
    try {
      response = await fetch(url, {
        ...options,
        headers,
        ...(controller ? { signal: controller.signal } : {})
      });
    } finally {
      if (timeoutId) window.clearTimeout(timeoutId);
    }
    const data = await response.json();
    // 处理401认证失败
    if (response.status === 401 || data.code === 401 || data.message?.includes('Access Denied')) {
      clearAuth();
      window.dispatchEvent(new CustomEvent('elder-auth-expired'));
      throw new Error('认证已过期');
    }
    return { response, data };
  };

  // 将 authFetch 注入 useTTS（hook 在组件顶部声明，此处动态绑定避免顺序耦合）
  setAuthFetch(authFetch);

  // 从数据库加载药箱列表
  const loadMedicineBoxList = async (userId) => {
    if (!userId) return;
    
    try {
      const { response, data } = await authFetch(`/api/v1/box/list`);
      
      
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

  // 加载缺药预警列表
  const loadShortageWarnings = async (userId) => {
    if (!userId) return;
    try {
      const { response, data } = await authFetch(`/api/v1/box/shortage-warnings`);
      if (response.ok && data.code === 200) {
        setShortageWarnings(data.data || []);
      } else {
        console.error('获取缺药预警失败:', data.message);
      }
    } catch (err) {
      console.error('获取缺药预警异常:', err);
    }
  };

  // 加载今日一课
  const fetchDailyLesson = async (overrideUserId) => {
    const userId = overrideUserId || user?.id;
    if (!userId) return;
    setDailyLessonLoading(true);
    try {
      const { response, data } = await authFetch(`/api/v1/daily-lesson/today?userId=${userId}`);
      if (response.ok && data.code === 200) {
        setDailyLesson(data.data);
      } else {
        console.error('获取今日一课失败:', data.message);
      }
    } catch (err) {
      console.error('获取今日一课异常:', err);
    } finally {
      setDailyLessonLoading(false);
    }
  };

  // 重新生成本日一课（换一篇）
  const handleDailyLessonRefresh = async () => {
    if (!user?.id) return;
    try {
      const { response, data } = await authFetch(`/api/v1/daily-lesson/regenerate?userId=${user.id}`, { method: 'POST' });
      if (response.ok && data.code === 200) {
        setDailyLesson(data.data);
      } else {
        console.error('重新生成今日一课失败:', data.message);
      }
    } catch (err) {
      console.error('重新生成今日一课异常:', err);
    }
  };

  // 从数据库加载紧急联系人列表
  const loadEmergencyContacts = async (elderId) => {
    if (!elderId) return;

    try {
      const { response, data } = await authFetch(`/api/emergency/v1/contacts?elderId=${elderId}`);


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
        let { plans } = JSON.parse(cached);
        
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
  // userIdOverride: 可选，登录时直接传入 userId，避免等待 setUser 异步更新
  // This loader is intentionally recreated with the current auth state.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const loadCalendarPlans = async (userIdOverride) => {
    const effectiveUserId = userIdOverride || (user && user.userId);
    if (!effectiveUserId) {
      console.warn('用户未登录，无法加载用药计划');
      setCalendarPlans([]);
      setIsLoadingCalendar(false);
      return;
    }
    
    setIsLoadingCalendar(true);
    
    try {
      const { response, data } = await authFetch(`/api/v1/plan/generate-today?userId=${effectiveUserId}`);
      
      
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
          boxDrugName: item.boxDrugName,
          remindBefore: item.remindBefore,
          reminderStage: item.reminderStage || 'none'
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
        
        // 保存到本地缓存（用于断网可读）
        try {
          localStorage.setItem(TODAY_PLANS_CACHE_KEY, JSON.stringify({
            plans: validPlans,
            timestamp: Date.now()
          }));
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

  // 保持 ref 始终指向最新的 loadCalendarPlans，供跨日检测使用
  loadCalendarPlansRef.current = loadCalendarPlans;

  // 从后端加载一周用药记录（包括已删除但在查询范围内的记录）
  // This loader is intentionally recreated with the current auth state.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const loadWeeklyMedication = async () => {
    if (!user) {
      console.warn('用户未登录，无法加载用药记录');
      setWeeklyMedicationData(null);
      return;
    }

    setIsLoadingCalendar(true);

    try {
      const { response, data } = await authFetch(`/api/v1/plan/weekly`);


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
      const { response, data } = await authFetch('/api/v1/plan/add-from-box', {
        method: 'POST',
        body: JSON.stringify({
          boxItemId: selectedDrugForPlan.boxItemId,
          timeSlots: selectedTimeSlots
        })
      });


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
    setRegisterSuccess(''); // 登录成功后清除注册成功提示

    // 用户信息仅保存在React state中，不存localStorage

    // 家属角色直接跳转家属端，不加载老人端数据
    if (loginData.role === 'family') {
      return;
    }

    // 登录成功后加载药箱列表和紧急联系人
    if (loginData.userId) {
      loadMedicineBoxList(loginData.userId);
      loadShortageWarnings(loginData.userId);
      // 登录后立即加载今日用药计划，传入userId避免等待setUser异步更新
      loadCalendarPlans(loginData.userId);

      // 检查已过期且未丢弃的药品
      setTimeout(() => {
        checkTodayExpiredMedicines(loginData.userId);
      }, 500);
    }
    if (loginData.id) {
      loadEmergencyContacts(loginData.id);
    }

    // 加载今日一课
    if (loginData.role !== 'family' && loginData.id) {
      fetchDailyLesson(loginData.id);
    }

    if (loginData.needProfile) {
      setShowProfileModal(true);
    }
  };

  const handleProfileComplete = (profileData) => {
    setUser(prev => ({ ...prev, ...profileData }));
    setShowProfileModal(false);
  };

  const handleProfileUpdate = (profileData) => {
    setUser(prev => ({ ...prev, ...profileData }));
    setShowProfileEdit(false);

    // 显示成功提示弹窗
    showToast('个人信息已更新！', 'success');
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
        const { data: result } = await authFetch(`/api/emergency/v1/contacts/${contactId}`, {
          method: 'DELETE'
        });
        
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
      loadShortageWarnings(user.userId);
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
      const { response, data } = await authFetch(`/api/v1/box/${pendingDeleteDrug.boxItemId}`, { method: 'DELETE' });
      
      
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
      const { data } = await authFetch('/api/conflict/check', {
        method: 'POST',
        body: JSON.stringify(drugNames)
      });
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
      loadShortageWarnings(user.userId);
      
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
  // This handler closes over the current reminder list.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const handleCloseMedicationReminder = () => {
    // 关闭弹窗时自动停止播报
    stopSpeaking();
    // 重置提醒ID，允许下次打开时重新播报
    lastReminderIdRef.current = null;
    // 记录当前已展示的最高阶段，下次只有阶段升级才再次提醒
    // （避免关闭 pre_remind 后1分钟又弹同一阶段）
    if (missedReminders.length > 0) {
      const stageOrder = ['pre_remind', 'due_now', 'overdue', 'notify_family'];
      let highestIdx = -1;
      missedReminders.forEach(r => {
        const idx = stageOrder.indexOf(r.reminderStage);
        if (idx > highestIdx) highestIdx = idx;
      });
      if (highestIdx >= 0) {
        lastShownStageRef.current = stageOrder[highestIdx];
      }
    }
    setShowMedicationReminder(false);
    setMissedReminders([]);
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
      
      
      // 如果还有未服用的药物，重新打开弹窗
      if (remainingMissed.length > 0) {
        setMissedReminders(remainingMissed);
        setShowMedicationReminder(true);
        lastReminderTimeRef.current = Date.now();
      } else {
        // 所有药物都已服用，关闭弹窗
        // 重置阶段记录，下次有新药品时从 pre_remind 开始正常提醒
        lastShownStageRef.current = null;
        handleCloseMedicationReminder();
      }
    }
  }, [calendarPlans, shouldRefreshReminder, handleCloseMedicationReminder]);

  const handleMarkAsTakenFromReminder = async (reminder) => {

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
    
    // 清除登录状态和JWT token
    clearAuth();
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
      const { response, data } = await authFetch(`/api/v1/box/expired/today`);
      
      
      if (response.ok && data.code === 200 && data.data && data.data.length > 0) {
        // 有已过期且未丢弃的药品，显示弹窗
        setTodayExpiredDrugs(data.data);
        setShowTodayExpiredModal(true);
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
  // eslint-disable-next-line no-unused-vars
  const handleDiscardDrug = async () => {
    if (!selectedDrug || !selectedDrug.boxItemId) {
      showToast('缺少必要参数，无法丢弃', 'error');
      return;
    }

    // 从 React state 获取用户ID
    const currentUserId = user?.userId || user?.id;

    if (!currentUserId) {
      showToast('用户信息异常，请重新登录', 'error');
      return;
    }

    try {
      // 调用后端API更新status为stopped
      const { response, data } = await authFetch(`/api/v1/box/${selectedDrug.boxItemId}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          status: 'stopped'
        })
      });

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

    // 从 React state 获取用户ID
    const currentUserId = user?.userId || user?.id;

    if (!currentUserId) {
      showToast('用户信息异常，请重新登录', 'error');
      return;
    }

    try {
      // 调用后端API更新status为stopped
      const { response, data } = await authFetch(`/api/v1/box/${drug.boxItemId}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          status: 'stopped'
        })
      });

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

  // 监听认证过期事件，自动跳转到登录页
  useEffect(() => {
    const handleAuthExpired = () => {
      setIsLoggedIn(false);
      setUser(null);
      showToast('认证已过期，请重新登录', 'error');
    };
    window.addEventListener('elder-auth-expired', handleAuthExpired);
    return () => window.removeEventListener('elder-auth-expired', handleAuthExpired);
  }, [showToast]);

  // 页面加载时恢复登录状态：家属端 token 优先，否则检查老人端 token
  // 用户信息通过API获取，不依赖localStorage
  // 自动恢复登录逻辑已禁用 - 用户每次需要手动登录
  /*
  useEffect(() => {
    // 家属端恢复
    if (isGuardianAuthenticated()) {
      const guardianToken = getGuardianToken();
      if (guardianToken) {
        // 通过API获取家属用户信息
        fetch('/api/v1/user/profile', {
          headers: { 'Authorization': `Bearer ${guardianToken}` }
        })
        .then(res => res.json())
        .then(data => {
          if (data.code === 200 && data.data && data.data.role === 'family') {
            // 不自动切换loginMode，保持当前状态（默认elder）
            // setLoginMode('guardian');  // 注释掉自动切换逻辑
            setUser(data.data);
            setIsLoggedIn(true);
          } else {
            // token无效，清除并跳登录
            clearAuth();
          }
        })
        .catch(() => {
          clearAuth();
        });
        return;
      }
    }

    // 老人端恢复
    if (isAuthenticated()) {
      const elderToken = getToken();
      if (elderToken) {
        fetch('/api/v1/user/profile', {
          headers: { 'Authorization': `Bearer ${elderToken}` }
        })
        .then(res => res.json())
        .then(data => {
          if (data.code === 200 && data.data && data.data.role && data.data.role !== 'family') {
            const userData = data.data;
            setUser(userData);
            setIsLoggedIn(true);

            // 加载老人端数据
            if (userData.id) {
              loadMedicineBoxList(userData.userId);
              loadShortageWarnings(userData.userId);
              loadCalendarPlans(userData.userId);
              loadEmergencyContacts(userData.id);
              fetchDailyLesson(userData.id);
            }
          } else {
            // token无效或角色不对，清除并跳登录
            clearAuth();
          }
        })
        .catch(() => {
          clearAuth();
        });
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  */

  // Token过期事件监听 - 自动跳转到对应登录页
  useEffect(() => {
    const handleElderAuthExpired = () => {
      setUser(null);
      setIsLoggedIn(false);
      setLoginMode('elder'); // 跳转到老人端登录
    };

    const handleGuardianAuthExpired = () => {
      setUser(null);
      setIsLoggedIn(false);
      setLoginMode('guardian'); // 跳转到家属端登录
    };

    window.addEventListener('elder-auth-expired', handleElderAuthExpired);
    window.addEventListener('guardian-auth-expired', handleGuardianAuthExpired);

    return () => {
      window.removeEventListener('elder-auth-expired', handleElderAuthExpired);
      window.removeEventListener('guardian-auth-expired', handleGuardianAuthExpired);
    };
  }, []);

  // WebSocket 连接管理 - 老人端实时通知
  useEffect(() => {
    if (!user || user.role === 'family' || !user.id) return;

    const connectWebSocket = () => {
      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      // 后端 WebSocket 地址（开发环境直接连 8080；token 通过查询参数传递，
      // 因为浏览器 WebSocket API 无法自定义请求头，后端拦截器同时支持两种方式）
      const host = window.location.hostname;
      const token = getToken();
      const wsUrl = `${wsProtocol}//${host}:8080/ws/notifications?token=${encodeURIComponent(token || '')}&elderId=${user.id}`;
      try {
        const ws = new WebSocket(wsUrl);
        wsRef.current = ws;

        ws.onopen = () => {
          setWsConnected(true);
        };

        ws.onmessage = (event) => {
          try {
            const msg = JSON.parse(event.data);
            if (msg.type === 'new_notification') {
              // 收到新通知，更新未读数
              setNotificationUnreadCount(prev => prev + 1);
            }
          } catch (e) {
            // 忽略解析错误
          }
        };

        ws.onclose = () => {
          wsRef.current = null;
          setWsConnected(false);
          // 5秒后重连
          setTimeout(() => {
            if (user && user.role !== 'family' && user.id) {
              connectWebSocket();
            }
          }, 5000);
        };

        ws.onerror = (error) => {
          console.warn('WebSocket连接错误');
          ws.close();
        };
      } catch (e) {
        console.warn('WebSocket连接失败，将使用轮询模式');
        setWsConnected(false);
      }
    };

    connectWebSocket();

    return () => {
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [user, user?.id, user?.role]);

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
      speakRef.current?.(text);
    };
    
    return () => {
      delete window.speakMedicationReminder;
    };
  }, [speakRef]);

  // 用药提醒：每分钟检查一次，支持渐进式提醒
  useEffect(() => {
    if (!isLoggedIn) {
      return;
    }

    // 渐进式提醒检查：提前15min → 到时 → 超时10min通知家属
    // 触发策略：只在"阶段升级"时弹窗，避免关闭后1分钟又弹同一阶段
    const checkProgressiveReminders = () => {
      if (!calendarPlans || calendarPlans.length === 0) {
        return;
      }

      const now = new Date();
      const currentHours = now.getHours();
      const currentMinutes = now.getMinutes();
      const currentTimeInMinutes = currentHours * 60 + currentMinutes;

      // 找出需要提醒的用药计划（提前15分钟、到时、超时10分钟通知家属）
      const needRemind = calendarPlans.filter(reminder => {
        if (reminder.taken || reminder.missed) {
          return false;
        }

        const [hours, minutes] = reminder.time.split(':').map(Number);
        const reminderTimeInMinutes = hours * 60 + minutes;
        const remindBefore = reminder.remindBefore || 15;
        const timeDiff = currentTimeInMinutes - reminderTimeInMinutes;

        // 提前15分钟提醒（使用后端的reminderStage或前端计算）
        const stage = reminder.reminderStage;
        if (stage && stage !== 'none') {
          return true; // 后端已推进阶段，需要提醒
        }

        // 前端兜底：提前remindBefore分钟也开始提醒
        return timeDiff >= -remindBefore;
      }).map(reminder => {
        // 为每个提醒计算前端阶段（后端阶段优先）
        const [hours, minutes] = reminder.time.split(':').map(Number);
        const reminderTimeInMinutes = hours * 60 + minutes;
        const remindBefore = reminder.remindBefore || 15;
        const timeDiff = currentTimeInMinutes - reminderTimeInMinutes;

        let stage = reminder.reminderStage;
        if (!stage || stage === 'none') {
          // 阶段简化：pre_remind → due_now → notify_family（超时10分钟）
          // 原 overdue 阶段已合并，到时提醒后10分钟内保持 due_now，不再反复打扰
          if (timeDiff >= 10) {
            stage = 'notify_family';
          } else if (timeDiff >= 0) {
            stage = 'due_now';
          } else if (timeDiff >= -remindBefore) {
            stage = 'pre_remind';
          }
        }

        return { ...reminder, reminderStage: stage };
      });

      if (needRemind.length > 0) {
        // 计算当前最高阶段
        const stageOrder = ['pre_remind', 'due_now', 'overdue', 'notify_family'];
        let currentHighestIdx = -1;
        needRemind.forEach(r => {
          const idx = stageOrder.indexOf(r.reminderStage);
          if (idx > currentHighestIdx) currentHighestIdx = idx;
        });

        // 只有阶段升级时才再次提醒（避免关闭后1分钟又弹同一阶段）
        const lastShownStage = lastShownStageRef.current;
        const shouldRemind = currentHighestIdx >= 0 && (
          !lastShownStage ||
          currentHighestIdx > stageOrder.indexOf(lastShownStage)
        );

        if (shouldRemind) {
          setMissedReminders(needRemind);
          setShowMedicationReminder(true);
          lastReminderTimeRef.current = Date.now();
        }
      }
    };

    checkProgressiveReminders();
    const intervalId = setInterval(checkProgressiveReminders, 60 * 1000); // 每分钟检查

    return () => {
      clearInterval(intervalId);
    };
  }, [isLoggedIn, calendarPlans]);

  // 跨日检测：页面长时间不刷新时，发现日期变化则自动刷新今日用药计划
  // 解决"昨天开着页面到今天，日历仍是昨天数据"的问题
  useEffect(() => {
    if (!isLoggedIn) return;

    const checkDateChange = () => {
      const todayKey = getLocalDateKey();
      if (lastDateRef.current && lastDateRef.current !== todayKey) {
        lastDateRef.current = todayKey;
        // 清空昨日缓存，避免断网时误读昨天的计划
        localStorage.removeItem(TODAY_PLANS_CACHE_KEY);
        // 重新加载今日用药计划（通过 ref 调用最新版本，避免闭包陈旧）
        if (loadCalendarPlansRef.current) {
          loadCalendarPlansRef.current();
        }
      } else if (!lastDateRef.current) {
        lastDateRef.current = todayKey;
      }
    };

    const intervalId = setInterval(checkDateChange, 60 * 1000); // 每分钟检查一次
    return () => clearInterval(intervalId);
  }, [isLoggedIn]);

  // 监听弹窗显示，自动播报（只在弹窗首次打开时播报一次）
  const lastReminderIdRef = useRef(null);
  
  useEffect(() => {
    if (showMedicationReminder && missedReminders.length > 0) {
      const firstReminderId = missedReminders[0]?.id;
      
      if (lastReminderIdRef.current === firstReminderId) {
        return;
      }
      
      lastReminderIdRef.current = firstReminderId;
      
      const timer = setTimeout(() => {
        const reminderTexts = missedReminders.map(reminder => {
          const drugName = reminder.drug || reminder.drugName || '未知药品';
          const time = reminder.time || reminder.scheduledTime || '未知时间';
          const dosage = reminder.dosage ? `，用量${reminder.dosage}` : '';
          return `${time}的${drugName}${dosage}`;
        });

        // 根据最高阶段播报不同内容
        const stageOrder = ['pre_remind', 'due_now', 'overdue', 'notify_family'];
        let highestIndex = -1;
        missedReminders.forEach(r => {
          const idx = stageOrder.indexOf(r.reminderStage);
          if (idx > highestIndex) highestIndex = idx;
        });
        const highestStage = highestIndex >= 0 ? stageOrder[highestIndex] : 'due_now';

        let speakText;
        switch (highestStage) {
          case 'pre_remind':
            speakText = `用药提醒！以下药物将在15分钟后需要服用：${reminderTexts.join('；')}。请做好准备。`;
            break;
          case 'due_now':
            speakText = `用药提醒！该服药了！您有以下药物需要服用：${reminderTexts.join('；')}。请及时服用。`;
            break;
          case 'overdue':
            speakText = `紧急提醒！您有以下药物已超时未服用：${reminderTexts.join('；')}。请尽快服药！`;
            break;
          case 'notify_family':
            speakText = `紧急提醒！您有以下药物超时较久未服用，已通知您的家属：${reminderTexts.join('；')}。请尽快服药！`;
            break;
          default:
            speakText = `用药提醒！您有以下药物还没有服用：${reminderTexts.join('；')}。请及时服用。`;
        }
        speakRef.current?.(speakText);
      }, 500);
      
      return () => clearTimeout(timer);
    }
  }, [showMedicationReminder, missedReminders, speakRef]);


  // 调用后端追问API
  const handleAskFollowUp = async () => {
    if (!followUpQuestion.trim() || isFollowUpLoading) return;

    const userMessage = { role: 'user', content: followUpQuestion.trim() };
    const newMessages = [...followUpMessages, userMessage];
    setFollowUpMessages(newMessages);
    setFollowUpQuestion('');
    setIsFollowUpLoading(true);

    try {
      // 构造药品详细信息对象
      const drugDetail = {
        genericName: selectedDrug.name,
        tradeName: selectedDrug.tradeName || '',
        specification: selectedDrug.spec || selectedDrug.specification || '',
        manufacturer: selectedDrug.manufacturer || '',
        ingredient: selectedDrug.ingredient || '',
        indications: selectedDrug.indications || '',
        usage: selectedDrug.usage || selectedDrug.dosage || '',
        precautions: selectedDrug.precautions || '',
        adverseReactions: selectedDrug.adverseReactions || ''
      };

      const { data } = await authFetch('/api/ai/follow-up-question', {
        method: 'POST',
        body: JSON.stringify({
          drugDetail,
          question: userMessage.content,
          conversationHistory: newMessages.slice(-12).map(m => ({ role: m.role, content: m.content }))
        })
      });
      if (data.code === 200 && data.data) {
        setFollowUpMessages(prev => [...prev, { role: 'assistant', content: data.data }]);
      } else {
        setFollowUpMessages(prev => [...prev, { role: 'assistant', content: '抱歉，我暂时无法回答这个问题，请稍后再试。' }]);
      }
    } catch (error) {
      setFollowUpMessages(prev => [...prev, { role: 'assistant', content: '网络异常，请检查网络连接后重试。' }]);
    } finally {
      setIsFollowUpLoading(false);
    }
  };

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
        loadCalendarPlansRef.current?.();
      } else if (calendarViewMode === 'week' && !weeklyMedicationData) {
        loadWeeklyMedicationRef.current?.();
      }
    }

    // 切换到冲突检测页面时，自动调用本地规则快速检测
    if (activeTab === 'conflict' && drugList.length >= 2) {
      const doAutoCheck = async () => {
        setIsAutoChecking(true);
        setAutoCheckResult(null);
        try {
          const drugNames = drugList.map(d => d.name);
          const { data } = await authFetch('/api/conflict/quick-check-local', {
            method: 'POST',
            body: JSON.stringify(drugNames)
          });
          if (data.code === 200 && data.data) {
            setAutoCheckResult(data.data);
          }
        } catch (err) {
          console.error('自动快速检测失败:', err);
        } finally {
          setIsAutoChecking(false);
        }
      };
      doAutoCheck();
    } else if (activeTab === 'conflict') {
      // 药品不足2种，清除之前的自动检测结果
      setAutoCheckResult(null);
    }
  }, [activeTab, isLoggedIn, calendarPlans.length, calendarViewMode, drugList, weeklyMedicationData]);

  const [ocrTaskId, setOcrTaskId] = useState(null);
  const [ocrPolling, setOcrPolling] = useState(false);
  const [elderlyGuide, setElderlyGuide] = useState(''); // 老年友好用药指导
  const [isLoadingGuide, setIsLoadingGuide] = useState(false); // 是否正在加载AI指导
  const [followUpMessages, setFollowUpMessages] = useState([]); // 追问对话消息列表
  const [followUpQuestion, setFollowUpQuestion] = useState(''); // 当前追问输入
  const [isFollowUpLoading, setIsFollowUpLoading] = useState(false); // 追问加载中

  // 追问消息变化时自动滚动到底部（用户发送新问题后跳到最下方）
  useEffect(() => {
    if (followUpMessagesRef.current) {
      followUpMessagesRef.current.scrollTop = followUpMessagesRef.current.scrollHeight;
    }
  }, [followUpMessages, isFollowUpLoading, followUpMessagesRef]);

  // 当selectedDrug变化时，自动调用AI生成老年友好指导
  useEffect(() => {
    if (selectedDrug && selectedDrug.name) {
      fetchElderlyGuide(selectedDrug);
    }
    // 切换药品时清空追问消息和播放状态
    setFollowUpMessages([]);
    setFollowUpQuestion('');
    stopFollowUpSpeaking();
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

      
      // 第一层：尝试调用后端 AI 服务
      const { data } = await authFetch('/api/ai/elderly-guide', {
        method: 'POST',
        body: JSON.stringify(drugDetail)
      });
      
      if (data.code === 200 && data.data) {
        // 将<br/>标签替换为换行符，方便阅读
        const guideText = data.data.replace(/<br\/>/g, '\n');
        setElderlyGuide(guideText);
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

      
      const { data } = await authFetch('/api/deepseek/chat', {
        method: 'POST',
        body: JSON.stringify({
          messages: [{ role: 'user', content: prompt }],
          model: 'deepseek-chat',
          temperature: 0.7,
          max_tokens: 800
        })
      });
      
      if (data.choices && data.choices[0]) {
        const aiResponse = data.choices[0].message.content;
        setElderlyGuide(aiResponse);
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
      // eslint-disable-next-line default-case
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
    
    for (const [, data] of Object.entries(contraindications)) {
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
      
      if (!file.type.startsWith('image/')) {
        resolve(file);
        return;
      }

      // 如果不是WebP格式，直接返回
      if (!file.type.includes('webp') && !file.name.toLowerCase().endsWith('.webp')) {
        resolve(file);
        return;
      }

      
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
              resolve(jpegFile);
            } else {
              resolve(file);
            }
          }, 'image/jpeg', 0.9);
        };
        img.onerror = () => {
          resolve(file);
        };
        img.src = e.target.result;
      };
      reader.onerror = () => {
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

  // 移除单个图片
  const removeImage = (imageId) => {
    setBatchRecognizeItems(prev => {
      const itemToRemove = prev.find(item => item.id === imageId);
      if (itemToRemove?.previewUrl) {
        URL.revokeObjectURL(itemToRemove.previewUrl); // 释放预览URL，避免内存泄漏
      }
      return prev.filter(item => item.id !== imageId);
    });
    
    // 如果该图片已被选中，从选中列表中移除
    setBatchSelectedForAdd(prev => {
      const newSet = new Set(prev);
      newSet.delete(imageId);
      return newSet;
    });
  };

  // 批量识别所有图片
  const handleBatchRecognize = async () => {
    if (batchRecognizeItems.length === 0) {
      showToast('请先选择要识别的图片', 'warning');
      return;
    }

    // 清空之前的选中状态
    setBatchSelectedForAdd(new Set());
    
    // 更新状态为识别中
    setBatchRecognizeItems(prev => prev.map(item => ({ ...item, status: 'recognizing' })));

    try {
      const files = batchRecognizeItems.map(item => item.file);
      const formData = new FormData();
      files.forEach(file => formData.append('files', file));

      const response = await fetch('/api/v1/drug/recognize/batch-upload', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${getToken()}` },
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
            // 使用药品ID而不是图片ID
            newSelected.add(result.matchedDrugId);
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
  // eslint-disable-next-line no-unused-vars
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
          const { response } = await authFetch(`/api/v1/box`, {
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
      
      
      const formData = new FormData();
      formData.append('file', convertedFile);

      // 不设置Content-Type，让浏览器自动处理
      // 通过代理转发到后端
      const response = await fetch('/api/v1/drug/recognize/upload', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${getToken()}`
          // 注意：不要设置Content-Type，浏览器会自动设置multipart/form-data及boundary
        },
        body: formData
      });
      
      // 调试：查看实际发送的请求头

      const data = await response.json();


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
        const { data } = await authFetch(`/api/v1/drug/recognize/result/${taskId}`);


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
      const { data: searchData } = await authFetch(`/api/v1/drug/list?keyword=${encodeURIComponent(drug.name)}`);
      
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
      const { response: addResponse, data: addData } = await authFetch(`/api/v1/box`, {
        method: 'POST',
        body: JSON.stringify(drugData)
      });

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

  // 批量添加所有确认的药品并检测冲突
  // eslint-disable-next-line no-unused-vars
  const handleBatchAddAllDrugs = async () => {
    
    if (batchConfirmedDrugs.length === 0) {
      showToast('没有需要添加的药品', 'warning');
      return;
    }
    
    setIsBatchAddingAll(true);
    
    try {
      let successCount = 0;
      let failCount = 0;
      const addedDrugNames = []; // 记录成功添加的药品名称
      let lastAddedDrugName = null; // 记录最后添加成功的药品名称（用于冲突检测）
      
      // 逐个添加药品，每次添加后刷新药箱列表
      for (let i = 0; i < batchConfirmedDrugs.length; i++) {
        const drug = batchConfirmedDrugs[i];
        
        try {
          const { response } = await authFetch(`/api/v1/box`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              drugId: drug.drugId,
              dosage: drug.dosage,
              frequency: drug.frequency,
              startDate: drug.startDate,
              endDate: drug.endDate,
              expiryDate: drug.expiryDate,
              totalQuantity: drug.totalQuantity,
              status: drug.status
            })
          });
          
          const data = await response.json();
          
          if (data.code === 200) {
            successCount++;
            addedDrugNames.push(drug.name);
            
            // 立即刷新药箱列表，确保数据同步
            await loadMedicineBoxList(user?.userId);
            
            // 记录最后添加成功的药品（用于后续冲突检测）
            lastAddedDrugName = drug.name;
          } else {
            failCount++;
            console.error(`❌ 第 ${i + 1} 个药品添加失败:`, data.message);
            showToast(`第 ${i + 1} 个药品添加失败: ${data.message}`, 'error');
          }
        } catch (err) {
          console.error(`❌ 第 ${i + 1} 个药品添加异常:`, err);
          failCount++;
          showToast(`第 ${i + 1} 个药品添加失败: ${err.message}`, 'error');
        }
      }
      
      
      if (successCount > 0) {
        showToast(`成功添加 ${successCount} 个药品到药箱`, 'success');
        
        // 只清空已确认的药品列表和索引，保留识别结果
        setBatchConfirmedDrugs([]);
        setBatchDrugIndex(0);
        // 注意：不清空 recognizedDrugs 和 batchSelectedForAdd，让用户可以继续使用
        
        // 对最后添加的药品进行冲突检测
        if (lastAddedDrugName && addedDrugNames.length > 0) {
          showToast('正在进行冲突检测...', 'info');
          
          // 重要：重新获取最新的药箱列表，确保包含刚添加的所有药品
          let latestDrugList = drugList;
          try {
            const { response, data } = await authFetch(`/api/v1/box/list`);
            if (response.ok && data.code === 200) {
              latestDrugList = data.data.map(item => ({
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
                remaining: item.remainingQuantity || item.totalQuantity,
                note: item.note,
                status: item.status,
                createdAt: item.createdAt
              }));
            }
          } catch (err) {
            console.error('获取最新药箱列表失败:', err);
          }
          
          
          // 使用最新的药箱列表进行冲突检测
          const conflictResult = await checkConflictsForNewDrug(lastAddedDrugName, latestDrugList);
          
          if (conflictResult && !conflictResult.noConflict) {
            showToast(`检测到 ${conflictResult.conflicts?.length || 0} 条冲突警告`, 'warning');
            setConflictAlertResult(conflictResult);
            setShowConflictAlert(true);
          } else {
          }
        }
        
        setConflictNeedsRecheck(true);
        setConflictReport(null);
      } else if (failCount > 0) {
        showToast(`添加失败 ${failCount} 个药品`, 'error');
      }
    } catch (error) {
      showToast('批量添加失败，请稍后重试', 'error');
      console.error('批量添加药品失败:', error);
    } finally {
      setIsBatchAddingAll(false);
    }
  };

  // 批量添加指定药品列表（不依赖状态）
  const handleBatchAddAllDrugsWithList = async (drugList) => {
    
    if (!drugList || drugList.length === 0) {
      showToast('没有需要添加的药品', 'warning');
      return;
    }
    
    setIsBatchAddingAll(true);
    
    try {
      let successCount = 0;
      let failCount = 0;
      const addedDrugNames = []; // 记录成功添加的药品名称
      let lastAddedDrugName = null; // 记录最后添加成功的药品名称（用于冲突检测）
      
      // 逐个添加药品，每次添加后刷新药箱列表
      for (let i = 0; i < drugList.length; i++) {
        const drug = drugList[i];
        
        try {
          const { response } = await authFetch(`/api/v1/box`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              drugId: drug.drugId,
              dosage: drug.dosage,
              frequency: drug.frequency,
              startDate: drug.startDate,
              endDate: drug.endDate,
              expiryDate: drug.expiryDate,
              totalQuantity: drug.totalQuantity,
              status: drug.status
            })
          });
          
          const data = await response.json();
          
          if (data.code === 200) {
            successCount++;
            addedDrugNames.push(drug.name);
            
            // 立即刷新药箱列表，确保数据同步
            await loadMedicineBoxList(user?.userId);
            
            // 记录最后添加成功的药品（用于后续冲突检测）
            lastAddedDrugName = drug.name;
          } else {
            failCount++;
            console.error(`❌ 第 ${i + 1} 个药品添加失败:`, data.message);
            showToast(`第 ${i + 1} 个药品添加失败: ${data.message}`, 'error');
          }
        } catch (err) {
          console.error(`❌ 第 ${i + 1} 个药品添加异常:`, err);
          failCount++;
          showToast(`第 ${i + 1} 个药品添加失败: ${err.message}`, 'error');
        }
      }
      
      
      if (successCount > 0) {
        showToast(`成功添加 ${successCount} 个药品到药箱`, 'success');
        
        // 对最后添加的药品进行冲突检测
        if (lastAddedDrugName && addedDrugNames.length > 0) {
          showToast('正在进行冲突检测...', 'info');
          
          // 重要：重新获取最新的药箱列表，确保包含刚添加的所有药品
          const latestDrugList = await authFetch(`/api/v1/box/list`)
            .then(({ data }) => {
              if (data.code === 200) {
                return data.data.map(item => ({
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
                  remaining: item.remainingQuantity || item.totalQuantity,
                  note: item.note,
                  status: item.status,
                  createdAt: item.createdAt
                }));
              }
              return [];
            })
            .catch(err => {
              console.error('获取最新药箱列表失败:', err);
              return drugList; // 如果失败，使用传入的 drugList
            });
          
          
          // 使用最新的药箱列表进行冲突检测
          const conflictResult = await checkConflictsForNewDrug(lastAddedDrugName, latestDrugList);
          
          if (conflictResult && !conflictResult.noConflict) {
            showToast(`检测到 ${conflictResult.conflicts?.length || 0} 条冲突警告`, 'warning');
            setConflictAlertResult(conflictResult);
            setShowConflictAlert(true);
          } else {
          }
        }
        
        setConflictNeedsRecheck(true);
        setConflictReport(null);
      } else if (failCount > 0) {
        showToast(`添加失败 ${failCount} 个药品`, 'error');
      }
    } catch (error) {
      showToast('批量添加失败，请稍后重试', 'error');
      console.error('批量添加药品失败:', error);
    } finally {
      setIsBatchAddingAll(false);
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
        return { ...r, taken: true, missed: false };
      }
      return r;
    }));

    setShowCelebration(true);
    setTimeout(() => setShowCelebration(false), 2500);

    // 等后端真正接受这次操作，再用后端数据做一次校验，避免本地乐观更新和后端脱节
    if (targetPlanId && user?.userId) {
      try {
        await executeMedicationActionWithAPI(targetPlanId, 'confirm', targetDosage);
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

    // 不论成功失败都拉后端做最终校验：今日 + 一周 + 药箱都刷
    // 成功时：本地就是 taken、后端也是 completed，状态对齐
    // 失败时：后端仍是 pending，reload 后 UI 自动回退到"待吃"，消除本地和后端的不一致
    await Promise.all([
      loadCalendarPlans(),
      typeof loadWeeklyMedication === 'function' ? loadWeeklyMedication() : Promise.resolve(),
      user?.userId ? loadMedicineBoxList(user.userId) : Promise.resolve()
    ]);

    // 服药后刷新缺药预警
    if (user?.userId) {
      loadShortageWarnings(user.userId);
    }
  };

  // 调用后端统一幂等用药操作接口
  const executeMedicationActionWithAPI = async (planId, action, dosage = '') => {
    try {
      const { response, data: result } = await authFetch(`/api/v1/plan/${planId}/action`, {
        method: 'PUT',
        body: JSON.stringify({ action })
      });

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
    const result = await executeMedicationActionWithAPI(item.planId, 'confirm');
    if (result.success) {
      // 刷新药箱列表和缺药预警
      loadMedicineBoxList(user.userId);
      loadShortageWarnings(user.userId);
    } else {
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
    const result = await executeMedicationActionWithAPI(item.planId, 'undo');
    if (result.success) {
      // 重拉当周数据，让后端的真实状态覆盖本地
      loadWeeklyMedication();
      // 刷新药箱列表和缺药预警
      loadMedicineBoxList(user.userId);
      loadShortageWarnings(user.userId);
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
  // eslint-disable-next-line no-unused-vars
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
        return { ...r, taken: false };
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
        await executeMedicationActionWithAPI(targetPlanId, 'undo', targetDosage);
      } catch (err) {
        console.error('撤销服药 API 调用异常:', err);
      }
    }

    // 清掉 localStorage + reload 两个视图 + 药箱，让后端做最终校验
    if (targetPlanId) {
      saveLocalMedicationStatus(targetPlanId, null);
    }
    await Promise.all([
      loadCalendarPlans(),
      typeof loadWeeklyMedication === 'function' ? loadWeeklyMedication() : Promise.resolve(),
      user?.userId ? loadMedicineBoxList(user.userId) : Promise.resolve()
    ]);

    // 撤销服药后刷新缺药预警
    if (user?.userId) {
      loadShortageWarnings(user.userId);
    }
  };

  const takenCount = calendarPlans.filter(r => r.taken).length;
  const totalCount = calendarPlans.length;
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
            {user?.role !== 'family' && (
              <button className="header-btn guardian-btn" onClick={() => setShowMyGuardians(true)}>
                <span className="btn-icon">👨‍👩‍👧</span>
                <span className="btn-label">我的家属</span>
              </button>
            )}
          </div>
          <span className="virtual-pharmacist">👨‍⚕️</span>
          <div className="user-greeting">
            <p className="user-name">您好，{user?.realName || user?.username || '用户'}！</p>
            <button className="logout-btn" onClick={handleLogout}>退出登录</button>
          </div>
        </div>
        <button className="notification-bell-btn" onClick={() => setShowNotificationPanel(true)}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 0 1-3.46 0" />
          </svg>
          {notificationUnreadCount > 0 && (
            <span className="notification-bell-badge">
              {notificationUnreadCount > 99 ? '99+' : notificationUnreadCount}
            </span>
          )}
        </button>
      </div>
      {user?.role !== 'family' && (
        <div className="header-sos-wrapper">
          <button className="header-sos-btn" onClick={() => setActiveTab('emergency')}>
            🚨 SOS 紧急求助
          </button>
        </div>
      )}
    </header>
  );

  const renderHomeTab = () => (
    <div>
      {/* 缺药预警横幅 */}
      {shortageWarnings.length > 0 && (
        <div className="shortage-warning-banner" onClick={() => setShowShortageDetail(true)}>
          <div className="shortage-warning-header">
            <span className="shortage-warning-icon">
              {shortageWarnings.some(w => w.warningLevel === 'critical') ? '🚨' : '⚠️'}
            </span>
            <div className="shortage-warning-text">
              <span className="shortage-warning-title">
                {shortageWarnings.some(w => w.warningLevel === 'critical')
                  ? `${shortageWarnings.filter(w => w.warningLevel === 'critical').length}种药品已用尽`
                  : shortageWarnings.length === 1
                    ? `${shortageWarnings[0].drugName}即将用尽`
                    : `${shortageWarnings.length}种药品即将用尽`}
              </span>
              <span className="shortage-warning-subtitle">
                {shortageWarnings.some(w => w.warningLevel === 'critical')
                  ? '请尽快补充药品'
                  : `最近${shortageWarnings[0].remainingDays}天将用完，建议提前购买`}
              </span>
            </div>
          </div>
          <div className="shortage-warning-actions">
            <button
              className="shortage-action-btn shortage-action-pharmacy"
              onClick={(e) => {
                e.stopPropagation();
                setActiveTab('drugs');
              }}
            >
              🏪 去购药
            </button>
            <button
              className="shortage-action-btn shortage-action-hospital"
              onClick={(e) => {
                e.stopPropagation();
                setActiveTab('emergency');
              }}
            >
              🏥 在线问诊
            </button>
          </div>
        </div>
      )}

      {/* 今日一课 - 慢病科普卡片 */}
      <DailyLessonCard
        lesson={dailyLesson}
        loading={dailyLessonLoading}
        onRefresh={handleDailyLessonRefresh}
        onGoProfile={() => setShowProfileEdit(true)}
              />

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
              <div className="progress-ring-wrapper">
                <div className="progress-ring-container">
                  <svg className="progress-ring" viewBox="0 0 180 180">
                    <defs>
                      <linearGradient id="progressGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#4A90E2" />
                        <stop offset="100%" stopColor="#98D4BB" />
                      </linearGradient>
                    </defs>
                    <circle className="progress-ring-circle-bg" cx="90" cy="90" r="80" />
                    {takenCount > 0 && (
                      <circle
                        className={`progress-ring-circle${isFullProgress ? ' full' : ''}`}
                        cx="90" cy="90" r="80"
                        strokeDasharray="502.65"
                        strokeDashoffset={502.65 - (takenCount / totalCount) * 502.65}
                      />
                    )}
                  </svg>
                  <div className="progress-ring-text">
                    <div className="progress-ring-value">{takenCount}/{totalCount}</div>
                    <div className="progress-ring-label">已完成</div>
                  </div>
                </div>
                <div className="progress-ring-encourage">
                  {isFullProgress ? '太棒了，今日用药已全部完成！' :
                   takenCount === 0 ? '新的一天，记得按时用药哦～' :
                   '继续加油，坚持就是胜利！'}
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
      <RecognitionHistoryModal
        ref={recognitionHistoryModalRef}
        authFetch={authFetch}
        onJumpToRecognition={(drug) => {
          setRecognizedDrugs([drug]);
          setBatchSelectedForAdd(new Set([drug.id]));
          setActiveTab('recognition');
        }}
      />
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 className="card-title">
          <span className="card-title-icon">📷</span>
          上传药品照片
        </h2>
        <button
          onClick={() => recognitionHistoryModalRef.current?.loadHistory()}
          style={{
            padding: '6px 14px', borderRadius: '8px', border: '1.5px solid #6366f1',
            background: '#eef2ff', color: '#4f46e5', fontSize: '13px', cursor: 'pointer',
            display: 'flex', alignItems: 'center', gap: '4px'
          }}
        >
          📋 识药历史
        </button>
      </div>

      {/* 批量识别区域 */}
      {batchRecognizeItems.length > 0 ? (
        <div className="batch-recognize-section">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '20px', color: '#4A90E2', margin: 0 }}>
              📸 批量识别结果 ({batchRecognizeItems.filter(i => i.status === 'success').length} 成功)
            </h3>
            <div style={{ display: 'flex', gap: '8px' }}>
              {/* 只有识别成功后才显示查看按钮 */}
              {batchRecognizeItems.some(item => item.status === 'success') && (
                <button
                  onClick={() => setActiveTab('recognition')}
                  style={{
                    padding: '12px 24px',
                    fontSize: '16px',
                    border: '3px solid #FF6B35',
                    borderRadius: '12px',
                    background: '#FFF5F0',
                    color: '#FF6B35',
                    cursor: 'pointer',
                    fontWeight: 'bold',
                    boxShadow: '0 2px 8px rgba(255, 107, 53, 0.2)',
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => {
                    e.target.style.background = '#FF6B35';
                    e.target.style.color = 'white';
                    e.target.style.transform = 'scale(1.05)';
                  }}
                  onMouseLeave={(e) => {
                    e.target.style.background = '#FFF5F0';
                    e.target.style.color = '#FF6B35';
                    e.target.style.transform = 'scale(1)';
                  }}
                >
                  ️ 查看识别结果
                </button>
              )}
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
          </div>

          <div className="batch-preview-grid">
            {batchRecognizeItems.map((item) => (
              <div
                key={item.id}
                className={`batch-preview-item ${item.status === 'success' ? 'success' : item.status === 'failed' ? 'failed' : 'pending'}`}
              >
                <img src={item.previewUrl} alt="预览" className="batch-preview-image" />
                {/* 状态标签 - 左上角 */}
                {item.status === 'recognizing' && (
                  <div className="batch-preview-status-label recognizing">
                    识别中
                  </div>
                )}
                {/* 删除按钮 - 右上角 */}
                <button
                  className="batch-preview-delete-btn"
                  onClick={(e) => {
                    e.stopPropagation();
                    removeImage(item.id);
                  }}
                  title={batchRecognizeItems.some(img => img.status === 'recognizing') ? '识别中，无法删除' : '移除图片'}
                  disabled={batchRecognizeItems.some(img => img.status === 'recognizing')}
                  style={{
                    opacity: batchRecognizeItems.some(img => img.status === 'recognizing') ? 0.5 : 1,
                    cursor: batchRecognizeItems.some(img => img.status === 'recognizing') ? 'not-allowed' : 'pointer'
                  }}
                >
                  ✕
                </button>
              </div>
            ))}
          </div>

          {/* 开始识别按钮 */}
          <div style={{ marginTop: '24px', textAlign: 'center' }}>
            <button
              className="btn btn-primary btn-large"
              onClick={handleBatchRecognize}
              disabled={batchRecognizeItems.some(item => item.status === 'recognizing')}
              style={{ minWidth: '200px' }}
            >
              {batchRecognizeItems.some(item => item.status === 'recognizing') ? '⏳ 识别中...' : ' 开始识别'}
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
    <div className="card" style={{ position: 'relative' }}>
      {/* 加载动画覆盖层 */}
      {isFetchingDrug && (
        <div className="loading-overlay">
          <div className="loading-spinner-container">
            <div className="loading-spinner"></div>
          </div>
          <div className="loading-progress-bar">
            <div className="loading-progress-fill"></div>
          </div>
          <p className="loading-text"> 正在查询药品详情，请稍候...</p>
        </div>
      )}
      
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
        <div style={{ display: 'flex', gap: '8px' }}>
          {/* 只有有识别结果时才显示全部加入药箱按钮 */}
          {recognizedDrugs.length > 0 && (
            <button
              onClick={async () => {
                // 获取所有选中的药品ID
                const selectedDrugIds = Array.from(batchSelectedForAdd);
                
                if (selectedDrugIds.length === 0) {
                  showToast('请先选择要加入药箱的药品', 'warning');
                  return;
                }
                
                // 筛选出选中的药品
                const selectedDrugs = recognizedDrugs.filter(drug => selectedDrugIds.includes(drug.id));
                
                if (selectedDrugs.length === 0) {
                  showToast('未找到选中的药品', 'error');
                  return;
                }
                
                // 清空之前的确认数据
                setBatchConfirmedDrugs([]);
                setBatchDrugIndex(0);
                
                // 打开第一个药品的确认弹窗
                setShowBatchConfirmModal(true);
              }}
              disabled={isBatchAdding || batchSelectedForAdd.size === 0}
              style={{
                padding: '12px 24px',
                fontSize: '16px',
                border: 'none',
                borderRadius: '12px',
                background: isBatchAdding ? '#CCCCCC' : (batchSelectedForAdd.size === 0 ? '#E0E0E0' : '#4CAF50'),
                color: 'white',
                cursor: isBatchAdding || batchSelectedForAdd.size === 0 ? 'not-allowed' : 'pointer',
                fontWeight: 'bold',
                boxShadow: '0 2px 8px rgba(76, 175, 80, 0.3)',
                transition: 'all 0.3s ease'
              }}
              onMouseEnter={(e) => {
                if (!isBatchAdding && batchSelectedForAdd.size > 0) {
                  e.target.style.background = '#45a049';
                  e.target.style.transform = 'scale(1.05)';
                }
              }}
              onMouseLeave={(e) => {
                if (!isBatchAdding && batchSelectedForAdd.size > 0) {
                  e.target.style.background = '#4CAF50';
                  e.target.style.transform = 'scale(1)';
                }
              }}
            >
              {isBatchAdding ? '⏳ 添加中...' : `✅ 加入药箱 (${batchSelectedForAdd.size}/${recognizedDrugs.length})`}
            </button>
          )}
          {recognizedDrugs.length > 0 && (
            <button
              className="btn btn-secondary"
              onClick={() => setActiveTab('upload')}
            >
              ← 返回继续识别
            </button>
          )}
        </div>
      </div>

      {recognizedDrugs.length > 0 ? (
        <div className="drug-list">
          {recognizedDrugs.map((drug, index) => {
            const isSelected = batchSelectedForAdd.has(drug.id);
            return (
              <div 
                key={index} 
                className={`drug-card ${isSelected ? 'selected' : ''}`} 
                style={{ 
                  animationDelay: `${index * 0.15}s`,
                  border: isSelected ? '3px solid #4CAF50' : '2px solid transparent',
                  boxShadow: isSelected ? '0 8px 24px rgba(76, 175, 80, 0.3)' : '0 2px 8px rgba(0, 0, 0, 0.08)',
                  cursor: 'pointer',
                  position: 'relative',
                  transition: 'all 0.3s ease'
                }}
                onClick={() => {
                  // 切换选中状态
                  setBatchSelectedForAdd(prev => {
                    const newSet = new Set(prev);
                    if (newSet.has(drug.id)) {
                      newSet.delete(drug.id);
                    } else {
                      newSet.add(drug.id);
                    }
                    return newSet;
                  });
                }}
              >
                {/* 选中状态标签 - 右上角 */}
                {isSelected && (
                  <div style={{
                    position: 'absolute',
                    top: '12px',
                    right: '12px',
                    background: 'linear-gradient(135deg, #4CAF50 0%, #66BB6A 100%)',
                    color: 'white',
                    padding: '8px 16px',
                    borderRadius: '20px',
                    fontSize: '15px',
                    fontWeight: 'bold',
                    boxShadow: '0 4px 12px rgba(76, 175, 80, 0.5)',
                    zIndex: 10,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    animation: 'fadeInScale 0.3s ease-out'
                  }}>
                    <span style={{ fontSize: '18px' }}>✓</span>
                    <span>已选择</span>
                  </div>
                )}
                
                <span className="drug-card-icon">💊</span>
                <h4 className="drug-name">{drug.name}</h4>
                <p className="drug-info">规格：{drug.spec}</p>
                <p className="drug-info">生产厂家：{drug.manufacturer}</p>
                <p className="drug-info">匹配度：<span className="drug-match">{drug.matchScore}%</span></p>
                <button
                  className="btn btn-primary"
                  style={{ marginTop: '20px', width: '100%', minHeight: '56px' }}
                  onClick={(e) => {
                    e.stopPropagation(); // 阻止事件冒泡
                    fetchDrugDetail(drug.name, drug, {
                      showLoading: true
                    });
                  }}
                >
                   查看用药说明
                </button>
                {!drugList.some(d => d.name === drug.name) && (
                <button
                  className="btn btn-success"
                  style={{ marginTop: '12px', width: '100%', minHeight: '56px' }}
                  onClick={(e) => {
                    e.stopPropagation(); // 阻止事件冒泡
                    addToMedicineBox(drug);
                  }}
                >
                  ➕ 加入我的药箱
                </button>
                )}
              </div>
            );
          })}
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
    
    authFetch(`/api/v1/drug/detail?drugName=${encodeURIComponent(drugName)}`)
      .then(({ data }) => {
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
          {/* 医学原文折叠对照 */}
          <div style={{
            borderRadius: '12px',
            marginBottom: '16px',
            border: '1px solid #e2e8f0',
            overflow: 'hidden'
          }}>
            <div
              onClick={() => setShowOriginalText(!showOriginalText)}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '10px 16px',
                background: 'linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%)',
                cursor: 'pointer', userSelect: 'none'
              }}
            >
              <span style={{ fontSize: '14px', fontWeight: 'bold', color: '#7c3aed', display: 'flex', alignItems: 'center', gap: '6px' }}>
                📄 医学原文
                <span style={{ fontSize: '11px', fontWeight: 'normal', color: '#a78bfa' }}>对照查看</span>
              </span>
              <span style={{ fontSize: '13px', color: '#7c3aed', transition: 'transform 0.2s', transform: showOriginalText ? 'rotate(180deg)' : 'rotate(0deg)' }}>
                ▼
              </span>
            </div>
            {showOriginalText && (
              <div style={{ padding: '14px 16px', background: '#fafafa' }}>
                {/* 左右对照布局 */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', height: '400px' }}>
                  {/* 左侧：老年友好版摘要 */}
                  <div style={{ background: '#f0fdf4', borderRadius: '10px', padding: '12px', border: '1px solid #bbf7d0', overflowY: 'auto' }}>
                    <h4 style={{ fontSize: '16px', fontWeight: 'bold', color: '#16a34a', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      👴 老年友好版
                    </h4>
                    {elderlyGuide ? (
                      <div style={{ fontSize: '16px', lineHeight: '2', color: '#374151', whiteSpace: 'pre-wrap' }}
                        dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(displayGuideHtml) }}
                      />
                    ) : (
                      <p style={{ fontSize: '16px', color: '#999' }}>加载中...</p>
                    )}
                  </div>
                  {/* 右侧：医学原文 */}
                  <div style={{ background: '#fffbeb', borderRadius: '10px', padding: '12px', border: '1px solid #fde68a', overflowY: 'auto' }}>
                    <h4 style={{ fontSize: '16px', fontWeight: 'bold', color: '#b45309', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                      📋 医学原文
                    </h4>
                    <div style={{ fontSize: '16px', lineHeight: '2', color: '#374151' }}>
                      <div style={{ marginBottom: '10px' }}>
                        <span style={{ fontWeight: 'bold', color: '#92400e', fontSize: '17px' }}>【成分】</span>
                        <p style={{ margin: '2px 0' }}>{drugDetails.ingredient}</p>
                      </div>
                      <div style={{ marginBottom: '10px' }}>
                        <span style={{ fontWeight: 'bold', color: '#92400e', fontSize: '17px' }}>【适应症】</span>
                        <p style={{ margin: '2px 0' }}>{drugDetails.indications}</p>
                      </div>
                      <div style={{ marginBottom: '10px' }}>
                        <span style={{ fontWeight: 'bold', color: '#92400e', fontSize: '17px' }}>【用法用量】</span>
                        <p style={{ margin: '2px 0' }}>{drugDetails.usage}</p>
                      </div>
                      <div style={{ marginBottom: '10px' }}>
                        <span style={{ fontWeight: 'bold', color: '#92400e', fontSize: '17px' }}>【注意事项】</span>
                        <p style={{ margin: '2px 0' }}>{drugDetails.precautions}</p>
                      </div>
                      <div>
                        <span style={{ fontWeight: 'bold', color: '#92400e', fontSize: '17px' }}>【不良反应】</span>
                        <p style={{ margin: '2px 0' }}>{drugDetails.adverseReactions}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

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
                      dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(displayGuideHtml) }}
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

          {/* 追问功能区域 */}
          <div style={{
            marginTop: '16px',
            borderRadius: '12px',
            border: '1px solid #bfdbfe',
            overflow: 'hidden',
            background: '#fff'
          }}>
            {/* 标题栏 - 蓝色渐变 */}
            <div style={{
              padding: '10px 16px',
              background: 'linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%)',
              color: '#fff',
              fontSize: '15px',
              fontWeight: 'bold',
              display: 'flex',
              alignItems: 'center',
              gap: '6px'
            }}>
              💬 向药师追问
              <span style={{ fontSize: '11px', fontWeight: 'normal', opacity: 0.8 }}>有疑问随时问</span>
            </div>

            {/* 对话消息区域 */}
            <div ref={followUpMessagesRef} style={{
              maxHeight: '300px',
              overflowY: 'auto',
              padding: '12px',
              background: '#f8fafc'
            }}>
              {followUpMessages.length === 0 && !isFollowUpLoading && (
                <p style={{ textAlign: 'center', color: '#94a3b8', fontSize: '14px', padding: '20px 0' }}>
                  如果对用药说明有疑问，请在下方输入您的问题
                </p>
              )}
              {followUpMessages.map((msg, idx) => {
                const isAssistant = msg.role === 'assistant';
                const isThisSpeaking = speakingFollowUpIdx === idx && isFollowUpSpeaking;
                return (
                  <div key={idx} style={{
                    display: 'flex',
                    gap: '8px',
                    marginBottom: '10px',
                    flexDirection: isAssistant ? 'row' : 'row-reverse'
                  }}>
                    {isAssistant && (
                      <div style={{
                        width: '32px', height: '32px', borderRadius: '50%',
                        background: '#3b82f6', color: '#fff',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: '16px', flexShrink: 0
                      }}>👨‍⚕️</div>
                    )}
                    <div style={{
                      display: 'flex', flexDirection: 'column',
                      maxWidth: '75%',
                      alignItems: isAssistant ? 'flex-start' : 'stretch'
                    }}>
                      <div style={{
                        padding: '10px 14px',
                        borderRadius: '12px',
                        fontSize: '15px',
                        lineHeight: '1.6',
                        whiteSpace: 'pre-wrap',
                        background: isAssistant ? '#fff' : '#3b82f6',
                        color: isAssistant ? '#1e293b' : '#fff',
                        border: isAssistant ? '1px solid #e2e8f0' : 'none',
                        boxShadow: '0 1px 2px rgba(0,0,0,0.05)'
                      }}>
                        {msg.content}
                      </div>
                      {isAssistant && msg.content && (
                        <button
                          onClick={() => toggleFollowUpSpeech(idx, msg.content)}
                          title={isThisSpeaking ? '停止播放' : '播放语音'}
                          style={{
                            marginTop: '4px', padding: '4px 8px', fontSize: '16px',
                            color: isThisSpeaking ? '#dc2626' : '#2563eb',
                            background: '#fff',
                            border: `1px solid ${isThisSpeaking ? '#fecaca' : '#bfdbfe'}`,
                            borderRadius: '50%', cursor: 'pointer',
                            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                            width: '32px', height: '32px', alignSelf: 'flex-start', lineHeight: 1
                          }}
                        >
                          {isThisSpeaking ? '🔊' : '🔇'}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
              {isFollowUpLoading && (
                <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                  <div style={{
                    width: '32px', height: '32px', borderRadius: '50%',
                    background: '#3b82f6', color: '#fff',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '16px', flexShrink: 0
                  }}>👨‍⚕️</div>
                  <div style={{
                    padding: '10px 14px', borderRadius: '12px',
                    background: '#fff', border: '1px solid #e2e8f0',
                    fontSize: '14px', color: '#64748b'
                  }}>
                    正在思考...
                  </div>
                </div>
              )}
            </div>

            {/* 输入区域 */}
            <div style={{
              padding: '10px 12px',
              borderTop: '1px solid #e2e8f0',
              background: '#fff',
              display: 'flex',
              gap: '8px'
            }}>
              <input
                type="text"
                value={followUpQuestion}
                onChange={(e) => setFollowUpQuestion(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleAskFollowUp(); } }}
                placeholder="输入您的问题，回车发送..."
                disabled={isFollowUpLoading}
                style={{
                  flex: 1, padding: '8px 12px', fontSize: '15px',
                  border: '1px solid #cbd5e1', borderRadius: '8px',
                  outline: 'none'
                }}
              />
              <button
                onClick={handleAskFollowUp}
                disabled={!followUpQuestion.trim() || isFollowUpLoading}
                style={{
                  padding: '8px 16px', fontSize: '15px',
                  background: (!followUpQuestion.trim() || isFollowUpLoading) ? '#94a3b8' : '#3b82f6',
                  color: '#fff', border: 'none', borderRadius: '8px',
                  cursor: (!followUpQuestion.trim() || isFollowUpLoading) ? 'not-allowed' : 'pointer',
                  whiteSpace: 'nowrap'
                }}
              >
                发送
              </button>
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

              {/* 突出的加入药箱按钮 - 已在药箱中则隐藏 */}
              {!drugList.some(d => d.name === drugInfo.name) && (
              <div className="add-to-box-prominent">
                <button
                  className="btn btn-success btn-extra-large"
                  onClick={() => addToMedicineBox(drugInfo)}
                >
                  <span className="btn-icon">➕</span>
                  <span className="btn-text">加入我的药箱</span>
                </button>
              </div>
              )}
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
    // 冲突规则缓存 key
    const CONFLICT_RULES_CACHE_KEY = 'conflict_rules_cache';

    // 从本地缓存加载冲突规则（断网时使用）
    const loadConflictFromCache = () => {
      try {
        const cached = localStorage.getItem(CONFLICT_RULES_CACHE_KEY);
        if (cached) {
          const { report, drugNames } = JSON.parse(cached);
          
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
        

        const { data } = await authFetch('/api/conflict/check', {
          method: 'POST',
          body: JSON.stringify(drugNames)
        });

        if (data.code === 200 && data.data) {
          setConflictReport(data.data);
          
          // 保存到本地缓存（用于断网可读）
          try {
            localStorage.setItem(CONFLICT_RULES_CACHE_KEY, JSON.stringify({
              report: data.data,
              drugNames: drugNames,
              timestamp: Date.now()
            }));
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
          `检测时间: ${formatDateTime(conflictReport.checkTime)}`,
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

    // 场景化选项配置（精简预设，鼓励用户自定义）
    const scenarioOptions = [
      { id: 'grapefruit', label: '🍊 西柚汁', type: 'beverage', value: '西柚汁' },
      { id: 'alcohol', label: '🍺 酒', type: 'beverage', value: '酒精' },
      { id: 'coffee', label: '☕ 咖啡', type: 'beverage', value: '咖啡' },
      { id: 'tea', label: '🍵 浓茶', type: 'beverage', value: '浓茶' },
      { id: 'vitamin_c', label: '💊 维C', type: 'supplement', value: '维生素C' },
      { id: 'calcium', label: '💊 钙片', type: 'supplement', value: '钙片' },
      { id: 'ginseng', label: '🌿 人参', type: 'supplement', value: '人参' },
      { id: 'seafood', label: '🦐 海鲜', type: 'food', value: '海鲜' },
    ];

    // 切换场景标签选中状态
    const toggleScenario = (scenario) => {
      setSelectedScenarios(prev => {
        const exists = prev.find(s => s.id === scenario.id);
        if (exists) return prev.filter(s => s.id !== scenario.id);
        return [...prev, scenario];
      });
      setScenarioConflictReport(null);
    };

    // 添加自定义食物
    const addCustomFood = () => {
      const trimmed = customFoodInput.trim();
      if (!trimmed) return;
      const customId = 'custom_' + Date.now();
      setSelectedScenarios(prev => [...prev, { id: customId, label: trimmed, type: 'food', value: trimmed }]);
      setCustomFoodInput('');
      setScenarioConflictReport(null);
    };

    // 调用 /analyze 端点进行综合冲突检测
    const handleAnalyzeConflicts = async () => {
      if (drugList.length === 0) {
        showToast('请先添加药品到药箱', 'warning');
        return;
      }
      if (selectedScenarios.length === 0) {
        showToast('请至少选择一个场景选项', 'warning');
        return;
      }

      setIsCheckingScenario(true);
      setScenarioConflictReport(null);

      try {
        const drugNames = drugList.map(d => d.name);
        const supplements = selectedScenarios.filter(s => s.type === 'supplement').map(s => s.value);
        const beverages = selectedScenarios.filter(s => s.type === 'beverage').map(s => s.value);
        const foods = selectedScenarios.filter(s => s.type === 'food').map(s => s.value);

        const requestBody = {
          drugNames,
          supplements: supplements.length > 0 ? supplements : undefined,
          beverages: beverages.length > 0 ? beverages : undefined,
          foods: foods.length > 0 ? foods : undefined,
          detailed: true,
          includeAlternatives: true
        };


        const { data } = await authFetch('/api/conflict/analyze', {
          method: 'POST',
          body: JSON.stringify(requestBody)
        });

        if (data.code === 200 && data.data) {
          setScenarioConflictReport(data.data);
        } else {
          showToast(data.message || '综合冲突检测失败', 'error');
        }
      } catch (error) {
        console.error('综合冲突检测异常:', error);
        showToast('网络错误，请稍后重试', 'error');
      } finally {
        setIsCheckingScenario(false);
      }
    };

    return (
      <div className="card">
        <h2 className="card-title">
          <span className="card-title-icon">⚠️</span>
          用药安全检查
        </h2>

        {/* 自动快速检测结果横幅 */}
        {isAutoChecking && (
          <div style={{
            borderRadius: '12px',
            padding: '14px 16px',
            marginBottom: '20px',
            background: '#f0f9ff',
            border: '1px solid #bae6fd',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
          }}>
            <div className="loading-spinner" style={{ width: '18px', height: '18px', borderWidth: '2px' }}></div>
            <span style={{ fontSize: '14px', color: '#0369a1' }}>正在自动检测药品冲突...</span>
          </div>
        )}
        {!isAutoChecking && autoCheckResult && autoCheckResult.conflicts && autoCheckResult.conflicts.length > 0 && (
          <div style={{
            borderRadius: '12px',
            padding: '14px 16px',
            marginBottom: '20px',
            background: autoCheckResult.hasSevereConflict ? '#fef2f2' : '#fffbeb',
            border: autoCheckResult.hasSevereConflict ? '1px solid #fca5a5' : '1px solid #fcd34d',
            animation: 'pulse-border 2s ease-in-out infinite'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <span style={{ fontSize: '20px' }}>{autoCheckResult.hasSevereConflict ? '🔴' : '🟡'}</span>
              <span style={{
                fontSize: '15px',
                fontWeight: 'bold',
                color: autoCheckResult.hasSevereConflict ? '#dc2626' : '#d97706'
              }}>
                {autoCheckResult.hasSevereConflict ? '发现严重冲突！' : '发现潜在冲突'}
              </span>
            </div>
            <p style={{ fontSize: '13px', color: '#555', marginBottom: '8px' }}>
              快速检测发现 {autoCheckResult.conflicts.length} 个冲突项
              {autoCheckResult.statistics && (
                <>（
                {autoCheckResult.statistics.severeCount > 0 && <span style={{ color: '#dc2626', fontWeight: 'bold' }}>{autoCheckResult.statistics.severeCount}严重 </span>}
                {autoCheckResult.statistics.moderateCount > 0 && <span style={{ color: '#d97706' }}>{autoCheckResult.statistics.moderateCount}中度 </span>}
                {autoCheckResult.statistics.mildCount > 0 && <span style={{ color: '#ca8a04' }}>{autoCheckResult.statistics.mildCount}轻微</span>}
                ）</>
              )}
            </p>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
              {autoCheckResult.conflicts.slice(0, 3).map((c, i) => (
                <span key={i} style={{
                  padding: '3px 10px',
                  borderRadius: '8px',
                  fontSize: '12px',
                  background: c.severity === 'SEVERE' ? '#fee2e2' : c.severity === 'MODERATE' ? '#fef3c7' : '#f0fdf4',
                  color: c.severity === 'SEVERE' ? '#dc2626' : c.severity === 'MODERATE' ? '#d97706' : '#16a34a'
                }}>
                  {c.drugA} × {c.drugB}
                </span>
              ))}
              {autoCheckResult.conflicts.length > 3 && (
                <span style={{ padding: '3px 10px', borderRadius: '8px', fontSize: '12px', background: '#f1f5f9', color: '#64748b' }}>
                  +{autoCheckResult.conflicts.length - 3}项
                </span>
              )}
            </div>
            <p style={{ fontSize: '12px', color: '#888', marginTop: '10px' }}>
              以上为本地规则快速检测结果，点击下方"开始检测"可获取AI深度分析
            </p>
          </div>
        )}
        {!isAutoChecking && autoCheckResult && (!autoCheckResult.conflicts || autoCheckResult.conflicts.length === 0) && drugList.length >= 2 && !conflictReport && (
          <div style={{
            borderRadius: '12px',
            padding: '14px 16px',
            marginBottom: '20px',
            background: '#f0fdf4',
            border: '1px solid #86efac'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '20px' }}>🟢</span>
              <span style={{ fontSize: '14px', color: '#16a34a', fontWeight: 'bold' }}>快速检测未发现明显冲突</span>
            </div>
            <p style={{ fontSize: '12px', color: '#888', marginTop: '6px' }}>
              本地规则未发现冲突，如需更全面的分析请点击"开始检测"
            </p>
          </div>
        )}

        {/* 药品/食物/保健品综合冲突 - 折叠面板 */}
        <div style={{
          borderRadius: '12px',
          marginBottom: '20px',
          border: '1px solid #bae6fd',
          overflow: 'hidden'
        }}>
          {/* 折叠标题栏 */}
          <div
            onClick={() => setShowScenarioPanel(!showScenarioPanel)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 16px',
              background: 'linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)',
              cursor: 'pointer',
              userSelect: 'none'
            }}
          >
            <span style={{ fontSize: '15px', fontWeight: 'bold', color: '#0369a1', display: 'flex', alignItems: 'center', gap: '6px' }}>
              🍽️ 药品/食物/保健品综合冲突
              {selectedScenarios.length > 0 && (
                <span style={{
                  fontSize: '11px', padding: '2px 8px', borderRadius: '10px',
                  background: '#0284c7', color: 'white'
                }}>
                  {selectedScenarios.length}项
                </span>
              )}
            </span>
            <span style={{ fontSize: '14px', color: '#0369a1', transition: 'transform 0.2s', transform: showScenarioPanel ? 'rotate(180deg)' : 'rotate(0deg)' }}>
              ▼
            </span>
          </div>

          {/* 折叠内容 */}
          {showScenarioPanel && (
            <div style={{ padding: '14px 16px', background: '#f8fafc' }}>
              <p style={{ fontSize: '13px', color: '#64748b', marginBottom: '10px' }}>
                选择今天的饮食/保健品，AI分析与您药品是否冲突
              </p>

              {/* 场景标签选择 */}
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginBottom: '10px' }}>
                {scenarioOptions.map(scenario => {
                  const isSelected = selectedScenarios.some(s => s.id === scenario.id);
                  return (
                    <button
                      key={scenario.id}
                      onClick={() => toggleScenario(scenario)}
                      style={{
                        padding: '5px 12px',
                        borderRadius: '16px',
                        fontSize: '13px',
                        border: isSelected ? '2px solid #0284c7' : '1.5px solid #cbd5e1',
                        background: isSelected ? '#0284c7' : '#ffffff',
                        color: isSelected ? '#ffffff' : '#475569',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease'
                      }}
                    >
                      {scenario.label}
                    </button>
                  );
                })}
              </div>

              {/* 自定义输入 */}
              <div style={{ display: 'flex', gap: '6px', marginBottom: '10px' }}>
                <input
                  type="text"
                  value={customFoodInput}
                  onChange={(e) => setCustomFoodInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && addCustomFood()}
                  placeholder="输入其他食物/饮品，回车添加"
                  style={{
                    flex: 1, padding: '7px 12px', borderRadius: '8px',
                    border: '1.5px solid #cbd5e1', fontSize: '13px', outline: 'none'
                  }}
                />
                <button
                  onClick={addCustomFood}
                  style={{
                    padding: '7px 14px', borderRadius: '8px', border: 'none',
                    background: '#0284c7', color: 'white', fontSize: '13px', cursor: 'pointer'
                  }}
                >
                  添加
                </button>
              </div>

              {/* 已选标签 */}
              {selectedScenarios.length > 0 && (
                <div style={{ marginBottom: '10px' }}>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px' }}>
                    {selectedScenarios.map(s => (
                      <span key={s.id} style={{
                        padding: '3px 10px', borderRadius: '10px', fontSize: '11px',
                        background: s.type === 'beverage' ? '#dbeafe' : s.type === 'supplement' ? '#fce7f3' : '#dcfce7',
                        color: s.type === 'beverage' ? '#1d4ed8' : s.type === 'supplement' ? '#be185d' : '#15803d',
                        display: 'flex', alignItems: 'center', gap: '3px'
                      }}>
                        {s.value}
                        <span onClick={() => toggleScenario(s)} style={{ cursor: 'pointer', fontWeight: 'bold' }}>×</span>
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* 检测按钮 */}
              <button
                className="btn btn-primary"
                onClick={handleAnalyzeConflicts}
                disabled={isCheckingScenario || drugList.length === 0 || selectedScenarios.length === 0}
                style={{
                  width: '100%', minHeight: '40px', fontSize: '14px',
                  opacity: (isCheckingScenario || drugList.length === 0 || selectedScenarios.length === 0) ? 0.6 : 1
                }}
              >
                {isCheckingScenario ? '🔍 AI分析中...' : '🔬 综合冲突检测'}
              </button>
            </div>
          )}
        </div>

        {/* 综合冲突检测结果 */}
        {isCheckingScenario && (
          <div style={{ textAlign: 'center', padding: '20px' }}>
            <div className="loading-spinner-container" style={{ margin: '0 auto 12px' }}>
              <div className="loading-spinner"></div>
            </div>
            <p style={{ fontSize: '14px', color: 'var(--text-primary)' }}>
              🔍 AI分析中：{drugList.map(d => d.name).join('、')} × {selectedScenarios.map(s => s.value).join('、')}
            </p>
          </div>
        )}

        {scenarioConflictReport && !isCheckingScenario && (
          <div style={{
            borderRadius: '12px',
            padding: '14px',
            marginBottom: '16px',
            border: scenarioConflictReport.hasSevereConflict ? '1px solid #fca5a5' : '1px solid #86efac',
            background: scenarioConflictReport.hasSevereConflict ? '#fef2f2' : '#f0fdf4'
          }}>
            <h4 style={{
              fontSize: '15px', fontWeight: 'bold', marginBottom: '10px',
              color: scenarioConflictReport.hasSevereConflict ? '#dc2626' : '#16a34a',
              display: 'flex', alignItems: 'center', gap: '6px'
            }}>
              {scenarioConflictReport.hasSevereConflict ? '🔴 发现严重冲突' : '🟢 未发现严重冲突'}
            </h4>

            {/* 检测范围 */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '10px' }}>
              {scenarioConflictReport.drugsChecked?.length > 0 && (
                <span style={{ padding: '2px 8px', borderRadius: '6px', fontSize: '14px', background: '#dbeafe', color: '#1d4ed8' }}>
                  💊{scenarioConflictReport.drugsChecked.join('、')}
                </span>
              )}
              {scenarioConflictReport.beveragesChecked?.length > 0 && (
                <span style={{ padding: '2px 8px', borderRadius: '6px', fontSize: '14px', background: '#fef3c7', color: '#92400e' }}>
                  🥤{scenarioConflictReport.beveragesChecked.join('、')}
                </span>
              )}
              {scenarioConflictReport.supplementsChecked?.length > 0 && (
                <span style={{ padding: '2px 8px', borderRadius: '6px', fontSize: '14px', background: '#fce7f3', color: '#9d174d' }}>
                  💊{scenarioConflictReport.supplementsChecked.join('、')}
                </span>
              )}
              {scenarioConflictReport.foodsChecked?.length > 0 && (
                <span style={{ padding: '2px 8px', borderRadius: '6px', fontSize: '14px', background: '#dcfce7', color: '#15803d' }}>
                  🍽️{scenarioConflictReport.foodsChecked.join('、')}
                </span>
              )}
            </div>

            {/* 统计 */}
            {scenarioConflictReport.conflicts?.length > 0 && (
              <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                {scenarioConflictReport.statistics?.severeCount > 0 && (
                  <span style={{ padding: '4px 10px', borderRadius: '8px', background: '#dc2626', color: 'white', fontSize: '14px' }}>
                    严重×{scenarioConflictReport.statistics.severeCount}
                  </span>
                )}
                {scenarioConflictReport.statistics?.moderateCount > 0 && (
                  <span style={{ padding: '4px 10px', borderRadius: '8px', background: '#ea580c', color: 'white', fontSize: '14px' }}>
                    中度×{scenarioConflictReport.statistics.moderateCount}
                  </span>
                )}
                {scenarioConflictReport.statistics?.mildCount > 0 && (
                  <span style={{ padding: '4px 10px', borderRadius: '8px', background: '#ca8a04', color: 'white', fontSize: '14px' }}>
                    轻微×{scenarioConflictReport.statistics.mildCount}
                  </span>
                )}
              </div>
            )}

            {/* 冲突详情 */}
            {scenarioConflictReport.conflicts?.map((conflict, index) => (
              <div
                key={index}
                className={`conflict-item conflict-level-${conflict.severity?.toLowerCase() || 'mild'}`}
                style={{ marginBottom: '8px', padding: '8px 10px' }}
              >
                <span className={`conflict-badge ${conflict.severity?.toLowerCase() || 'mild'}`}>
                  {conflict.severity === 'SEVERE' && '🔴 严重'}
                  {conflict.severity === 'MODERATE' && '🟡 中度'}
                  {conflict.severity === 'MILD' && '🔵 轻微'}
                  {(!conflict.severity || conflict.severity === 'NONE') && '🟢 安全'}
                </span>
                <div className="drug-connection">
                  <div className="drug-node">{conflict.drugA}</div>
                  <span className="drug-connector">⚡</span>
                  <div className="drug-node">{conflict.drugB}</div>
                </div>
                {conflict.conflictExplanation && (
                  <p className="conflict-explanation-text">{conflict.conflictExplanation}</p>
                )}
                {conflict.riskWarning && (
                  <p className="conflict-explanation-text" style={{
                    color: conflict.severity === 'SEVERE' ? '#dc2626' : conflict.severity === 'MODERATE' ? '#ea580c' : '#856404',
                    fontWeight: 'bold', marginTop: '4px', fontSize: '14px'
                  }}>
                    ⚠️ {conflict.riskWarning}
                  </p>
                )}
                {conflict.alternatives?.length > 0 && (
                  <div style={{ marginTop: '4px' }}>
                    {conflict.alternatives.map((alt, i) => (
                      <p key={i} style={{ fontSize: '14px', color: '#666', marginLeft: '8px' }}>💡 {alt}</p>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {/* 无冲突 */}
            {scenarioConflictReport.conflicts?.length === 0 && (
              <div style={{ textAlign: 'center', padding: '10px' }}>
                <span style={{ fontSize: '32px' }}>🛡️</span>
                <p style={{ fontSize: '14px', fontWeight: 'bold', color: '#16a34a' }}>未发现冲突</p>
              </div>
            )}

            {/* 总体建议 */}
            {scenarioConflictReport.generalAdvice && (
              <div className="warning-box" style={{ marginTop: '10px', background: '#e0f2fe', padding: '8px 10px' }}>
                <p style={{ fontSize: '14px', color: '#075985' }}>💊 {scenarioConflictReport.generalAdvice}</p>
              </div>
            )}

            <button
              className="btn btn-secondary"
              style={{ width: '100%', marginTop: '10px', fontSize: '14px', minHeight: '36px' }}
              onClick={() => { setScenarioConflictReport(null); }}
            >
              🔄 重新检测
            </button>
          </div>
        )}

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
              <p style={{ fontSize: '15px', color: 'var(--text-light)', marginTop: '12px' }}>
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
                  <div style={{ fontSize: '14px' }}>严重冲突</div>
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
                  <div style={{ fontSize: '14px' }}>中度冲突</div>
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
                  <div style={{ fontSize: '14px' }}>轻微注意</div>
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
                        <p key={altIndex} style={{ fontSize: '14px', color: '#666', marginLeft: '12px' }}>
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
            <p style={{ fontSize: '15px', color: '#666', marginTop: '12px' }}>
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
                  <p style={{ fontSize: '15px', color: '#666', marginBottom: '24px' }}>
                    您最近添加了新药品，药箱中共有 {drugList.length} 种药品
                  </p>
                </>
              ) : (
                <>
                  <div style={{ fontSize: '64px', marginBottom: '16px' }}>🔍</div>
                  <h3 style={{ fontSize: '20px', marginBottom: '12px' }}>
                    AI智能冲突检测
                  </h3>
                  <p style={{ fontSize: '15px', color: '#666', marginBottom: '24px' }}>
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
          <span className="card-title-icon"></span>
          {calendarViewMode === 'today' ? '今日用药时间轴' : '一周用药记录'}
        </h2>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          {/* AI周报显示/隐藏按钮 - 仅在周视图显示 */}
          {calendarViewMode === 'week' && (
            <button
              className="btn"
              onClick={() => {
                setShowWeeklyReport(!showWeeklyReport);
                if (selectedWeekDay) setSelectedWeekDay(null); // 打开AI周报时关闭日详情
              }}
              style={{ 
                minHeight: '40px', 
                fontSize: '16px',
                background: showWeeklyReport ? '#e91e63' : '#ff5722',
                color: 'white',
                border: 'none',
                fontWeight: 'bold',
                boxShadow: '0 4px 12px rgba(233, 30, 99, 0.3)',
                transition: 'all 0.3s ease'
              }}
            >
              {showWeeklyReport ? '📊 隐藏AI周报' : '📊 显示AI周报'}
            </button>
          )}
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

      {/* AI周报摘要 - 仅在周视图显示 */}
      {calendarViewMode === 'week' && showWeeklyReport && (
        <div style={{ marginTop: '32px', borderTop: '2px solid #e8f4fd', paddingTop: '24px' }}>
          <WeeklyReport compact />
          {/* 截图按钮 - 参考冲突检测模块 */}
          <div style={{ 
            display: 'flex', 
            gap: '16px', 
            marginTop: '20px'
          }}>
            <button
              className="btn btn-primary"
              onClick={async () => {
                // 找到WeeklyReport组件的reportRef并截图
                const reportContent = document.querySelector('.weekly-report-content');
                if (!reportContent) return;
                
                try {
                  showToast('正在生成截图...', 'info');
                  const canvas = await html2canvas(reportContent, {
                    backgroundColor: '#ffffff',
                    scale: 2,
                    useCORS: true,
                    logging: false
                  });
                  
                  const link = document.createElement('a');
                  const dateStr = new Date().toLocaleDateString('zh-CN').replace(/\//g, '-');
                  link.download = `用药周报_${dateStr}.png`;
                  link.href = canvas.toDataURL('image/png');
                  link.click();
                  showToast('截图已保存！', 'success');
                } catch (error) {
                  console.error('截图失败:', error);
                  showToast('截图失败，请稍后重试', 'error');
                }
              }}
              style={{
                flex: 1,
                background: '#2196F3',
                color: 'white',
                border: 'none',
                padding: '16px 32px',
                fontSize: '18px',
                fontWeight: 'bold',
                borderRadius: '12px',
                cursor: 'pointer',
                boxShadow: '0 6px 16px rgba(33, 150, 243, 0.4)',
                transition: 'all 0.3s ease',
                minHeight: '52px'
              }}
            >
               📷 截图报告
            </button>
            <button
              className="btn btn-secondary"
              onClick={async () => {
                // 获取AI总结文本并复制
                try {
                  const { data } = await authFetch(`/api/weekly-report/latest`);
                  
                  if (data.code === 200 && data.data?.fullReportText) {
                    await navigator.clipboard.writeText(data.data.fullReportText);
                    showToast('报告已复制到剪贴板！', 'success');
                  } else {
                    showToast('复制失败', 'error');
                  }
                } catch (error) {
                  console.error('复制失败:', error);
                  showToast('复制失败，请稍后重试', 'error');
                }
              }}
              style={{
                flex: 1,
                background: '#FF9800',
                color: 'white',
                border: 'none',
                padding: '16px 32px',
                fontSize: '18px',
                fontWeight: 'bold',
                borderRadius: '12px',
                cursor: 'pointer',
                boxShadow: '0 6px 16px rgba(255, 152, 0, 0.4)',
                transition: 'all 0.3s ease',
                minHeight: '52px'
              }}
            >
               📋 复制文本
            </button>
          </div>
        </div>
      )}
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
                        style={{ marginTop: '12px' }}
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

    // 计算一周总览统计
    const allItems = weeklyMedicationData.dailyRecords.flatMap(d => d.items || []);
    const weekTotal = allItems.length;
    const weekTaken = allItems.filter(i => normalizeWeekStatus(i.status) === 'taken').length;
    const weekRate = weekTotal > 0 ? Math.round((weekTaken / weekTotal) * 100) : 0;

    return (
      <div className="week-view">
        {/* 一周总览统计 */}
        <div className="week-summary">
          <div className="week-summary__ring">
            <svg viewBox="0 0 36 36" className="week-summary__svg">
              <path
                className="week-summary__ring-bg"
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
              <path
                className="week-summary__ring-fill"
                strokeDasharray={`${weekRate}, 100`}
                d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
              />
            </svg>
            <span className="week-summary__percent">{weekRate}%</span>
          </div>
          <div className="week-summary__info">
            <div className="week-summary__label">本周服药完成率</div>
            <div className="week-summary__detail">
              已服 <strong>{weekTaken}</strong> 项 / 共 <strong>{weekTotal}</strong> 项
            </div>
          </div>
        </div>

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
                onClick={() => {
                  handleWeekDayToggle(day.date);
                  if (showWeeklyReport) setShowWeeklyReport(false); // 打开日详情时关闭AI周报
                }}
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
                              style={{ marginTop: '12px' }}
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
      const { response, data } = await authFetch(`/api/v1/box/search?keyword=${encodeURIComponent(keyword)}&status=active`);
      
      
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
      <DrugManagementTab
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
        user={user}
      />
    );
  };

  return (
    <>
      {/* 百度TTS音频播放器 */}
      <audio ref={audioRef} style={{ display: 'none' }} />
      <audio ref={followUpAudioRef} style={{ display: 'none' }} />

      <div className="watermark-bg"></div>
      {!isLoggedIn ? (
        <AuthGate
          mode={loginMode}
          showRegister={showRegister}
          registerSuccess={registerSuccess}
          onLogin={handleLogin}
          onRegister={handleRegister}
          onShowRegister={() => {
            setRegisterSuccess('');
            setShowRegister(true);
          }}
          onSwitchToGuardian={() => {
            setRegisterSuccess('');
            setLoginMode('guardian');
          }}
          onSwitchToElder={() => {
            setRegisterSuccess('');
            setLoginMode('elder');
          }}
        />
      ) : loginMode === 'guardian' ? (
        <GuardianApp
          onLogout={() => {
            setUser(null);
            setIsLoggedIn(false);
            setLoginMode('elder');
          }}
        />
      ) : user?.role === 'family' ? (
        <GuardianApp user={user} onLogout={() => { setUser(null); setIsLoggedIn(false); clearAuth(); }} />
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

            <div key={activeTab} className="tab-page">
            {activeTab === 'home' && renderHomeTab()}
            {activeTab === 'upload' && renderUploadTab()}
            {activeTab === 'recognition' && renderRecognitionTab()}
            {activeTab === 'explanation' && renderExplanationTab()}
            {activeTab === 'conflict' && renderConflictTab()}
            {activeTab === 'calendar' && renderCalendarTab()}
            {activeTab === 'drugs' && renderDrugsTab()}
            {activeTab === 'emergency' && <EmergencyTab emergencyContacts={emergencyContacts} elderId={user?.id} />}
            </div>
          </div>

        </div>
      )}

      {/* 老人端通知面板 */}
      {user?.role !== 'family' && user?.id && (
        <ElderNotificationPanel
          isOpen={showNotificationPanel}
          onClose={() => setShowNotificationPanel(false)}
          onUnreadCountChange={setNotificationUnreadCount}
          onContactAdded={() => loadEmergencyContacts(user.id)}
          wsConnected={wsConnected}
        />
      )}

      {/* 语音交互入口 - 浮动麦克风按钮 */}
      {isLoggedIn && user?.role !== 'family' && (
        <FloatingMicButton
          onTranscript={(text) => {
            // 语音指令解析
            const cmd = text.trim().toLowerCase();
            const commands = [
              { keywords: ['首页', '主页', '回家', '回到首页'], tab: 'home', label: '首页' },
              { keywords: ['识别', '拍照', '扫描', '上传', '识别药品'], tab: 'upload', label: '识别药品' },
              { keywords: ['说明', '用药说明', '说明书', '药品说明'], tab: 'explanation', label: '用药说明' },
              { keywords: ['冲突', '冲突检测', '药物冲突', '检测冲突'], tab: 'conflict', label: '冲突检测' },
              { keywords: ['日历', '用药日历', '日程', '计划'], tab: 'calendar', label: '用药日历' },
              { keywords: ['药箱', '管理', '我的药箱', '药品管理', '药箱管理'], tab: 'drugs', label: '药箱管理' },
              { keywords: ['紧急', '急救', '求助', '救命', '紧急助手'], tab: 'emergency', label: '紧急助手' },
            ];
            const match = commands.find(c => c.keywords.some(k => cmd.includes(k)));
            if (match) {
              setActiveTab(match.tab);
              showToast(`已跳转到：${match.label}`, 'success');
            } else {
              showToast(`未识别指令："${text}"，请尝试说：冲突检测、紧急助手、识别药品等`, 'info');
            }
          }}
        />
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

      {showMyGuardians && (
        <MyGuardiansModal
          onClose={() => setShowMyGuardians(false)}
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

      {/* 批量识别药品确认弹窗 - 逐个确认 */}
      {showBatchConfirmModal && recognizedDrugs.length > 0 && batchDrugIndex < recognizedDrugs.length && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0, 0, 0, 0.6)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1000,
          padding: '20px',
          backdropFilter: 'blur(4px)'
        }}>
          <div 
            ref={batchConfirmModalRef}
            style={{
            background: 'white',
            borderRadius: '32px',
            padding: '48px',
            width: '100%',
            maxWidth: '600px',
            maxHeight: '90vh',
            overflowY: 'auto',
            boxShadow: '0 20px 60px rgba(0, 0, 0, 0.3)',
            position: 'relative'
          }}>
            {/* 关闭按钮 */}
            <button
              style={{
                position: 'absolute',
                top: '20px',
                right: '20px',
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                border: 'none',
                background: '#F5F5F5',
                fontSize: '24px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'all 0.3s ease'
              }}
              onClick={() => {
                setShowBatchConfirmModal(false);
                setBatchConfirmedDrugs([]);
                setBatchDrugIndex(0);
              }}
              onMouseEnter={(e) => e.target.style.background = '#E0E0E0'}
              onMouseLeave={(e) => e.target.style.background = '#F5F5F5'}
            >
              ✕
            </button>

            {/* 进度提示 */}
            <div style={{ textAlign: 'center', marginBottom: '24px' }}>
              <div style={{
                display: 'inline-block',
                padding: '8px 20px',
                background: 'linear-gradient(135deg, #4A90E2 0%, #357ABD 100%)',
                color: 'white',
                borderRadius: '20px',
                fontSize: '16px',
                fontWeight: 'bold'
              }}>
                {(() => {
                  // 计算当前步骤：找到当前药品在选中列表中的位置
                  const selectedDrugIds = Array.from(batchSelectedForAdd);
                  const currentIndexInSelected = selectedDrugIds.indexOf(recognizedDrugs[batchDrugIndex]?.id);
                  const currentStep = currentIndexInSelected >= 0 ? currentIndexInSelected + 1 : batchDrugIndex + 1;
                  const totalSteps = selectedDrugIds.length;
                  return `步骤 ${currentStep} / ${totalSteps}`;
                })()}
              </div>
            </div>

            {/* 标题 */}
            <div style={{ textAlign: 'center', marginBottom: '40px' }}>
              <div style={{ fontSize: '64px', marginBottom: '16px' }}>💊</div>
              <h2 style={{
                fontSize: '32px',
                fontWeight: '800',
                color: '#4A90E2',
                marginBottom: '8px'
              }}>
                确认药品信息
              </h2>
              <p style={{ fontSize: '18px', color: '#6B6B6B' }}>
                请完善以下药品的用药信息
              </p>
            </div>

            {/* 当前药品信息 */}
            <div style={{
              padding: '24px',
              background: 'linear-gradient(135deg, #E3F2FD 0%, #F1F8E9 100%)',
              borderRadius: '16px',
              border: '2px solid #4A90E2',
              marginBottom: '32px'
            }}>
              <h3 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '16px', color: '#3D3D3D' }}>
                {recognizedDrugs[batchDrugIndex].name}
              </h3>
              <p style={{ fontSize: '16px', color: '#6B6B6B', marginBottom: '8px' }}>
                <strong>规格：</strong>{recognizedDrugs[batchDrugIndex].spec || '未指定'}
              </p>
              <p style={{ fontSize: '16px', color: '#6B6B6B' }}>
                <strong>匹配度：</strong><span style={{ color: '#4CAF50', fontWeight: 'bold' }}>{recognizedDrugs[batchDrugIndex].matchScore}%</span>
              </p>
            </div>

            {/* 表单 - 使用key强制重新渲染 */}
            <form 
              key={`drug-form-${recognizedDrugs[batchDrugIndex]?.id || batchDrugIndex}`}
              onSubmit={(e) => {
              e.preventDefault();
              
              // 获取表单数据
              const formData = new FormData(e.target);
              const dosageAmount = formData.get('dosageAmount') || '1';
              const dosageUnit = formData.get('dosageUnit') || '片';
              const frequency = formData.get('frequency');
              const startDate = formData.get('startDate');
              const endDate = formData.get('endDate');
              const expiryDate = formData.get('expiryDate');
              const totalQuantity = formData.get('totalQuantity') || '30';
              
              // 验证必填项
              if (!frequency || !startDate || !endDate || !expiryDate) {
                showToast('请填写所有必填项', 'warning');
                return;
              }
              
              // 保存当前药品信息
              const confirmedDrug = {
                drugId: recognizedDrugs[batchDrugIndex].id,
                name: recognizedDrugs[batchDrugIndex].name,
                spec: recognizedDrugs[batchDrugIndex].spec,
                dosage: `${dosageAmount}${dosageUnit}`,
                frequency: frequency,
                startDate: startDate,
                endDate: endDate,
                expiryDate: expiryDate,
                totalQuantity: parseFloat(totalQuantity),
                status: 'active'
              };
              
              
              // 检查是否是最后一个选中的药品
              const selectedDrugIds = Array.from(batchSelectedForAdd);
              
              const currentIndex = selectedDrugIds.indexOf(recognizedDrugs[batchDrugIndex].id);
              
              const isLastDrug = currentIndex >= selectedDrugIds.length - 1;
              
              if (!isLastDrug) {
                // 不是最后一个，添加到列表并切换到下一个
                setBatchConfirmedDrugs(prev => [...prev, confirmedDrug]);
                
                // 找到下一个选中药品的索引
                const nextDrugId = selectedDrugIds[currentIndex + 1];
                const nextIndex = recognizedDrugs.findIndex(d => d.id === nextDrugId);
                setBatchDrugIndex(nextIndex);
                // 延迟滚动到顶部，确保DOM已更新
                setTimeout(() => {
                  if (batchConfirmModalRef.current) {
                    batchConfirmModalRef.current.scrollTop = 0;
                  }
                }, 100);
              } else {
                // 是最后一个药品，将所有药品一起添加
                
                // 重要：直接构建完整的药品列表，不依赖异步状态
                const finalDrugList = [...batchConfirmedDrugs, confirmedDrug];
                
                // 关闭弹窗
                setShowBatchConfirmModal(false);
                
                // 清空临时状态
                setBatchConfirmedDrugs([]);
                setBatchDrugIndex(0);
                
                // 使用 setTimeout 确保弹窗关闭后再执行批量添加
                setTimeout(() => {
                  // 直接传递完整的药品列表给批量添加函数
                  handleBatchAddAllDrugsWithList(finalDrugList);
                }, 100);
              }
            }}>
              {/* 每次用量 */}
              <div style={{ marginBottom: '28px' }}>
                <label style={{
                  fontSize: '20px',
                  fontWeight: '600',
                  marginBottom: '12px',
                  display: 'block',
                  color: '#3D3D3D'
                }}>
                  💉 每次用量 <span style={{ color: '#E74C3C' }}>*</span>
                </label>
                <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                  <input
                    type="text"
                    name="dosageAmount"
                    defaultValue="1"
                    placeholder="输入剂量"
                    style={{
                      flex: 1,
                      padding: '20px 24px',
                      fontSize: '20px',
                      border: '3px solid #F0EBE3',
                      borderRadius: '20px',
                      outline: 'none',
                      background: '#FAF7F2',
                      fontFamily: 'inherit'
                    }}
                  />
                  <select
                    name="dosageUnit"
                    defaultValue="片"
                    style={{
                      flex: 1,
                      padding: '20px 24px',
                      fontSize: '20px',
                      border: '3px solid #F0EBE3',
                      borderRadius: '20px',
                      outline: 'none',
                      background: '#FAF7F2',
                      fontFamily: 'inherit',
                      cursor: 'pointer'
                    }}
                  >
                    {/* 固体剂型 */}
                    <option value="片">片</option>
                    <option value="粒">粒</option>
                    <option value="丸">丸</option>
                    <option value="颗">颗</option>
                    <option value="胶囊">胶囊</option>
                    <option value="锭">锭</option>
                    
                    {/* 包装单位 */}
                    <option value="瓶">瓶</option>
                    <option value="支">支</option>
                    <option value="盒">盒</option>
                    <option value="袋">袋</option>
                    
                    {/* 液体剂型 */}
                    <option value="ml">ml</option>
                    <option value="L">L</option>
                    <option value="滴">滴</option>
                    <option value="喷">喷</option>
                    <option value="口服液">口服液</option>
                    <option value="糖浆">糖浆</option>
                    <option value="溶液">溶液</option>
                    <option value="混悬液">混悬液</option>
                    <option value="乳剂">乳剂</option>
                    
                    {/* 外用剂型 */}
                    <option value="贴">贴</option>
                    <option value="膏">膏</option>
                    <option value="霜">霜</option>
                    <option value="软膏">软膏</option>
                    <option value="凝胶">凝胶</option>
                    <option value="栓">栓</option>
                    <option value="洗剂">洗剂</option>
                    <option value="搽剂">搽剂</option>
                    
                    {/* 注射剂型 */}
                    <option value="针">针</option>
                    <option value="安瓿">安</option>
                    <option value="粉针">粉针</option>
                    <option value="水针">水针</option>
                    
                    {/* 重量单位 */}
                    <option value="g">g</option>
                    <option value="mg">mg</option>
                    <option value="μg">μg</option>
                    <option value="ng">ng</option>
                    <option value="kg">kg</option>
                  </select>
                </div>
              </div>

              {/* 用药频率 */}
              <div style={{ marginBottom: '28px' }}>
                <label style={{
                  fontSize: '20px',
                  fontWeight: '600',
                  marginBottom: '12px',
                  display: 'block',
                  color: '#3D3D3D'
                }}>
                   用药频率 <span style={{ color: '#E74C3C' }}>*</span>
                </label>
                <select
                  name="frequency"
                  required
                  style={{
                    width: '100%',
                    padding: '20px 24px',
                    fontSize: '20px',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    outline: 'none',
                    background: 'white',
                    fontFamily: 'inherit',
                    cursor: 'pointer'
                  }}
                >
                  <option value="">-- 请选择用药频率 --</option>
                  <option value="每日一次">每日一次</option>
                  <option value="每日两次">每日两次</option>
                  <option value="每日三次">每日三次</option>
                  <option value="每日四次">每日四次</option>
                  <option value="隔日一次">隔日一次</option>
                  <option value="每周一次">每周一次</option>
                  <option value="必要时服用">必要时服用</option>
                  <option value="睡前服用">睡前服用</option>
                  <option value="饭前服用">饭前服用</option>
                  <option value="饭后服用">饭后服用</option>
                </select>
              </div>

              {/* 开始服药日期 */}
              <div style={{ marginBottom: '28px' }}>
                <label style={{
                  fontSize: '20px',
                  fontWeight: '600',
                  marginBottom: '12px',
                  display: 'block',
                  color: '#3D3D3D'
                }}>
                   开始服药日期 <span style={{ color: '#E74C3C' }}>*</span>
                </label>
                <input
                  type="date"
                  name="startDate"
                  required
                  defaultValue={new Date().toISOString().split('T')[0]}
                  style={{
                    width: '100%',
                    padding: '20px 24px',
                    fontSize: '20px',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    outline: 'none',
                    background: '#FAF7F2',
                    fontFamily: 'inherit'
                  }}
                />
              </div>

              {/* 结束服药日期 */}
              <div style={{ marginBottom: '28px' }}>
                <label style={{
                  fontSize: '20px',
                  fontWeight: '600',
                  marginBottom: '12px',
                  display: 'block',
                  color: '#3D3D3D'
                }}>
                  📅 结束服药日期 <span style={{ color: '#E74C3C' }}>*</span>
                </label>
                <input
                  type="date"
                  name="endDate"
                  required
                  defaultValue={new Date(Date.now() + 90 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]}
                  style={{
                    width: '100%',
                    padding: '20px 24px',
                    fontSize: '20px',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    outline: 'none',
                    background: '#FAF7F2',
                    fontFamily: 'inherit'
                  }}
                />
              </div>

              {/* 有效期 */}
              <div style={{ marginBottom: '28px' }}>
                <label style={{
                  fontSize: '20px',
                  fontWeight: '600',
                  marginBottom: '12px',
                  display: 'block',
                  color: '#3D3D3D'
                }}>
                  📅 有效期 <span style={{ color: '#E74C3C' }}>*</span>
                </label>
                <input
                  type="date"
                  name="expiryDate"
                  required
                  defaultValue={new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]}
                  style={{
                    width: '100%',
                    padding: '20px 24px',
                    fontSize: '20px',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    outline: 'none',
                    background: '#FAF7F2',
                    fontFamily: 'inherit'
                  }}
                />
              </div>

              {/* 总数量 */}
              <div style={{ marginBottom: '36px' }}>
                <label style={{
                  fontSize: '20px',
                  fontWeight: '600',
                  marginBottom: '12px',
                  display: 'block',
                  color: '#3D3D3D'
                }}>
                  🔢 总数量 <span style={{ color: '#E74C3C' }}>*</span>
                </label>
                <input
                  type="number"
                  name="totalQuantity"
                  defaultValue="30"
                  min="1"
                  step="0.1"
                  style={{
                    width: '100%',
                    padding: '20px 24px',
                    fontSize: '20px',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    outline: 'none',
                    background: '#FAF7F2',
                    fontFamily: 'inherit',
                    // 完全隐藏并禁用数字输入框的滚动调整条
                    MozAppearance: 'textfield',
                    WebkitAppearance: 'none',
                    appearance: 'none'
                  }}
                />
              </div>

              {/* 按钮组 */}
              <div style={{
                display: 'flex',
                gap: '20px',
                justifyContent: 'center'
              }}>
                <button
                  type="button"
                  onClick={() => {
                    setShowBatchConfirmModal(false);
                    setBatchConfirmedDrugs([]);
                    setBatchDrugIndex(0);
                  }}
                  style={{
                    flex: 1,
                    padding: '24px 40px',
                    fontSize: '22px',
                    fontWeight: '700',
                    border: '3px solid #F0EBE3',
                    borderRadius: '20px',
                    background: 'white',
                    color: '#6B6B6B',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease'
                  }}
                >
                  取消
                </button>
                <button
                  type="submit"
                  style={{
                    flex: 1,
                    padding: '24px 40px',
                    fontSize: '22px',
                    fontWeight: '700',
                    border: 'none',
                    borderRadius: '20px',
                    background: 'linear-gradient(135deg, #4A90E2 0%, #357ABD 100%)',
                    color: 'white',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease',
                    boxShadow: '0 8px 24px rgba(74, 144, 226, 0.3)'
                  }}
                >
                  {(() => {
                    // 计算当前药品在选中列表中的位置
                    const selectedDrugIds = Array.from(batchSelectedForAdd);
                    const currentIndexInSelected = selectedDrugIds.indexOf(recognizedDrugs[batchDrugIndex]?.id);
                    const isLastDrug = currentIndexInSelected >= selectedDrugIds.length - 1;
                    return isLastDrug ? '✅ 全部添加' : '✅ 下一步';
                  })()}
                </button>
              </div>
            </form>
          </div>
        </div>
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
                  检测时间: {formatDateTime(conflictReport.checkTime)}
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

      {/* 缺药预警详情弹窗 */}
      {showShortageDetail && shortageWarnings.length > 0 && (
        <div className="modal-overlay" onClick={() => setShowShortageDetail(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '550px' }}>
            <div className="modal-header">
              <h3 className="modal-title" style={{ color: '#e67e22' }}>⚠️ 缺药预警</h3>
              <button className="modal-close-btn" onClick={() => setShowShortageDetail(false)}>✕</button>
            </div>
            <div className="modal-body" style={{ padding: '20px' }}>
              <div style={{ textAlign: 'center', marginBottom: '16px' }}>
                <div style={{ fontSize: '40px', marginBottom: '8px' }}>
                  {shortageWarnings.some(w => w.warningLevel === 'critical') ? '🚨' : '⚠️'}
                </div>
                <p style={{ fontSize: '16px', color: '#333', fontWeight: '600' }}>
                  您有 <span style={{ color: '#e74c3c', fontSize: '20px' }}>{shortageWarnings.length}</span> 种药品即将用尽
                </p>
                <p style={{ fontSize: '13px', color: '#888', marginTop: '4px' }}>
                  建议尽快补充，避免断药影响治疗
                </p>
              </div>

              {/* 预警药品列表 */}
              <div style={{ maxHeight: '300px', overflowY: 'auto' }}>
                {shortageWarnings.map((warning, index) => (
                  <div
                    key={warning.boxItemId || index}
                    style={{
                      background: warning.warningLevel === 'critical'
                        ? 'linear-gradient(135deg, #fff5f5 0%, #ffe0e0 100%)'
                        : warning.warningLevel === 'urgent'
                          ? 'linear-gradient(135deg, #fff8f0 0%, #ffe8cc 100%)'
                          : 'linear-gradient(135deg, #fffff0 0%, #fff8dc 100%)',
                      borderRadius: '10px',
                      padding: '12px 14px',
                      marginBottom: '10px',
                      borderLeft: `4px solid ${
                        warning.warningLevel === 'critical' ? '#e74c3c'
                          : warning.warningLevel === 'urgent' ? '#e67e22' : '#f39c12'
                      }`
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div>
                        <span style={{ fontWeight: '600', fontSize: '15px', color: '#333' }}>
                          {warning.drugName}
                        </span>
                        {warning.specification && (
                          <span style={{ fontSize: '12px', color: '#999', marginLeft: '6px' }}>
                            {warning.specification}
                          </span>
                        )}
                      </div>
                      <span style={{
                        fontSize: '12px',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontWeight: '600',
                        color: 'white',
                        background: warning.warningLevel === 'critical' ? '#e74c3c'
                          : warning.warningLevel === 'urgent' ? '#e67e22' : '#f39c12'
                      }}>
                        {warning.warningLevelDesc}
                      </span>
                    </div>
                    <div style={{ marginTop: '6px', fontSize: '13px', color: '#666', display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
                      <span>用量：{warning.dosage}</span>
                      <span>频率：{warning.frequency}</span>
                      <span>剩余：{warning.remainingQuantity ?? 0}份</span>
                    </div>
                    <div style={{ marginTop: '4px', fontSize: '14px', fontWeight: '600',
                      color: warning.remainingDays <= 0 ? '#e74c3c' : '#e67e22'
                    }}>
                      {warning.remainingDays <= 0
                        ? '药品已用尽，请立即补充'
                        : `预计还可服用${warning.remainingDays}天`}
                    </div>
                  </div>
                ))}
              </div>

              {/* 快捷操作按钮 */}
              <div style={{ display: 'flex', gap: '12px', marginTop: '20px' }}>
                <button
                  className="btn"
                  style={{
                    flex: 1,
                    padding: '12px',
                    fontSize: '15px',
                    fontWeight: '600',
                    background: 'linear-gradient(135deg, #4A90E2 0%, #357ABD 100%)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '10px',
                    cursor: 'pointer'
                  }}
                  onClick={() => {
                    setShowShortageDetail(false);
                    setActiveTab('drugs');
                  }}
                >
                  🏪 去购药
                </button>
                <button
                  className="btn"
                  style={{
                    flex: 1,
                    padding: '12px',
                    fontSize: '15px',
                    fontWeight: '600',
                    background: 'linear-gradient(135deg, #2ecc71 0%, #27ae60 100%)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '10px',
                    cursor: 'pointer'
                  }}
                  onClick={() => {
                    setShowShortageDetail(false);
                    setActiveTab('emergency');
                  }}
                >
                  🏥 在线问诊
                </button>
              </div>
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
              检测时间: {formatDateTime(conflictReport.checkTime)}
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
