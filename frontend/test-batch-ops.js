const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const SCREENSHOT_DIR = path.join(__dirname, 'test-screenshots');

if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

async function screenshot(page, name) {
  const filePath = path.join(SCREENSHOT_DIR, `${name}.png`);
  await page.screenshot({ path: filePath, fullPage: false });
  console.log(`📸 截图已保存: ${filePath}`);
  return filePath;
}

async function main() {
  console.log('🚀 启动浏览器...');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 } });
  const page = await context.newPage();

  try {
    // 步骤1: 导航到首页
    console.log('\n📍 步骤1: 导航到 http://localhost:3000');
    await page.goto('http://localhost:3000', { waitUntil: 'networkidle', timeout: 15000 });
    await screenshot(page, '01-homepage');

    // 步骤2: 登录
    console.log('\n📍 步骤2: 登录');
    // 根据Login.js代码：用户名input placeholder="请输入用户名"，密码 placeholder="请输入密码"
    const usernameInput = await page.$('input[placeholder="请输入用户名"]');
    const passwordInput = await page.$('input[placeholder="请输入密码"]');

    if (usernameInput && passwordInput) {
      console.log('检测到登录页面，开始登录...');
      await screenshot(page, '02-login-page');

      await usernameInput.fill('laowang');
      await passwordInput.fill('123456');
      await screenshot(page, '03-login-filled');

      // 登录按钮文本是 "登 录"（中间有空格），且是 onClick 而非 type=submit
      const loginBtn = await page.$('button:has-text("登 录")');
      if (loginBtn) {
        await loginBtn.click();
        // 等待API响应和页面更新
        await page.waitForResponse(resp => resp.url().includes('/api/v1/user/login'), { timeout: 10000 }).catch(() => {});
        await page.waitForTimeout(3000);
        await screenshot(page, '04-after-login');
        console.log(`登录后URL: ${page.url()}`);
      } else {
        console.log('⚠️ 未找到登录按钮');
      }
    } else {
      console.log('未检测到登录表单，可能已登录');
    }

    // 步骤3: 关闭可能出现的过期提醒弹窗
    console.log('\n📍 步骤3: 关闭可能出现的弹窗');
    // 登录后可能弹出"药品过期提醒"弹窗，需要先关闭
    await page.waitForTimeout(1500);

    // 尝试关闭所有可能出现的弹窗（可能多层）
    for (let attempt = 0; attempt < 5; attempt++) {
      let closed = false;

      // 优先点击"我知道了"按钮
      const iknowBtn = await page.$('button:has-text("我知道了")');
      if (iknowBtn && await iknowBtn.isVisible()) {
        console.log('找到"我知道了"按钮，点击关闭');
        await iknowBtn.click();
        await page.waitForTimeout(500);
        closed = true;
      }

      // 尝试点击 modal-close-btn（✕按钮）
      if (!closed) {
        const closeBtn = await page.$('.modal-close-btn');
        if (closeBtn && await closeBtn.isVisible()) {
          console.log('找到关闭按钮(✕)，点击关闭');
          await closeBtn.click();
          await page.waitForTimeout(500);
          closed = true;
        }
      }

      // 尝试点击 modal-overlay 关闭
      if (!closed) {
        const overlay = await page.$('.modal-overlay');
        if (overlay && await overlay.isVisible()) {
          console.log('点击遮罩层关闭弹窗');
          await overlay.evaluate(el => el.click());
          await page.waitForTimeout(500);
          closed = true;
        }
      }

      if (!closed) break;
    }

    await screenshot(page, '04-modal-closed');

    // 步骤4: 点击"药箱管理"标签
    console.log('\n📍 步骤4: 点击"药箱管理"标签');
    // 根据App.js：按钮文本是 "🏠 药箱管理"
    const drugsTab = await page.$('button:has-text("药箱管理")');
    if (drugsTab) {
      await drugsTab.click();
      await page.waitForTimeout(2000);
      console.log('已点击药箱管理标签');
    } else {
      console.log('⚠️ 未找到药箱管理标签');
      const visibleText = await page.evaluate(() => document.body.innerText.substring(0, 1000));
      console.log('页面文本:\n', visibleText);
    }
    await screenshot(page, '05-drugs-tab');

    // 步骤4: 点击"多选"按钮
    console.log('\n📍 步骤4: 点击"多选"按钮');
    // 根据DrugListView.js：按钮文本是 "☑ 多选"
    const multiSelectBtn = await page.$('button:has-text("多选")');
    if (multiSelectBtn) {
      await multiSelectBtn.click();
      await page.waitForTimeout(1000);
      console.log('已点击多选按钮');
    } else {
      console.log('⚠️ 未找到多选按钮');
      const visibleText = await page.evaluate(() => document.body.innerText.substring(0, 1000));
      console.log('页面文本:\n', visibleText);
    }
    await screenshot(page, '06-multi-select-mode');

    // 步骤5: 选择药品（在多选模式下，点击列表项会触发toggleDrugSelect）
    console.log('\n📍 步骤5: 选择药品');
    // DrugListView中每个药品项在多选模式下点击会切换选中状态
    const drugItems = await page.$$('.drug-list-item');
    console.log(`找到 ${drugItems.length} 个药品列表项`);

    let selectedCount = 0;
    if (drugItems.length > 0) {
      // 在多选模式下，直接点击列表项来选中（不用点击checkbox，避免双重触发）
      const selectCount = Math.min(3, drugItems.length);
      for (let i = 0; i < selectCount; i++) {
        try {
          // 直接点击列表项文本区域（药品名称），避免点击到checkbox
          const nameEl = await drugItems[i].$('.drug-list-name');
          if (nameEl) {
            await nameEl.click();
          } else {
            // 备用：点击列表项但避开checkbox区域
            await drugItems[i].click({ position: { x: 200, y: 15 } });
          }
          selectedCount++;
          console.log(`已选择第 ${i + 1} 个药品`);
          await page.waitForTimeout(300);
        } catch (e) {
          console.log(`选择第 ${i + 1} 个药品失败: ${e.message}`);
        }
      }
    }
    console.log(`共选择了 ${selectedCount} 个药品`);
    await screenshot(page, '07-items-selected');

    // 步骤6: 点击"批量删除"按钮
    console.log('\n📍 步骤6: 点击"批量删除"按钮');
    // DrugListView中按钮文本是 "🗑️ 批量删除"
    const batchDeleteBtn = await page.$('button:has-text("批量删除")');
    let deleteBtnFound = false;
    if (batchDeleteBtn) {
      const isEnabled = await batchDeleteBtn.isEnabled();
      console.log(`批量删除按钮状态: ${isEnabled ? '可用' : '禁用'}`);
      if (isEnabled) {
        await batchDeleteBtn.click();
        await page.waitForTimeout(1000);
        deleteBtnFound = true;
        console.log('已点击批量删除按钮');
      }
    } else {
      console.log('⚠️ 未找到批量删除按钮');
    }
    await screenshot(page, '08-batch-delete-dialog');

    // 步骤7: 检查批量删除确认弹窗
    console.log('\n📍 步骤7: 检查批量删除确认弹窗');
    if (deleteBtnFound) {
      // BatchConfirmModal 使用 createPortal 渲染到 document.body
      // 弹窗有固定定位和半透明背景遮罩
      const dialogInfo = await page.evaluate(() => {
        // 查找弹窗 - 通过固定定位的遮罩层和弹窗内容来识别
        const overlays = document.querySelectorAll('div[style*="position: fixed"]');
        for (const overlay of overlays) {
          const style = overlay.getAttribute('style') || '';
          if (style.includes('z-index') || style.includes('zIndex') || style.includes('rgba(0, 0, 0, 0.5)')) {
            const innerDiv = overlay.querySelector('div');
            if (innerDiv) {
              return {
                found: true,
                text: innerDiv.innerText.substring(0, 500),
                isCustom: true,
                overlayStyle: style.substring(0, 200)
              };
            }
          }
        }
        // 备用：查找包含"确认批量删除"文本的元素
        const allElements = document.querySelectorAll('*');
        for (const el of allElements) {
          if (el.innerText && el.innerText.includes('确认批量删除') && el.offsetParent !== null) {
            return {
              found: true,
              text: el.innerText.substring(0, 500),
              isCustom: true
            };
          }
        }
        return { found: false };
      });

      console.log('弹窗检测结果:', JSON.stringify(dialogInfo, null, 2));

      if (dialogInfo.found) {
        console.log('✅ 检测到自定义确认弹窗（非原生window.confirm）！');
        console.log(`弹窗内容预览: ${dialogInfo.text}`);
      } else {
        console.log('⚠️ 未检测到自定义弹窗');
      }
    }

    // 步骤8: 点击取消关闭删除弹窗
    console.log('\n📍 步骤8: 点击取消关闭删除弹窗');
    // BatchConfirmModal的取消按钮文本是"取消"
    // 需要找到弹窗内的取消按钮（不是其他地方的取消按钮）
    const cancelBtnInModal = await page.$('div[style*="position: fixed"] button:has-text("取消")');
    if (cancelBtnInModal) {
      const isVisible = await cancelBtnInModal.isVisible();
      if (isVisible) {
        await cancelBtnInModal.click();
        await page.waitForTimeout(500);
        console.log('已点击弹窗中的取消按钮');
      }
    } else {
      // 备用方案
      const cancelBtn = await page.$('button:has-text("取消")');
      if (cancelBtn && await cancelBtn.isVisible()) {
        await cancelBtn.click();
        await page.waitForTimeout(500);
        console.log('已点击取消按钮（备用方案）');
      }
    }
    await screenshot(page, '09-after-cancel-delete');

    // 步骤9: 点击"批量丢弃"按钮
    console.log('\n📍 步骤9: 点击"批量丢弃"按钮');
    // DrugListView中按钮文本是 "🗑️ 批量丢弃"
    const batchDiscardBtn = await page.$('button:has-text("批量丢弃")');
    let discardBtnFound = false;
    if (batchDiscardBtn) {
      const isEnabled = await batchDiscardBtn.isEnabled();
      console.log(`批量丢弃按钮状态: ${isEnabled ? '可用' : '禁用'}`);
      if (isEnabled) {
        await batchDiscardBtn.click();
        await page.waitForTimeout(1000);
        discardBtnFound = true;
        console.log('已点击批量丢弃按钮');
      }
    } else {
      console.log('⚠️ 未找到批量丢弃按钮');
    }
    await screenshot(page, '10-batch-discard-dialog');

    // 步骤10: 检查批量丢弃确认弹窗
    console.log('\n📍 步骤10: 检查批量丢弃确认弹窗');
    if (discardBtnFound) {
      const dialogInfo = await page.evaluate(() => {
        const overlays = document.querySelectorAll('div[style*="position: fixed"]');
        for (const overlay of overlays) {
          const style = overlay.getAttribute('style') || '';
          if (style.includes('rgba(0, 0, 0, 0.5)')) {
            const innerDiv = overlay.querySelector('div');
            if (innerDiv) {
              return {
                found: true,
                text: innerDiv.innerText.substring(0, 500),
                isCustom: true,
              };
            }
          }
        }
        const allElements = document.querySelectorAll('*');
        for (const el of allElements) {
          if (el.innerText && el.innerText.includes('确认批量丢弃') && el.offsetParent !== null) {
            return {
              found: true,
              text: el.innerText.substring(0, 500),
              isCustom: true
            };
          }
        }
        return { found: false };
      });

      console.log('弹窗检测结果:', JSON.stringify(dialogInfo, null, 2));

      if (dialogInfo.found) {
        console.log('✅ 检测到自定义确认弹窗（非原生window.confirm）！');
        console.log(`弹窗内容预览: ${dialogInfo.text}`);
      } else {
        console.log('⚠️ 未检测到自定义弹窗');
      }
    }

    // 步骤11: 点击取消关闭丢弃弹窗
    console.log('\n📍 步骤11: 点击取消关闭丢弃弹窗');
    const cancelBtnInModal2 = await page.$('div[style*="position: fixed"] button:has-text("取消")');
    if (cancelBtnInModal2) {
      const isVisible = await cancelBtnInModal2.isVisible();
      if (isVisible) {
        await cancelBtnInModal2.click();
        await page.waitForTimeout(500);
        console.log('已点击弹窗中的取消按钮');
      }
    } else {
      const cancelBtn = await page.$('button:has-text("取消")');
      if (cancelBtn && await cancelBtn.isVisible()) {
        await cancelBtn.click();
        await page.waitForTimeout(500);
        console.log('已点击取消按钮（备用方案）');
      }
    }
    await screenshot(page, '11-after-cancel-discard');

    console.log('\n✅ 测试完成！所有截图已保存到:', SCREENSHOT_DIR);

  } catch (error) {
    console.error('❌ 测试出错:', error.message);
    await screenshot(page, 'error-state').catch(() => {});
  } finally {
    await browser.close();
  }
}

main().catch(console.error);
