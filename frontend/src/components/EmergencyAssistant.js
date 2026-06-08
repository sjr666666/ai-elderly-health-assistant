import { useState, useEffect, useRef } from 'react';
import ContactModal from './ContactModal';

const EmergencyAssistant = ({ emergencyContacts }) => {
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [emergencyMode, setEmergencyMode] = useState(false);
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [showContactModal, setShowContactModal] = useState(false);
  
  const messagesEndRef = useRef(null);

  // 紧急情况分类标签
  const categories = [
    { id: 'heart', name: '心脏问题', icon: '❤️', keywords: ['心脏病', '胸闷', '心悸', '心跳', '胸痛'] },
    { id: 'stroke', name: '中风', icon: '🧠', keywords: ['中风', '偏瘫', '言语不清', '肢体麻木'] },
    { id: 'breathing', name: '呼吸困难', icon: '🫁', keywords: ['呼吸困难', '窒息', '气喘', '咳嗽'] },
    { id: 'accident', name: '意外伤害', icon: '🦴', keywords: ['摔倒', '骨折', '出血', '外伤'] },
    { id: 'poison', name: '中毒', icon: '☠️', keywords: ['中毒', '误食', '药物过量'] },
    { id: 'fire', name: '火灾', icon: '🔥', keywords: ['火灾', '着火', '浓烟'] },
    { id: 'medical', name: '急救常识', icon: '🏥', keywords: ['急救', '止血', '包扎', 'CPR'] },
    { id: 'other', name: '其他紧急情况', icon: '⚠️', keywords: ['紧急', '帮助', '求救'] },
  ];

  // 离线模式下的基础应答
  const offlineResponses = {
    heart: '❤️ **心脏问题紧急处理建议：**\n\n1. 立即拨打120急救电话\n2. 让患者平躺，保持安静\n3. 松开紧身衣物\n4. 如果患者失去意识，立即进行心肺复苏\n5. 不要给意识不清的患者喂水或食物',
    stroke: '🧠 **中风紧急处理建议：**\n\n1. 立即拨打120急救电话\n2. 让患者侧卧，保持呼吸通畅\n3. 不要移动患者，除非处于危险环境\n4. 记录发病时间，这对治疗非常重要\n5. 不要给患者喂水或食物',
    breathing: '🫁 **呼吸困难紧急处理建议：**\n\n1. 立即拨打120急救电话\n2. 让患者保持舒适的姿势\n3. 保持周围空气流通\n4. 安抚患者情绪，保持冷静\n5. 如果患者停止呼吸，立即进行人工呼吸',
    accident: '🦴 **意外伤害紧急处理建议：**\n\n1. 确保现场安全\n2. 检查患者意识和呼吸\n3. 如果有出血，立即止血\n4. 不要随意移动骨折部位\n5. 拨打120急救电话',
    poison: '☠️ **中毒紧急处理建议：**\n\n1. 立即拨打120和12320中毒控制中心\n2. 保留呕吐物或可疑物品供医生分析\n3. 不要自行催吐（腐蚀性物质除外）\n4. 不要给意识不清的患者喂水',
    fire: '🔥 **火灾紧急处理建议：**\n\n1. 立即拨打119火警电话\n2. 用湿毛巾捂住口鼻\n3. 低姿逃生，不要乘坐电梯\n4. 如果无法逃生，关紧房门，在窗口呼救\n5. 不要贪恋财物',
    medical: '🏥 **急救常识：**\n\n**止血方法：**\n- 直接压迫止血法\n- 抬高伤肢止血法\n- 止血带止血法（仅限四肢）\n\n**CPR心肺复苏：**\n- 胸外按压：每分钟100-120次\n- 人工呼吸：每30次按压后2次呼吸\n- 直到专业人员到达',
    other: '⚠️ **紧急情况处理建议：**\n\n1. 保持冷静，评估情况\n2. 确保自身安全\n3. 立即拨打相应的紧急电话（110/120/119）\n4. 提供准确的位置信息\n5. 等待专业人员到达',
  };

  // 检查是否为紧急问题
  const isEmergencyQuestion = (question) => {
    const emergencyKeywords = ['紧急', '救命', '快', '难受', '疼', '痛', '呼吸困难', '心跳', '晕倒', '出血', '着火', '中毒'];
    return emergencyKeywords.some(keyword => question.includes(keyword));
  };

  // 获取分类标签
  const getCategoryByQuestion = (question) => {
    for (const category of categories) {
      if (category.keywords.some(keyword => question.includes(keyword))) {
        return category;
      }
    }
    return categories[categories.length - 1];
  };

  // 滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 监听网络状态
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // 锁定/解锁页面滚动
  useEffect(() => {
    if (showContactModal) {
      // 保存原始滚动位置
      const scrollY = window.scrollY;
      // 保存原始样式
      const originalStyle = window.getComputedStyle(document.body);
      const originalOverflow = originalStyle.overflow;
      
      // 锁定滚动
      document.body.style.overflow = 'hidden';
      document.body.style.position = 'fixed';
      document.body.style.top = `-${scrollY}px`;
      document.body.style.left = '0';
      document.body.style.right = '0';
      
      return () => {
        // 恢复滚动
        document.body.style.overflow = originalOverflow;
        document.body.style.position = '';
        document.body.style.top = '';
        document.body.style.left = '';
        document.body.style.right = '';
        window.scrollTo(0, scrollY);
      };
    }
  }, [showContactModal]);

  // 发送消息
  const sendMessage = async () => {
    const question = inputValue.trim();
    if (!question || isLoading) return;

    setError('');
    setIsLoading(true);
    
    const category = getCategoryByQuestion(question);
    const isEmergency = isEmergencyQuestion(question) || emergencyMode;
    
    // 添加用户消息
    const userMessage = {
      id: Date.now(),
      type: 'user',
      text: question,
      category: category.id,
      isEmergency,
      timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages(prev => [...prev, userMessage]);
    setInputValue('');

    try {
      let responseText = '';
      let isOfflineResponse = false;

      if (!isOnline) {
        // 离线模式
        responseText = offlineResponses[category.id] || offlineResponses.other;
        isOfflineResponse = true;
      } else {
        // 在线模式，调用AI API
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000); // 10秒超时

        try {
          const response = await fetch('/api/emergency/ask', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
              question,
              isEmergency: isEmergency || emergencyMode,
              category: category.id,
              // 传递对话历史以实现记忆功能
              history: messages.map(msg => ({
                role: msg.type === 'user' ? 'user' : 'assistant',
                content: msg.text
              })),
            }),
            signal: controller.signal,
          });

          clearTimeout(timeoutId);

          if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
          }

          const data = await response.json();
          
          if (data.code === 200) {
            responseText = data.data || '已收到您的请求，正在处理中...';
          } else {
            // API返回错误，使用离线响应作为备用
            console.warn('API返回错误:', data.message);
            responseText = offlineResponses[category.id] || offlineResponses.other;
            isOfflineResponse = true;
          }
        } catch (fetchError) {
          clearTimeout(timeoutId);
          if (fetchError.name === 'AbortError') {
            console.warn('请求超时');
            responseText = offlineResponses[category.id] || offlineResponses.other;
            isOfflineResponse = true;
          } else {
            throw fetchError;
          }
        }
      }

      // 添加AI回复
      const aiMessage = {
        id: Date.now() + 1,
        type: 'assistant',
        text: responseText,
        category: category.id,
        isEmergency,
        isOfflineResponse,
        timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, aiMessage]);

    } catch (err) {
      // 网络错误或超时，使用离线响应
      const category = getCategoryByQuestion(question);
      const offlineMessage = {
        id: Date.now() + 1,
        type: 'assistant',
        text: offlineResponses[category.id] || offlineResponses.other,
        category: category.id,
        isEmergency,
        isOfflineResponse: true,
        timestamp: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages(prev => [...prev, offlineMessage]);
      setError('网络连接不稳定，已切换到离线模式');
    } finally {
      setIsLoading(false);
    }
  };

  // 处理分类标签点击
  const handleCategoryClick = (category) => {
    setInputValue(prev => prev + `【${category.name}】`);
  };

  // 清空对话
  const clearMessages = () => {
    setMessages([]);
    setError('');
  };

  // 处理一键呼叫家人 - 展示联系人选择面板
  const handleCallFamily = () => {
    if (!emergencyContacts || emergencyContacts.length === 0) {
      setError('请先添加紧急联系人');
      return;
    }
    setShowContactModal(true);
  };

  // 格式化消息文本（移动端优化版本）
  const formatMessageText = (text) => {
    // 处理AI回复，使用更好的格式
    if (text.includes('【') || text.includes('1.') || text.includes('2.') || text.includes('\n')) {
      // 分割段落
      const paragraphs = text.split(/\n\n+/).filter(p => p.trim());
      
      return paragraphs.map((paragraph, pIndex) => {
        // 检查是否是紧急提醒
        const isEmergencySection = paragraph.includes('【') || paragraph.includes('紧急') || paragraph.includes('⚠️');
        
        // 处理列表项
        if (paragraph.match(/^\d+[.、]\s/m) || paragraph.match(/^[•\-*]\s/m)) {
          const items = paragraph.split(/\n/).filter(line => line.trim());
          const listItems = items.map((item, iIndex) => {
            // 移除开头的序号
            const cleanItem = item.replace(/^\d+[.、]\s*/, '').replace(/^[•\-*]\s*/, '');
            return (
              <li key={iIndex} style={{ 
                marginBottom: '8px', 
                fontSize: '14px',
                lineHeight: '1.6',
                color: 'inherit'
              }}>
                {cleanItem}
              </li>
            );
          });
          
          return (
            <ul key={pIndex} style={{ 
              margin: '10px 0', 
              paddingLeft: '20px',
              backgroundColor: isEmergencySection ? 'rgba(255, 243, 224, 0.5)' : 'transparent',
              padding: isEmergencySection ? '12px 12px 12px 28px' : '0 0 0 20px',
              borderRadius: isEmergencySection ? '8px' : '0'
            }}>
              {listItems}
            </ul>
          );
        }
        
        // 普通段落
        return (
          <p key={pIndex} style={{ 
            margin: '0 0 10px 0', 
            fontSize: '14px',
            lineHeight: '1.6',
            color: 'inherit',
            backgroundColor: isEmergencySection ? 'rgba(255, 243, 224, 0.5)' : 'transparent',
            padding: isEmergencySection ? '10px 12px' : '0',
            borderRadius: isEmergencySection ? '8px' : '0',
            fontWeight: isEmergencySection ? '600' : '400'
          }}>
            {paragraph}
          </p>
        );
      });
    }
    
    // 默认简单换行格式
    return text.split('\n').map((line, index) => (
      <span key={index}>
        {line}
        <br />
      </span>
    ));
  };

  return (
    <div className="emergency-container">
      {/* 头部 */}
      <div className="emergency-header">
        <div className="header-title">
          <span className="emergency-icon">🚨</span>
          <h2>AI紧急助手</h2>
        </div>
        {messages.length > 0 && (
          <button className="clear-btn" onClick={clearMessages} title="清空对话">
            🗑️
          </button>
        )}
      </div>

      {/* 一键呼叫家人按钮 */}
      <div className="emergency-call-section">
        <button className="emergency-call-btn" onClick={handleCallFamily}>
          <span className="call-icon">📞</span>
          <span className="call-text">一键呼叫家人</span>
        </button>
      </div>

      {/* 紧急模式开关 - 大号高对比度设计 */}
      <div className="emergency-toggle" style={{
        marginBottom: '20px',
        padding: '16px 20px',
        background: 'linear-gradient(135deg, #FFF5F5 0%, #FFF0F0 100%)',
        borderRadius: '16px',
        border: '2px solid #FFCDD2',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span style={{ fontSize: '28px' }}>🚨</span>
          <div>
            <div style={{
              fontSize: '18px',
              fontWeight: '700',
              color: '#C62828',
              letterSpacing: '0.5px'
            }}>紧急模式（救命用）</div>
            <div style={{
              fontSize: '12px',
              color: '#E57373',
              marginTop: '2px'
            }}>开启后优先处理紧急求助</div>
          </div>
        </div>
        <label style={{ cursor: 'pointer', position: 'relative' }}>
          <input
            type="checkbox"
            checked={emergencyMode}
            onChange={(e) => setEmergencyMode(e.target.checked)}
            style={{
              width: '64px',
              height: '36px',
              appearance: 'none',
              background: emergencyMode ? 'linear-gradient(135deg, #E53935, #C62828)' : '#E0E0E0',
              borderRadius: '18px',
              position: 'relative',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              boxShadow: emergencyMode ? '0 4px 15px rgba(229, 57, 53, 0.4)' : '0 2px 8px rgba(0,0,0,0.1)'
            }}
          />
          <span style={{
            position: 'absolute',
            width: '28px',
            height: '28px',
            background: 'white',
            borderRadius: '50%',
            top: '4px',
            left: emergencyMode ? '32px' : '4px',
            transition: 'all 0.3s ease',
            boxShadow: '0 2px 6px rgba(0,0,0,0.25)',
            pointerEvents: 'none'
          }} />
        </label>
      </div>

      {/* 网络状态提示 */}
      {!isOnline && (
        <div className="error-message">
          ⚠️ 当前网络离线，将使用本地紧急指南
        </div>
      )}

      {/* 错误提示 */}
      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      {/* 分类标签 */}
      <div className="category-section">
        <div className="category-title">快速选择紧急情况：</div>
        <div className="category-tags" style={{
          display: 'flex',
          overflowX: 'auto',
          gap: '8px',
          padding: '8px 0',
          WebkitOverflowScrolling: 'touch',
          scrollbarWidth: 'none',
          msOverflowStyle: 'none'
        }}>
          {categories.map((category) => (
            <button
              key={category.id}
              className="category-tag"
              onClick={() => handleCategoryClick(category)}
              style={{
                flexShrink: 0,
                padding: '8px 12px',
                borderRadius: '16px',
                border: '1px solid #e0e0e0',
                background: 'white',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '4px',
                fontSize: '13px',
                transition: 'all 0.2s ease'
              }}
            >
              <span className="tag-icon">{category.icon}</span>
              <span className="tag-name">{category.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* 消息展示区域 */}
      <div className="messages-container">
        {messages.length === 0 ? (
          <div className="welcome-message">
            <span className="welcome-icon">🏥</span>
            <h3 className="welcome-title">欢迎使用AI紧急助手</h3>
            <p className="welcome-desc">
              遇到紧急情况？请描述您的问题，我将为您提供专业的急救指导。
              <br />
              您也可以点击上方的分类标签快速选择常见紧急情况。
            </p>
          </div>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={`message-item ${message.type} ${message.isEmergency ? 'emergency' : ''}`}
            >
              <div className="message-avatar">
                {message.type === 'user' ? '👤' : '🤖'}
              </div>
              <div className="message-content">
                <div className="message-header">
                  <span className="message-name">
                    {message.type === 'user' ? '您' : 'AI紧急助手'}
                  </span>
                  {message.isEmergency && (
                    <span className="emergency-badge">紧急</span>
                  )}
                  {message.isOfflineResponse && (
                    <span className="offline-badge">离线模式</span>
                  )}
                  <span className="message-time">{message.timestamp}</span>
                </div>
                <div className="message-text">
                  {formatMessageText(message.text)}
                </div>
              </div>
            </div>
          ))
        )}
        
        {/* 加载状态 */}
        {isLoading && (
          <div className="loading-message">
            <div className="loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
            <span className="loading-text">
              {emergencyMode ? '🚨 紧急处理中，请稍候...' : '正在分析您的问题...'}
            </span>
          </div>
        )}
        
        <div ref={messagesEndRef} />
      </div>

      {/* 输入区域 */}
      <div className="input-container">
        <textarea
          className="question-input"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              sendMessage();
            }
          }}
          placeholder="请描述您遇到的紧急情况..."
          disabled={isLoading}
        />
        <div className="input-actions">
          <button
            className={`send-btn ${isLoading ? 'loading' : ''}`}
            onClick={sendMessage}
            disabled={!inputValue.trim() || isLoading}
            title="发送消息"
          >
            {isLoading ? <div className="btn-spinner"></div> : '➤'}
          </button>
        </div>
      </div>

      {/* 全局联系人选择弹窗 */}
      <ContactModal
        isOpen={showContactModal}
        onClose={() => setShowContactModal(false)}
        contacts={emergencyContacts}
      />
    </div>
  );
};

export default EmergencyAssistant;
