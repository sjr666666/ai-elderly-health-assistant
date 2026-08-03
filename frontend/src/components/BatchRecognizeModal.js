import React, { useState, useRef } from 'react';
import { useToast } from './Toast';
import { getToken } from '../utils/elderApi';

/**
 * 批量识别药品弹窗组件
 * 支持一键拍照批量识药，多张缩略图展示确认，可一次性全部加入药箱
 */
function BatchRecognizeModal({ onClose, onAddToBox, userId }) {
  const { showToast } = useToast();
  const fileInputRef = useRef(null);
  const cameraInputRef = useRef(null);

  // 状态管理
  const [selectedImages, setSelectedImages] = useState([]); // 已选择的图片
  const [isRecognizing, setIsRecognizing] = useState(false); // 是否正在识别
  const [selectedForAdd, setSelectedForAdd] = useState(new Set()); // 选中的要添加的药品
  const [isAddingToBox, setIsAddingToBox] = useState(false); // 是否正在添加到药箱

  // 将WebP图片转换为JPEG格式
  const convertToJpeg = (file) => {
    return new Promise((resolve, reject) => {
      if (!file.type.includes('webp')) {
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
              const convertedFile = new File([blob], file.name.replace('.webp', '.jpg'), { type: 'image/jpeg' });
              resolve(convertedFile);
            } else {
              resolve(file);
            }
          }, 'image/jpeg', 0.95);
        };
        img.onerror = () => resolve(file);
        img.src = e.target.result;
      };
      reader.onerror = () => resolve(file);
      reader.readAsDataURL(file);
    });
  };

  // 处理文件选择
  const handleFileSelect = async (e) => {
    const files = Array.from(e.target.files);
    if (files.length === 0) return;

    const MAX_SIZE = 10 * 1024 * 1024; // 10MB
    const MIN_PX = 15;
    const MAX_PX = 4096;
    const validFiles = [];

    for (const f of files) {
      if (f.size > MAX_SIZE) {
        showToast(`${f.name} 超过 10MB，已跳过`, 'warning');
        continue;
      }
      const ok = await new Promise((resolve) => {
        const url = URL.createObjectURL(f);
        const img = new Image();
        img.onload = () => {
          URL.revokeObjectURL(url);
          if (img.width < MIN_PX || img.height < MIN_PX) {
            showToast(`${f.name} 图片太小（最小 15×15 像素），已跳过`, 'warning');
            resolve(false);
          } else if (img.width > MAX_PX || img.height > MAX_PX) {
            showToast(`${f.name} 尺寸过大，请使用不超过 4096×4096 像素的图片，已跳过`, 'warning');
            resolve(false);
          } else {
            resolve(true);
          }
        };
        img.onerror = () => {
          URL.revokeObjectURL(url);
          showToast(`${f.name} 无法读取，已跳过`, 'warning');
          resolve(false);
        };
        img.src = url;
      });
      if (ok) validFiles.push(f);
    }

    if (validFiles.length === 0) return;

    // 转换为JPEG格式
    const convertedFiles = await Promise.all(validFiles.map(convertToJpeg));

    // 创建预览URL
    const newImages = convertedFiles.map((file, index) => ({
      id: Date.now() + index,
      file: file,
      previewUrl: URL.createObjectURL(file),
      status: 'pending', // pending, recognizing, success, failed
      result: null
    }));

    setSelectedImages(prev => [...prev, ...newImages]);
    e.target.value = '';
  };

  // 触发文件选择
  const triggerFileSelect = () => {
    fileInputRef.current?.click();
  };

  // 触发相机拍照
  const triggerCamera = () => {
    cameraInputRef.current?.click();
  };

  // 移除图片
  const removeImage = (imageId) => {
    setSelectedImages(prev => {
      const image = prev.find(img => img.id === imageId);
      if (image?.previewUrl) {
        URL.revokeObjectURL(image.previewUrl);
      }
      return prev.filter(img => img.id !== imageId);
    });
    // 同时从选中添加到药箱的列表中移除
    setSelectedForAdd(prev => {
      const newSet = new Set(prev);
      newSet.delete(imageId);
      return newSet;
    });
  };

  // 批量识别所有图片
  const recognizeAllImages = async () => {
    if (selectedImages.length === 0) {
      showToast('请先选择要识别的图片', 'warning');
      return;
    }

    setIsRecognizing(true);

    try {
      // 准备FormData
      const formData = new FormData();
      selectedImages.forEach(img => {
        formData.append('files', img.file);
      });

      // 调用批量识别API
      const response = await fetch('/api/v1/drug/recognize/batch-upload', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${getToken()}`
        },
        body: formData
      });

      const data = await response.json();

      if (data.code === 200 && data.data) {
        const results = data.data.items || [];

        // 更新图片状态和结果
        setSelectedImages(prev => prev.map((img, index) => {
          const result = results[index] || {};
          return {
            ...img,
            status: result.status === 'matched' ? 'success' : (result.status === 'failed' ? 'failed' : 'failed'),
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
        selectedImages.forEach((img, index) => {
          const result = results[index] || {};
          if (result.status === 'matched' && result.matchedDrugId) {
            newSelected.add(img.id);
          }
        });
        setSelectedForAdd(newSelected);

        showToast(`识别完成！成功: ${data.data.successCount}, 失败: ${data.data.failedCount}`,
          data.data.failedCount > 0 ? 'warning' : 'success');
      } else {
        showToast(data.message || '批量识别失败', 'error');
      }
    } catch (error) {
      console.error('批量识别失败:', error);
      showToast('批量识别失败，请检查网络连接', 'error');
    } finally {
      setIsRecognizing(false);
    }
  };

  // 切换选中状态
  const toggleSelect = (imageId) => {
    setSelectedForAdd(prev => {
      const newSet = new Set(prev);
      if (newSet.has(imageId)) {
        newSet.delete(imageId);
      } else {
        newSet.add(imageId);
      }
      return newSet;
    });
  };

  // 全选/取消全选
  const toggleSelectAll = () => {
    const successImages = selectedImages.filter(img => img.status === 'success' && img.result?.drugId);
    const allSelected = successImages.every(img => selectedForAdd.has(img.id));

    if (allSelected) {
      // 取消全选
      setSelectedForAdd(prev => {
        const newSet = new Set(prev);
        successImages.forEach(img => newSet.delete(img.id));
        return newSet;
      });
    } else {
      // 全选
      setSelectedForAdd(prev => {
        const newSet = new Set(prev);
        successImages.forEach(img => newSet.add(img.id));
        return newSet;
      });
    }
  };

  // 全部加入药箱
  const addAllToMedicineBox = async () => {
    if (selectedForAdd.size === 0) {
      showToast('请选择要添加到药箱的药品', 'warning');
      return;
    }

    setIsAddingToBox(true);

    try {
      const selectedImagesList = selectedImages.filter(img => selectedForAdd.has(img.id));
      let successCount = 0;
      let failCount = 0;

      for (const img of selectedImagesList) {
        if (!img.result?.drugId) continue;

        try {
          const response = await fetch(`/api/v1/box`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${getToken()}`,
            },
            body: JSON.stringify({
              drugId: img.result.drugId,
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
        onAddToBox?.();
        onClose();
      } else if (failCount > 0) {
        showToast(`添加失败 ${failCount} 个药品`, 'error');
      }
    } finally {
      setIsAddingToBox(false);
    }
  };

  // 计算已识别成功的数量
  const successCount = selectedImages.filter(img => img.status === 'success' && img.result?.drugId).length;
  const allSelected = successCount > 0 &&
    selectedImages.filter(img => img.status === 'success' && img.result?.drugId)
      .every(img => selectedForAdd.has(img.id));

  return (
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
      <div style={{
        background: 'white',
        borderRadius: '32px',
        padding: '48px',
        width: '100%',
        maxWidth: '900px',
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
          onClick={onClose}
          onMouseEnter={(e) => e.target.style.background = '#E0E0E0'}
          onMouseLeave={(e) => e.target.style.background = '#F5F5F5'}
        >
          ✕
        </button>

        {/* 标题 */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <div style={{ fontSize: '64px', marginBottom: '16px' }}>📸</div>
          <h2 style={{
            fontSize: '32px',
            fontWeight: '800',
            color: '#4A90E2',
            marginBottom: '8px'
          }}>
            批量拍照识药
          </h2>
          <p style={{ fontSize: '18px', color: '#6B6B6B' }}>
            一键拍照批量识别多种药品，可一次性全部加入药箱
          </p>
        </div>

        {/* 隐藏的文件输入 */}
        <input
          type="file"
          ref={fileInputRef}
          accept="image/*"
          multiple
          onChange={handleFileSelect}
          style={{ display: 'none' }}
        />
        <input
          type="file"
          ref={cameraInputRef}
          accept="image/*"
          capture="environment"
          multiple
          onChange={handleFileSelect}
          style={{ display: 'none' }}
        />

        {/* 操作按钮 */}
        <div style={{
          display: 'flex',
          gap: '16px',
          marginBottom: '24px',
          justifyContent: 'center'
        }}>
          <button
            onClick={triggerFileSelect}
            style={{
              padding: '16px 32px',
              fontSize: '18px',
              fontWeight: '700',
              border: '3px solid #4A90E2',
              borderRadius: '16px',
              background: 'white',
              color: '#4A90E2',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = '#4A90E2';
              e.target.style.color = 'white';
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'white';
              e.target.style.color = '#4A90E2';
            }}
          >
            📁 选择图片
          </button>
          <button
            onClick={triggerCamera}
            style={{
              padding: '16px 32px',
              fontSize: '18px',
              fontWeight: '700',
              border: '3px solid #4CAF50',
              borderRadius: '16px',
              background: 'white',
              color: '#4CAF50',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}
            onMouseEnter={(e) => {
              e.target.style.background = '#4CAF50';
              e.target.style.color = 'white';
            }}
            onMouseLeave={(e) => {
              e.target.style.background = 'white';
              e.target.style.color = '#4CAF50';
            }}
          >
            📷 拍照
          </button>
        </div>

        {/* 图片预览网格 */}
        {selectedImages.length > 0 && (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))',
            gap: '16px',
            marginBottom: '24px'
          }}>
            {selectedImages.map((img) => (
              <div
                key={img.id}
                style={{
                  position: 'relative',
                  borderRadius: '16px',
                  overflow: 'hidden',
                  border: selectedForAdd.has(img.id) ? '3px solid #4CAF50' : '3px solid #E0E0E0',
                  background: '#F5F5F5',
                  cursor: img.status === 'success' ? 'pointer' : 'default'
                }}
                onClick={() => img.status === 'success' && img.result?.drugId && toggleSelect(img.id)}
              >
                <img
                  src={img.previewUrl}
                  alt="预览"
                  style={{
                    width: '100%',
                    height: '120px',
                    objectFit: 'cover'
                  }}
                />

                {/* 状态图标 */}
                <div style={{
                  position: 'absolute',
                  top: '8px',
                  right: '8px',
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  background: img.status === 'success' ? '#4CAF50' :
                    img.status === 'failed' ? '#E74C3C' :
                      img.status === 'recognizing' ? '#F39C12' : '#9E9E9E',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontSize: '16px'
                }}>
                  {img.status === 'success' ? '✓' :
                    img.status === 'failed' ? '✗' :
                      img.status === 'recognizing' ? '⏳' : '?'}
                </div>

                {/* 删除按钮 */}
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    removeImage(img.id);
                  }}
                  style={{
                    position: 'absolute',
                    top: '8px',
                    left: '8px',
                    width: '28px',
                    height: '28px',
                    borderRadius: '50%',
                    border: 'none',
                    background: 'rgba(0,0,0,0.5)',
                    color: 'white',
                    fontSize: '14px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  ✕
                </button>

                {/* 药品名称 */}
                {img.result?.drugName && (
                  <div style={{
                    position: 'absolute',
                    bottom: 0,
                    left: 0,
                    right: 0,
                    background: 'rgba(0,0,0,0.7)',
                    color: 'white',
                    padding: '8px',
                    fontSize: '12px',
                    textAlign: 'center',
                    maxHeight: '40px',
                    overflow: 'hidden'
                  }}>
                    {img.result.drugName}
                  </div>
                )}

                {/* 选择指示器 */}
                {selectedForAdd.has(img.id) && (
                  <div style={{
                    position: 'absolute',
                    bottom: '8px',
                    right: '8px',
                    width: '24px',
                    height: '24px',
                    borderRadius: '50%',
                    background: '#4CAF50',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white',
                    fontSize: '14px',
                    fontWeight: 'bold'
                  }}>
                    ✓
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* 空状态提示 */}
        {selectedImages.length === 0 && (
          <div style={{
            textAlign: 'center',
            padding: '48px',
            color: '#9E9E9E',
            fontSize: '18px'
          }}>
            <div style={{ fontSize: '64px', marginBottom: '16px' }}>💊</div>
            <p>点击上方按钮选择图片或拍照</p>
            <p style={{ fontSize: '14px', marginTop: '8px' }}>支持同时选择多张图片进行批量识别</p>
          </div>
        )}

        {/* 底部操作栏 */}
        <div style={{
          display: 'flex',
          gap: '16px',
          justifyContent: 'space-between',
          alignItems: 'center',
          paddingTop: '24px',
          borderTop: '2px solid #F0F0F0'
        }}>
          {/* 左侧统计 */}
          <div style={{ fontSize: '16px', color: '#6B6B6B' }}>
            已选择 <strong style={{ color: '#4A90E2' }}>{selectedImages.length}</strong> 张图片，
            可添加 <strong style={{ color: '#4CAF50' }}>{selectedForAdd.size}</strong> 个药品
          </div>

          {/* 右侧按钮 */}
          <div style={{ display: 'flex', gap: '16px' }}>
            <button
              onClick={onClose}
              style={{
                padding: '16px 32px',
                fontSize: '18px',
                fontWeight: '700',
                border: '3px solid #E0E0E0',
                borderRadius: '16px',
                background: 'white',
                color: '#6B6B6B',
                cursor: 'pointer'
              }}
            >
              取消
            </button>

            {selectedImages.length > 0 && !isRecognizing && (
              <button
                onClick={triggerFileSelect}
                style={{
                  padding: '16px 32px',
                  fontSize: '18px',
                  fontWeight: '700',
                  border: 'none',
                  borderRadius: '16px',
                  background: '#F5F5F5',
                  color: '#4A90E2',
                  cursor: 'pointer'
                }}
              >
                + 继续添加
              </button>
            )}

            {selectedImages.length > 0 && !isRecognizing && (
              <button
                onClick={recognizeAllImages}
                disabled={selectedImages.length === 0}
                style={{
                  padding: '16px 32px',
                  fontSize: '18px',
                  fontWeight: '700',
                  border: 'none',
                  borderRadius: '16px',
                  background: selectedImages.length === 0 ? '#B0BEC5' : 'linear-gradient(135deg, #4A90E2 0%, #357ABD 100%)',
                  color: 'white',
                  cursor: selectedImages.length === 0 ? 'not-allowed' : 'pointer',
                  boxShadow: selectedImages.length === 0 ? 'none' : '0 8px 24px rgba(74, 144, 226, 0.3)'
                }}
              >
                🔍 开始识别
              </button>
            )}

            {isRecognizing && (
              <button
                disabled
                style={{
                  padding: '16px 32px',
                  fontSize: '18px',
                  fontWeight: '700',
                  border: 'none',
                  borderRadius: '16px',
                  background: '#B0BEC5',
                  color: 'white',
                  cursor: 'not-allowed'
                }}
              >
                ⏳ 识别中...
              </button>
            )}

            {successCount > 0 && (
              <>
                <button
                  onClick={toggleSelectAll}
                  style={{
                    padding: '16px 24px',
                    fontSize: '18px',
                    fontWeight: '700',
                    border: '3px solid #4CAF50',
                    borderRadius: '16px',
                    background: allSelected ? '#4CAF50' : 'white',
                    color: allSelected ? 'white' : '#4CAF50',
                    cursor: 'pointer',
                    transition: 'all 0.3s ease'
                  }}
                >
                  {allSelected ? '☑ 取消全选' : '☐ 全选'}
                </button>
                <button
                  onClick={addAllToMedicineBox}
                  disabled={selectedForAdd.size === 0 || isAddingToBox}
                  style={{
                    padding: '16px 32px',
                    fontSize: '18px',
                    fontWeight: '700',
                    border: 'none',
                    borderRadius: '16px',
                    background: selectedForAdd.size === 0 ? '#B0BEC5' : 'linear-gradient(135deg, #4CAF50 0%, #388E3C 100%)',
                    color: 'white',
                    cursor: selectedForAdd.size === 0 ? 'not-allowed' : 'pointer',
                    boxShadow: selectedForAdd.size === 0 ? 'none' : '0 8px 24px rgba(76, 175, 80, 0.3)'
                  }}
                >
                  {isAddingToBox ? '⏳ 添加中...' : `✅ 全部加入药箱 (${selectedForAdd.size})`}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default BatchRecognizeModal;
