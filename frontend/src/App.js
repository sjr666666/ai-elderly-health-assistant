import React, { useState, useRef } from 'react';
import './App.css';
import Login from './components/Login';
import Register from './components/Register';
import ProfileModal from './components/ProfileModal';
import ProfileEdit from './components/ProfileEdit';
import EmergencyContacts from './components/EmergencyContacts';

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showRegister, setShowRegister] = useState(true);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [user, setUser] = useState(null);
  const [activeTab, setActiveTab] = useState('home');
  const [drugList, setDrugList] = useState([
    { id: 1, name: '硝苯地平缓释片', spec: '5mg', manufacturer: '德州德药制药厂', expiryDate: '2026-12-31', dosage: '每日两次，每次半片', remaining: 25 },
    { id: 2, name: '二甲双胍片', spec: '0.5g', manufacturer: '上海现代制药厂', expiryDate: '2026-06-30', dosage: '每日三次，每次一片', remaining: 60 }
  ]);
  const [imagePreview, setImagePreview] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isListening, setIsListening] = useState(false);
  const [recognizedText, setRecognizedText] = useState('');
  const [manualDrugName, setManualDrugName] = useState('');
  const [manualSpec, setManualSpec] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [showVoicePanel, setShowVoicePanel] = useState(false);
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
  const [voiceLoading, setVoiceLoading] = useState(false);
  const [showProfileEdit, setShowProfileEdit] = useState(false);
  const [emergencyContacts, setEmergencyContacts] = useState([
    { id: 1, name: '张小明', phone: '13800138000', relationship: '儿子', isPrimary: true },
    { id: 2, name: '张小红', phone: '13900139000', relationship: '女儿', isPrimary: false }
  ]);
  const [showAddContact, setShowAddContact] = useState(false);
  const fileInputRef = useRef(null);

  const handleRegister = (registerData) => {
    if (registerData) {
      alert(`✅ 注册成功！用户ID: ${registerData.userId}`);
    }
    setShowRegister(false);
  };

  const handleLogin = (loginData) => {
    setUser({
      username: loginData.username,
      ...loginData
    });
    setIsLoggedIn(true);
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
    alert('✅ 个人信息已更新！');
  };

  const handleAddContact = (contact) => {
    setEmergencyContacts([...emergencyContacts, contact]);
    alert('✅ 紧急联系人已添加！');
  };

  const handleDeleteContact = (id) => {
    setEmergencyContacts(emergencyContacts.filter(c => c.id !== id));
    alert('✅ 联系人已删除！');
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
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  };

  const startVoiceRecognition = () => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
      const recognition = new SpeechRecognition();
      recognition.lang = 'zh-CN';
      recognition.continuous = false;
      recognition.interimResults = false;

      recognition.onstart = () => {
        setIsListening(true);
        setShowVoicePanel(true);
        setVoiceLoading(true);
      };

      recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setRecognizedText(transcript);
        setIsListening(false);
        setVoiceLoading(false);
      };

      recognition.onerror = (event) => {
        console.error('语音识别错误:', event.error);
        setIsListening(false);
        setVoiceLoading(false);
        alert('语音识别失败，请重试');
      };

      recognition.onend = () => {
        setIsListening(false);
        setVoiceLoading(false);
      };

      recognition.start();
    } else {
      alert('您的浏览器不支持语音识别功能');
    }
  };

  const speak = (text, rate = speechRate) => {
    if ('speechSynthesis' in window) {
      setIsSpeaking(true);
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = 'zh-CN';
      utterance.rate = rate;
      utterance.volume = 1;
      utterance.onend = () => setIsSpeaking(false);
      utterance.onerror = () => setIsSpeaking(false);
      window.speechSynthesis.speak(utterance);
    } else {
      alert('您的浏览器不支持语音播报功能');
    }
  };

  const stopSpeaking = () => {
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      setIsSpeaking(false);
    }
  };

  const analyzeImage = () => {
    setIsLoading(true);
    setTimeout(() => {
      setRecognizedDrugs([
        { id: 1, name: '硝苯地平缓释片(Ⅰ)', spec: '5mg×30片', manufacturer: '德州德药制药厂', matchScore: 96 },
        { id: 2, name: '硝苯地平缓释片(II)', spec: '10mg×30片', manufacturer: '德州德药制药厂', matchScore: 87 },
        { id: 3, name: '硝苯地平控释片', spec: '20mg×7片', manufacturer: '上海现代制药厂', matchScore: 73 }
      ]);
      setIsLoading(false);
      setActiveTab('recognition');
    }, 2500);
  };

  const addToMedicineBox = (drug) => {
    const newDrug = {
      id: Date.now(),
      name: drug.name,
      spec: drug.spec,
      manufacturer: drug.manufacturer,
      expiryDate: '2026-12-31',
      dosage: '每日两次，每次一片',
      remaining: 30
    };
    setDrugList([...drugList, newDrug]);
    alert('✅ 药品已加入药箱！');
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
        className={`upload-area ${isDragging ? 'dragging' : ''}`}
        onClick={() => fileInputRef.current?.click()}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <input
          ref={fileInputRef}
          id="file-input"
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={handleFileUpload}
        />
        <span className="upload-icon">💊</span>
        <p className="upload-text">拖拽药盒图片到此处，或点击上传</p>
        <p className="upload-hint">支持 JPG、PNG 格式，文件小于10MB</p>
      </div>

      {imagePreview && (
        <img src={imagePreview} alt="药品预览" className="preview-image" />
      )}

      {isLoading && (
        <div className="loading-container">
          <div className="loading-dna">
            <div className="dna-dot"></div>
            <div className="dna-dot"></div>
            <div className="dna-dot"></div>
            <div className="dna-dot"></div>
            <div className="dna-dot"></div>
          </div>
          <p className="loading-text">🔍 AI正在识别药品，请稍候...</p>
        </div>
      )}

      <div className="btn-group">
        <button
          className="btn btn-primary btn-large"
          onClick={analyzeImage}
          disabled={!imagePreview || isLoading}
        >
          🔍 开始识别
        </button>
        <button
          className="btn btn-secondary btn-large"
          onClick={startVoiceRecognition}
          disabled={isLoading}
        >
          🎤 说不出名字？点这里说话
        </button>
      </div>

      <div style={{ marginTop: '48px' }}>
        <h3 style={{ fontSize: '26px', fontWeight: 'bold', color: 'var(--tech-blue)', marginBottom: '24px' }}>
          ✍️ 手动输入药品
        </h3>
        <div className="input-section">
          <label className="input-label">药品名称</label>
          <input
            type="text"
            className="text-input"
            placeholder="请输入药品名称"
            value={manualDrugName}
            onChange={(e) => setManualDrugName(e.target.value)}
          />
        </div>
        <div className="input-section">
          <label className="input-label">规格</label>
          <input
            type="text"
            className="text-input"
            placeholder="例如：5mg×30片"
            value={manualSpec}
            onChange={(e) => setManualSpec(e.target.value)}
          />
        </div>
        <button className="btn btn-success btn-large">
          ➕ 添加到药箱
        </button>
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
                className="btn btn-success"
                style={{ marginTop: '20px', width: '100%', minHeight: '56px' }}
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

  const renderExplanationTab = () => (
    <div className="card explanation-card">
      <div className="herb-pattern"></div>
      <h2 className="card-title">
        <span className="card-title-icon">📖</span>
        用药说明
      </h2>

      <div className="explanation-layout">
        <div className="drug-info-panel">
          <div className="drug-info-card">
            <div className="drug-info-icon">💊</div>
            <h3 className="drug-info-name">硝苯地平缓释片</h3>
            <p className="drug-info-spec">规格：5mg × 30片</p>
            <p className="drug-info-mfr">德州德药制药厂</p>
            <div className="drug-info-divider"></div>
            <p className="drug-info-dosage">每日两次，每次一片</p>
            <p className="drug-info-note">建议饭前半小时服用</p>
          </div>
        </div>

        <div className="chat-section">
          <div className="chat-header">
            <div className="speaker-avatar">👨‍⚕️</div>
            <div className="speaker-info">
              <p className="speaker-name">虚拟药剂师</p>
              <p className="speaker-title">您的用药小助手</p>
            </div>
          </div>

          <div className="chat-bubble-wrapper">
            <div className="chat-bubble-avatar">👨‍⚕️</div>
            <div className="chat-bubble-content">
              <p className="chat-bubble-text">
                👋 阿姨，这药每天<span className="highlight">早晚各吃一片</span>，<br/>
                <span className="highlight">饭后半小时</span>吃最好哦。<br/><br/>
                每片5毫克，您需要吃<span className="highlight">半片</span>。<br/>
                记得<span className="highlight">不要喝酒</span>，会影响药效！
              </p>
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
              onClick={() => isSpeaking ? stopSpeaking() : speak('阿姨，这药每天早晚各吃一片，饭后半小时吃最好哦。每片5毫克，您需要吃半片。记得不要喝酒，会影响药效！')}
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

      <div className="warning-box">
        <h4 className="warning-title">
          ⚠️ 重要提醒
        </h4>
        <p className="warning-text">以上为AI生成，用药请遵医嘱</p>
      </div>
    </div>
  );

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

  const renderDrugsTab = () => (
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
            placeholder="搜索药品..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <button className="btn btn-primary btn-large" onClick={() => setActiveTab('upload')}>
          ➕ 添加新药
        </button>
      </div>

      <div className="drug-grid">
        {drugList.map((drug, index) => {
          const isExpiring = new Date(drug.expiryDate) - new Date() < 30 * 24 * 60 * 60 * 1000;
          const remainingPercent = Math.max(0, Math.min(100, (drug.remaining / 30) * 100));

          return (
            <div key={index} className={`drug-bottle-card ${isExpiring ? 'expiring' : ''}`}>
              {isExpiring && <span className="expiring-tag">即将过期</span>}
              <div className="bottle-icon">💊</div>
              <h4 className="bottle-name">{drug.name}</h4>
              <p className="bottle-info">规格：{drug.spec}</p>
              <p className="bottle-info">用法：{drug.dosage}</p>
              <p className="bottle-info">剩余：{drug.remaining}片</p>
              <div className="bottle-progress">
                <div className="bottle-progress-fill" style={{ width: `${remainingPercent}%` }}></div>
              </div>
              <p className="bottle-info" style={{ color: isExpiring ? 'var(--warning-orange)' : 'var(--text-light)' }}>
                效期：{drug.expiryDate}
              </p>
            </div>
          );
        })}
      </div>

      <div className="stats-bar">
        <div className="stats-item">
          <div className="stats-value">{drugList.length}</div>
          <div className="stats-label">种药品</div>
        </div>
        <div className="stats-item">
          <div className="stats-value warning">
            {drugList.filter(d => new Date(d.expiryDate) - new Date() < 30 * 24 * 60 * 60 * 1000).length}
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

  const [emergencyMessages, setEmergencyMessages] = useState([
    { id: 1, type: 'ai', text: '您好，我是您的AI健康助手。请描述一下您目前的不适症状，我会尽力为您提供一些建议。' }
  ]);
  const [emergencyInput, setEmergencyInput] = useState('');

  const handleEmergencySubmit = () => {
    if (!emergencyInput.trim()) return;
    const userMsg = { id: Date.now(), type: 'user', text: emergencyInput };
    setEmergencyMessages(prev => [...prev, userMsg]);
    setEmergencyInput('');

    setTimeout(() => {
      const aiResponse = { id: Date.now() + 1, type: 'ai', text: '我理解您的不适。请不要担心，先深呼吸放松一下。如果症状持续或加重，建议您尽快联系家人或医生。🏥' };
      setEmergencyMessages(prev => [...prev, aiResponse]);
    }, 1500);
  };

  const renderEmergencyTab = () => (
    <div className="emergency-container">
      <h2 className="emergency-title">🚨 您哪里不舒服？</h2>

      <div className="emergency-conversation">
        {emergencyMessages.map((msg) => (
          <div key={msg.id} className={`emergency-bubble ${msg.type}`}>
            <span className="bubble-avatar">{msg.type === 'ai' ? '🤖' : '👤'}</span>
            <p className="bubble-text">{msg.text}</p>
          </div>
        ))}
      </div>

      <div className="emergency-input-area">
        <textarea
          className="emergency-textarea"
          placeholder="请描述您的症状..."
          rows="3"
          value={emergencyInput}
          onChange={(e) => setEmergencyInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), handleEmergencySubmit())}
        />

        <div className="emergency-buttons">
          <button className="btn btn-primary btn-large" onClick={handleEmergencySubmit}>
            🤖 咨询AI助手
          </button>
          <button className="btn btn-secondary btn-large" onClick={startVoiceRecognition}>
            🎤 语音输入
          </button>
        </div>
      </div>

      <button
        className="emergency-call-btn"
        style={{ marginTop: '48px', marginLeft: 'auto', marginRight: 'auto' }}
        onClick={() => window.location.href = 'tel:13800138000'}
      >
        <span className="emergency-call-icon">📞</span>
        一键呼叫家人
        <span className="emergency-call-label">主要联系人</span>
        <span className="emergency-contact">张小明 - 138-0013-8000</span>
      </button>
    </div>
  );

  const renderVoiceAssistant = () => (
    <div
      className={`voice-assistant ${isListening ? 'listening' : ''}`}
      onClick={startVoiceRecognition}
    >
      <div className="voice-wave"></div>
      <div className="voice-wave"></div>
      <div className="voice-wave"></div>
      <div className="voice-assistant-icon">🎤</div>
    </div>
  );

  const renderVoicePanel = () => {
    if (!showVoicePanel) return null;

    return (
      <div className="voice-panel">
        <div className="voice-panel-header">
          <h4 className="voice-panel-title">🎤 语音助手</h4>
          <button className="voice-panel-close" onClick={() => {
            setShowVoicePanel(false);
            setRecognizedText('');
          }}>✕</button>
        </div>

        <p className="voice-panel-subtitle">点击麦克风开始说话</p>

        {voiceLoading || isListening ? (
          <>
            <div className={`voice-panel-mic ${isListening ? 'listening' : ''}`}>
              <span className="voice-panel-mic-icon">🎤</span>
            </div>
            <p className="voice-panel-status">
              {isListening ? '🔊 正在听您说话...' : '⏳ 正在启动...'}
            </p>
          </>
        ) : recognizedText ? (
          <>
            <div className="voice-panel-result">
              <p className="voice-panel-result-label">您说的是：</p>
              <p className="voice-panel-result-text">{recognizedText}</p>
            </div>
            <div className="voice-panel-actions">
              <button className="btn btn-secondary" onClick={() => {
                setShowVoicePanel(false);
                setRecognizedText('');
              }}>
                取消
              </button>
              <button className="btn btn-primary" onClick={() => {
                setShowVoicePanel(false);
                setRecognizedText('');
                setActiveTab('upload');
              }}>
                确认
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="voice-panel-mic">
              <span className="voice-panel-mic-icon">🎤</span>
            </div>
            <button className="btn btn-primary" style={{ width: '100%' }} onClick={startVoiceRecognition}>
              开始说话
            </button>
          </>
        )}
      </div>
    );
  };

  return (
    <>
      <div className="watermark-bg"></div>
      {showRegister ? (
        <Register onRegister={handleRegister} />
      ) : !isLoggedIn ? (
        <Login onLogin={handleLogin} />
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
            {activeTab === 'emergency' && renderEmergencyTab()}
          </div>

          {renderVoiceAssistant()}
          {renderVoicePanel()}
        </div>
      )}

      {showProfileModal && (
        <ProfileModal
          onComplete={handleProfileComplete}
          onClose={() => setShowProfileModal(false)}
        />
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
    </>
  );
}

export default App;
