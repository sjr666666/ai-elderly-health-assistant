#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAG 回答质量评测集：20 题检索命中率评测
========================================
对「用药问问」知识问答跑 20 道代表性问题，逐题检查回答引用的资料（sources）
是否命中期望关键词，汇总检索命中率，输出评测报告。

用法（需后端已启动，且知识库已入库）：
    python evaluate_rag.py --base http://localhost:8081 --username laowang --password 123456
    python evaluate_rag.py --base http://localhost:8081 --username laowang --password 123456 --json

评分口径：
    - 命中：回答引用的任一资料 title 或内容片段包含期望关键词（任一即可）
    - 命中率 = 命中题数 / 总题数（期望 >= 85%，否则说明检索兜底需要加强）
    - 附带输出每题的检索模式（VECTOR / KEYWORD / LOCAL），便于定位降级原因
"""

import argparse
import json
import sys
import time
import urllib.error
import urllib.request

# ============ 评测集：20 题（期望命中关键词，任一命中即算通过） ============
QUESTIONS = [
    {"q": "阿司匹林能和降压药一起吃吗", "keywords": ["阿司匹林", "相互作用", "抗凝"]},
    {"q": "布洛芬有哪些注意事项", "keywords": ["布洛芬"]},
    {"q": "药过期了还能吃吗", "keywords": ["过期", "有效期"]},
    {"q": "忘记吃药了怎么办", "keywords": ["漏服", "忘记", "补服"]},
    {"q": "高血压患者饮食上要注意什么", "keywords": ["高血压"]},
    {"q": "糖尿病日常怎么管理", "keywords": ["糖尿病"]},
    {"q": "缓释片能掰开吃吗", "keywords": ["缓释", "掰"]},
    {"q": "感冒药和退烧药能一起吃吗", "keywords": ["感冒", "退烧", "对乙酰氨基酚"]},
    {"q": "阿莫西林是抗生素吗", "keywords": ["阿莫西林", "抗生素"]},
    {"q": "降糖药应该饭前吃还是饭后吃", "keywords": ["降糖", "二甲双胍"]},
    {"q": "硝酸甘油片怎么含服", "keywords": ["硝酸甘油"]},
    {"q": "他汀类降脂药有什么副作用", "keywords": ["他汀", "阿托伐他汀"]},
    {"q": "中成药和西药能一起吃吗", "keywords": ["中成药", "中药", "相互作用"]},
    {"q": "维生素C和钙片能一起吃吗", "keywords": ["维生素", "钙"]},
    {"q": "老年人失眠可以吃什么药", "keywords": ["失眠", "安眠"]},
    {"q": "高血压吃降压药的注意事项", "keywords": ["高血压", "降压"]},
    {"q": "吃了头孢能喝酒吗", "keywords": ["头孢", "酒精"]},
    {"q": "孕妇感冒了能吃药吗", "keywords": ["孕妇", "孕期", "妊娠"]},
    {"q": "心脏病患者用药要注意什么", "keywords": ["心脏", "心衰", "心血管"]},
    {"q": "藿香正气水怎么喝", "keywords": ["藿香正气"]},
]


def http_json(url, method="GET", token=None, body=None, timeout=60):
    """极简 HTTP JSON 请求（仅标准库）"""
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json;charset=UTF-8"
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def login(base, username, password):
    """登录拿 JWT token"""
    data = http_json(base + "/api/v1/user/login", method="POST", body={
        "username": username, "password": password})
    if data.get("code") != 200 or not data.get("data", {}).get("token"):
        raise RuntimeError("登录失败: %s" % json.dumps(data, ensure_ascii=False))
    return data["data"]["token"]


def ask(base, token, question):
    """调用 /api/rag/ask，返回 answer/mode/sources/userDrugs"""
    data = http_json(base + "/api/rag/ask", method="POST", token=token,
                     body={"question": question}, timeout=90)
    if data.get("code") != 200:
        return None, data.get("message", "请求失败"), []
    ans = data.get("data") or {}
    return ans.get("answer"), ans.get("mode"), ans.get("sources") or []


def hit(sources, keywords):
    """任一条资料标题/内容含任一关键词 → 命中"""
    for s in sources:
        title = (s.get("title") or "") + (s.get("sourceRef") or "")
        content = s.get("content") or ""
        for kw in keywords:
            if kw in title or kw in content:
                return True
    return False


def main():
    parser = argparse.ArgumentParser(description="RAG 20 题检索命中率评测")
    parser.add_argument("--base", default="http://localhost:8081", help="后端地址（默认 http://localhost:8081）")
    parser.add_argument("--username", default="laowang", help="老人端测试账号（默认 laowang）")
    parser.add_argument("--password", default="123456", help="密码（默认 123456）")
    parser.add_argument("--json", action="store_true", help="输出 JSON 结果")
    parser.add_argument("--sleep", type=float, default=0.5, help="每题间隔秒数（默认 0.5）")
    args = parser.parse_args()

    base = args.base.rstrip("/")
    token = login(base, args.username, args.password)
    print("登录成功，开始评测 %d 题（后端 %s）...\n" % (len(QUESTIONS), base))

    results = []
    passed = 0
    mode_counter = {}
    for i, item in enumerate(QUESTIONS, 1):
        answer, mode, sources = ask(base, token, item["q"])
        mode_counter[mode] = mode_counter.get(mode, 0) + 1
        ok = bool(sources) and hit(sources, item["keywords"])
        if ok:
            passed += 1
        results.append({
            "no": i, "question": item["q"], "keywords": item["keywords"],
            "hit": ok, "mode": mode, "sources": len(sources),
            "topTitle": sources[0]["title"] if sources else "",
        })
        print("%2d. %s %s [%s] 引用%d条 top:%s" % (
            i, "✅" if ok else "❌", item["q"], mode or "-",
            len(sources), results[-1]["topTitle"][:30]))
        time.sleep(args.sleep)

    hit_rate = passed / len(QUESTIONS) * 100
    print("\n========== 评测汇总 ==========")
    print("命中率: %d/%d = %.1f%%（目标 >= 85%%）" % (passed, len(QUESTIONS), hit_rate))
    print("检索模式分布: %s" % json.dumps(mode_counter, ensure_ascii=False))
    if args.json:
        print(json.dumps({"hitRate": round(hit_rate, 1), "passed": passed,
                          "total": len(QUESTIONS), "modes": mode_counter,
                          "details": results}, ensure_ascii=False, indent=2))
    if hit_rate < 85:
        print("\n⚠️ 命中率低于目标，建议：")
        print("  1. 检查未命中题的期望关键词是否过严（资料标题与关键词表述差异）")
        print("  2. 配 SiliconFlow Key 后重灌知识库（/api/rag/ingest），语义检索更稳")
        print("  3. 对高频未命中题补充知识条目（resources/knowledge/ 加 .md 即可）")
        sys.exit(1)


if __name__ == "__main__":
    main()
