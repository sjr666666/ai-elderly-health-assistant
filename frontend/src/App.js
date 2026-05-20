import React, { useState, useRef, useEffect } from 'react';
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

function App() {
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
  const [showCelebration, setShowCelebration] = useState(false);
  const [particles, setParticles] = useState([]);
  const [takenButtons, setTakenButtons] = useState({});
  const [recognizedDrugs, setRecognizedDrugs] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [showProfileEdit, setShowProfileEdit] = useState(false);
  const [emergencyContacts, setEmergencyContacts] = useState([
    { id: 1, name: '张小明', phone: '13800138000', relationship: '儿子', isPrimary: true },
    { id: 2, name: '张小红', phone: '13900139000', relationship: '女儿', isPrimary: false }
  ]);
  const [showAddContact, setShowAddContact] = useState(false);
  const [showAddDrugModal, setShowAddDrugModal] = useState(false);
  const [showEditDrugModal, setShowEditDrugModal] = useState(false); // 编辑药品弹窗
  const [showConfirmDelete, setShowConfirmDelete] = useState(false); // 确认删除弹窗
  const [pendingDeleteDrug, setPendingDeleteDrug] = useState(null); // 待删除的药品
  const [showSuccessToast, setShowSuccessToast] = useState(false); // 成功提示弹窗
  const [toastMessage, setToastMessage] = useState(''); // 提示消息
  const [showDrugDetailModal, setShowDrugDetailModal] = useState(false); // 药品详情弹窗
  const [selectedDrug, setSelectedDrug] = useState(null); // 选中的药品
  const fileInputRef = useRef(null);

  const handleRegister = (registerData) => {
    if (registerData) {
      alert(`✅ 注册成功！用户ID: ${registerData.userId}`);
    }
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

  const handleLogin = (loginData) => {
    setUser({
      username: loginData.username,
      ...loginData
    });
    setIsLoggedIn(true);
    
    // 登录成功后加载药箱列表
    if (loginData.userId) {
      loadMedicineBoxList(loginData.userId);
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
    setToastMessage('个人信息已更新！');
    setShowSuccessToast(true);
    setTimeout(() => setShowSuccessToast(false), 2000);
  };

  const handleAddContact = (contact) => {
    setEmergencyContacts([...emergencyContacts, contact]);
    alert('✅ 紧急联系人已添加！');
  };

  const handleDeleteContact = (id) => {
    setEmergencyContacts(emergencyContacts.filter(c => c.id !== id));
    alert('✅ 联系人已删除！');
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
    setToastMessage('药品修改成功！');
    setShowSuccessToast(true);
    setTimeout(() => setShowSuccessToast(false), 2000);
    
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
        setToastMessage(data.message || '药品删除成功！');
        setShowSuccessToast(true);
        setTimeout(() => setShowSuccessToast(false), 2000);
        
        // 重新加载列表（从数据库获取最新数据）
        if (user && user.userId) {
          await loadMedicineBoxList(user.userId);
        }
      } else {
        alert(data.message || '删除失败，请重试');
      }
    } catch (err) {
      console.error('删除药品异常:', err);
      alert(' 网络连接失败，请稍后重试');
    }
  };

  // 取消删除
  const handleCancelDelete = () => {
    setShowConfirmDelete(false);
    setPendingDeleteDrug(null);
  };

  const handleAddDrug = async (drugData) => {
    // 构造新药数据
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
      remaining: drugData.totalQuantity, // 初始时剩余数量等于总数量
      note: drugData.note
    };
    
    setDrugList([...drugList, newDrug]);
    setShowAddDrugModal(false);
    
    // 显示自定义成功提示
    setToastMessage('药品添加成功！');
    setShowSuccessToast(true);
    setTimeout(() => setShowSuccessToast(false), 2000);
    
    // 添加成功后重新加载药箱列表（从数据库获取最新数据）
    if (user && user.userId) {
      await loadMedicineBoxList(user.userId);
    }
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setUser(null);
    setShowProfileModal(false);
    setActiveTab('home');
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
      alert('请选择图片文件！');
    }
    
    // 确保焦点回到窗口，关闭可能的文件对话框
    window.focus();
  };

  const handleDragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

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

  const audioRef = useRef(null);

  const speak = async (text, rate = speechRate) => {
    if (!text || text.trim() === '') {
      alert('没有可播放的内容');
      return;
    }

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
            audioRef.current.play();
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
      alert('您的浏览器不支持语音播报功能');
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
  }, [activeTab]);

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

  // 调用AI生成老年友好用药指导
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
        console.log('老年友好用药指导生成成功:', guideText);
      } else {
        console.error('生成老年友好指导失败:', data.message);
        // 生成默认指导
        setElderlyGuide(generateFallbackGuide(drugInfo));
      }
    } catch (error) {
      console.error('调用AI服务失败:', error);
      // 生成默认指导
      setElderlyGuide(generateFallbackGuide(drugInfo));
    } finally {
      setIsLoadingGuide(false);
    }
  };

  // 生成备用老年友好指导（当AI不可用时使用）
  const generateFallbackGuide = (drugInfo) => {
    const usage = drugInfo.usage || drugInfo.dosage || '';
    const precautions = drugInfo.precautions || '';
    const adverseReactions = drugInfo.adverseReactions || '';
    
    let guide = `您好，您查询的药品是${drugInfo.name}。\n\n`;
    
    // 用法用量
    if (usage) {
      guide += `【吃多少】：${usage}\n\n`;
    }
    
    // 服用时间
    if (usage.includes('饭前') || usage.includes('空腹')) {
      guide += `【什么时候吃】：建议在饭前半个小时吃，这样药效会更好。\n\n`;
    } else if (usage.includes('饭后')) {
      guide += `【什么时候吃】：建议在吃完饭半小时后吃，这样可以减少对胃的刺激。\n\n`;
    } else if (usage.includes('睡前')) {
      guide += `【什么时候吃】：建议在晚上睡觉前吃。\n\n`;
    } else {
      guide += `【什么时候吃】：按照医生说的时间吃就好。\n\n`;
    }
    
    // 注意事项
    if (precautions) {
      const warnings = [];
      if (precautions.includes('酒')) warnings.push('服药期间千万不要喝酒');
      if (precautions.includes('开车')) warnings.push('吃完药后最好不要开车');
      if (warnings.length > 0) {
        guide += `【特别提醒您】：${warnings.join('；')}。\n\n`;
      }
    }
    
    // 不良反应
    if (adverseReactions && adverseReactions !== '暂无详细信息') {
      guide += `【可能出现的不舒服】：有的人吃了这个药可能会有点${adverseReactions}，如果感觉很难受，一定要去找医生看看。\n\n`;
    }
    
    guide += `请您一定按照医生说的剂量和时间来吃药。\n祝您早日康复！`;
    
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

  const analyzeImage = async () => {
    console.log('=== 分析图片 ===');
    console.log('fileInputRef.current:', fileInputRef.current);
    console.log('fileInputRef.current?.files:', fileInputRef.current?.files);
    console.log('fileInputRef.current?.files[0]:', fileInputRef.current?.files[0]);
    
    if (!fileInputRef.current?.files[0]) {
      alert('请先选择图片');
      return;
    }

    setIsLoading(true);
    setOcrTaskId(null);

    try {
      // 将WebP图片转换为JPEG格式
      const file = await convertToJpeg(fileInputRef.current.files[0]);
      
      console.log('=== 准备上传 ===');
      console.log('文件名:', file.name);
      console.log('文件类型:', file.type);
      console.log('文件大小:', file.size);
      
      const formData = new FormData();
      formData.append('file', file);
      
      // 调试：检查FormData内容
      console.log('FormData内容:');
      for (let pair of formData.entries()) {
        console.log('  键:', pair[0], ', 值:', pair[1]);
      }

      // 不设置Content-Type，让浏览器自动处理
      // 直接调用后端API，绕过代理的multipart问题
      const response = await fetch('http://localhost:8080/api/v1/drug/recognize/upload', {
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
        alert(data.message || '上传失败');
        setIsLoading(false);
      }
    } catch (error) {
      console.error('=== 上传失败 ===', error);
      alert('上传失败，请检查网络连接');
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
              alert('未能识别出匹配的药品，请尝试手动输入');
            } else if (result.status === 'failed') {
              setIsLoading(false);
              alert('识别失败，请重试');
            }
          } else if (pollingCount < maxPollingCount) {
            pollingCount++;
            setTimeout(poll, 1000);
          } else {
            setOcrPolling(false);
            setIsLoading(false);
            alert('识别超时，请重试');
          }
        } else {
          setOcrPolling(false);
          setIsLoading(false);
          alert(data.message || '查询失败');
        }
      } catch (error) {
        console.error('查询失败:', error);
        setOcrPolling(false);
        setIsLoading(false);
        alert('查询失败，请检查网络连接');
      }
    };

    poll();
  };

  const addToMedicineBox = async (drug) => {
    if (!user || !user.userId) {
      alert('请先登录');
      return;
    }

    try {
      // 先查询药品基础库，获取药品ID
      const searchResponse = await fetch(`/api/v1/drug/list?keyword=${encodeURIComponent(drug.name)}`);
      const searchData = await searchResponse.json();
      
      let drugId = null;
      if (searchData.code === 200 && searchData.data && searchData.data.length > 0) {
        drugId = searchData.data[0].id;
      }

      // 如果找不到药品ID，使用默认值或提示用户
      if (!drugId) {
        alert('未找到匹配的药品，请手动添加');
        setActiveTab('drugs');
        return;
      }

      // 构造添加药品请求
      const today = new Date();
      const oneYearLater = new Date();
      oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);

      const addResponse = await fetch(`/api/v1/box?userId=${user.userId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          drugId: drugId,
          dosage: '每日两次，每次一片',
          frequency: '每日两次',
          startDate: today.toISOString().split('T')[0],
          expiryDate: oneYearLater.toISOString().split('T')[0],
          totalQuantity: 30
        })
      });

      const addData = await addResponse.json();

      if (addResponse.ok && addData.code === 200) {
        // 添加成功后重新加载药箱列表
        await loadMedicineBoxList(user.userId);
        setToastMessage('✅ 药品已加入药箱！');
        setShowSuccessToast(true);
        setTimeout(() => setShowSuccessToast(false), 2000);
      } else {
        alert(addData.message || '添加失败，请重试');
      }
    } catch (error) {
      console.error('添加药品失败:', error);
      alert('添加失败，请稍后重试');
    }
  };

  const markAsTaken = (id, event) => {
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

    setReminders(reminders.map(r =>
      r.id === id ? { ...r, taken: true, missed: false } : r
    ));
    setShowCelebration(true);
    setTimeout(() => setShowCelebration(false), 2500);
  };

  const undoMarkAsTaken = (id) => {
    const shouldUndo = window.confirm('确定要撤销吗？这将标记为未服药状态。');
    if (shouldUndo) {
      setReminders(reminders.map(r =>
        r.id === id ? { ...r, taken: false } : r
      ));
      setTakenButtons(prev => {
        const newState = { ...prev };
        delete newState[id];
        return newState;
      });
    }
  };

  const takenCount = reminders.filter(r => r.taken).length;
  const totalCount = reminders.length;
  const progressPercent = (takenCount / totalCount) * 440;

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
            <div className="progress-ring-container">
              <svg className="progress-ring" viewBox="0 0 180 180">
                <defs>
                  <linearGradient id="progressGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#4A90E2" />
                    <stop offset="100%" stopColor="#98D4BB" />
                  </linearGradient>
                </defs>
                <circle className="progress-ring-circle-bg" cx="90" cy="90" r="80" />
                <circle
                  className="progress-ring-circle"
                  cx="90" cy="90" r="80"
                  strokeDasharray={`${progressPercent} 440`}
                />
              </svg>
              <div className="progress-ring-text">
                <div className="progress-ring-value">{takenCount}/{totalCount}</div>
                <div className="progress-ring-label">已完成</div>
              </div>
            </div>
          </div>
        </div>

        <div className="dashboard-card" onClick={() => setActiveTab('drugs')}>
          <h3 className="dashboard-card-title">我的药箱</h3>
          <div className="dashboard-card-desc">
            <div className="medicine-box-stats">
              <div className="medicine-box-count">{drugList.length}</div>
              <div className="medicine-box-unit">种药品</div>
            </div>
            <div className="notification-bar" style={{ marginTop: '20px', padding: '14px 20px' }}>
              <span className="notification-icon">⚠️</span>
              <span className="notification-text">1盒药还有7天过期</span>
            </div>
          </div>
        </div>
      </div>

      <div className="notification-bar">
        <span className="notification-icon">🔔</span>
        <span className="notification-text">您有1盒药还有7天过期</span>
      </div>
    </div>
  );

  const renderUploadTab = () => (
    <div className="card">
      <h2 className="card-title">
        <span className="card-title-icon">📷</span>
        上传药品照片
      </h2>

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
      </div>

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
      <h2 className="card-title">
        <span className="card-title-icon">✅</span>
        识别结果
      </h2>

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

    return (
      <div className="card">
        <h2 className="card-title">
          <span className="card-title-icon">⚠️</span>
          用药安全检查
        </h2>

        {hasConflict ? (
          <div className="conflict-section">
            <div className="conflict-item conflict-level-severe">
              <span className="conflict-badge severe">🔴 严重冲突</span>
              <div className="drug-connection">
                <div className="drug-node">阿司匹林</div>
                <span className="drug-connector">⚡</span>
                <div className="drug-node">布洛芬</div>
              </div>
              <div className="conflict-explanation">
                <p className="conflict-explanation-text">
                  这两个药一起吃会对胃造成严重损伤，可能引起胃出血，<span style={{ color: 'var(--danger-red)', fontWeight: 'bold' }}>不能同时服用</span>！
                </p>
              </div>
            </div>

            <div className="conflict-item conflict-level-moderate">
              <span className="conflict-badge moderate">🟡 中等冲突</span>
              <div className="drug-connection">
                <div className="drug-node">硝苯地平</div>
                <span className="drug-connector">⚡</span>
                <div className="drug-node">西柚汁</div>
              </div>
              <div className="conflict-explanation">
                <p className="conflict-explanation-text">
                  西柚汁会影响药物代谢，建议<span style={{ color: 'var(--warning-orange)', fontWeight: 'bold' }}>间隔4小时以上</span>服用。
                </p>
              </div>
            </div>

            <div className="conflict-item conflict-level-mild">
              <span className="conflict-badge mild">🔵 轻微注意</span>
              <div className="drug-connection">
                <div className="drug-node">二甲双胍</div>
                <span className="drug-connector">💡</span>
                <div className="drug-node">酒</div>
              </div>
              <div className="conflict-explanation">
                <p className="conflict-explanation-text">
                  服用此药期间尽量不要饮酒，以免影响血糖控制。
                </p>
              </div>
            </div>

            <button className="btn btn-primary btn-large" style={{ marginTop: '32px', width: '100%' }}>
              📄 生成冲突报告卡片
            </button>
          </div>
        ) : (
          <div className="safe-display">
            <span className="shield-icon">🛡️</span>
            <h3 className="safe-title">未发现冲突</h3>
            <p className="safe-subtitle">您的用药方案是安全的，继续保持！</p>
          </div>
        )}
      </div>
    );
  };

  const renderCalendarTab = () => (
    <div className="card">
      <h2 className="card-title">
        <span className="card-title-icon">📅</span>
        今日用药时间轴
      </h2>

      <div className="timeline-container">
        <div className="timeline-line"></div>
        {reminders.map((reminder, index) => (
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
            <p className="timeline-drug">💊 {reminder.drug}</p>
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
                  >
                    <span className="btn-text">✓ 我吃了</span>
                  </button>
                </>
              )}
            </div>
          </div>
        ))}
      </div>

      {showCelebration && (
        <div className="celebration-overlay">
          <div className="celebration-card">
            <span className="celebration-icon">🎉</span>
            <p className="celebration-text">真棒！</p>
          </div>
        </div>
      )}
    </div>
  );

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
    // 如果有搜索关键词且正在搜索，显示搜索中的药品列表
    // 如果有搜索关键词且已完成搜索，显示过滤后的药品列表
    // 否则显示完整列表
    const displayList = filteredDrugList.length > 0 && searchQuery.trim() ? filteredDrugList : drugList;
    
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
            onChange={(e) => handleSearchDrugs(e.target.value)}
          />
        </div>
        <button className="btn btn-primary btn-large" onClick={() => setShowAddDrugModal(true)}>
          ➕ 添加新药
        </button>
      </div>

      <div className="drug-grid">
        {displayList.length === 0 && searchQuery.trim() && !isSearching ? (
          <div style={{ textAlign: 'center', padding: '48px', color: 'var(--text-light)' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔍</div>
            <p style={{ fontSize: '22px' }}>未找到与"{searchQuery}"相关的药品</p>
            <p style={{ fontSize: '18px', marginTop: '12px' }}>请尝试其他关键词</p>
          </div>
        ) : (
          displayList.map((drug, index) => {
          const isExpiring = new Date(drug.expiryDate) - new Date() < 30 * 24 * 60 * 60 * 1000;
          // 使用真实总数量计算进度百分比
          const totalQty = drug.totalQuantity || 30; // 如果没有总数量，默认30
          const remainingQty = drug.remaining || totalQty;
          const remainingPercent = Math.max(0, Math.min(100, (remainingQty / totalQty) * 100));

          return (
            <div 
              key={index} 
              className={`drug-bottle-card ${isExpiring ? 'expiring' : ''}`}
              onClick={() => handleOpenDrugDetail(drug)}
              style={{ cursor: 'pointer' }}
            >
              {isExpiring && <span className="expiring-tag">即将过期</span>}
              <div className="bottle-icon">💊</div>
              <h4 className="bottle-name">{drug.name}</h4>
              <p className="bottle-info">规格：{drug.spec}</p>
              <p className="bottle-info">用法：{drug.dosage}</p>
              <p className="bottle-info">剩余：{remainingQty}/{totalQty}片</p>
              <div className="bottle-progress">
                <div className="bottle-progress-fill" style={{ width: `${remainingPercent}%` }}></div>
              </div>
              <p className="bottle-info" style={{ color: isExpiring ? 'var(--warning-orange)' : 'var(--text-light)' }}>
                效期：{drug.expiryDate}
              </p>
            </div>
          );
        }))}
      </div>

      <div className="stats-bar">
        <div className="stats-item">
          <div className="stats-value">{displayList.length}</div>
          <div className="stats-label">种药品</div>
        </div>
        <div className="stats-item">
          <div className="stats-value warning">
            {displayList.filter(d => new Date(d.expiryDate) - new Date() < 30 * 24 * 60 * 60 * 1000).length}
          </div>
          <div className="stats-label">需关注</div>
        </div>
        <div className="stats-item">
          <div className="stats-value success">{takenCount}</div>
          <div className="stats-label">今日已服</div>
        </div>
      </div>
    </div>
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
        />
      )}

      {showAddDrugModal && (
        <AddDrugModal
          onClose={() => setShowAddDrugModal(false)}
          onAdd={handleAddDrug}
          userId={user?.userId}
        />
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

      {/* 成功提示弹窗 */}
      {showSuccessToast && (
        <div className="success-toast">
          <div className="success-toast-content">
            <div className="success-toast-icon">✓</div>
            <p className="success-toast-message">{toastMessage}</p>
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
                  <p className="detail-value">{selectedDrug.name}</p>
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

            <div className="modal-footer">
              <button className="btn btn-secondary btn-large" onClick={() => handleEditDrug(selectedDrug)}>
                ✏️ 修改
              </button>
              <button className="btn btn-danger btn-large" onClick={() => handleDeleteDrug(selectedDrug)}>
                 删除
              </button>
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
    </>
  );
}

export default App;
