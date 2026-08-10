# -*- coding: utf-8 -*-
"""
开源药品数据集采集脚本：CN-Drug-KG-800 → RAG 知识库 Markdown
来源: https://huggingface.co/datasets/Sofun2009/yzint-drug-data (cc-by-nc-4.0, 学术/竞赛用途)
输入: scripts/rag-dataset/entities.jsonl + relations.jsonl (实为 JSON 数组)
输出: src/main/resources/knowledge/drugs/*.md (每药一个文件, YAML front-matter)
"""
import json
import os
import re
import sys

BASE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.normpath(os.path.join(BASE, "../../src/main/resources/knowledge/drugs"))

SOURCE_REF = "CN-Drug-KG-800 开源数据集（成分/适应症/禁忌/相互作用，仅供参考，遵医嘱）"


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def safe_name(name):
    """生成安全的文件名（去非法字符）"""
    return re.sub(r'[\\/:*?"<>|#%&\s]+', "-", str(name)).strip("-") or "drug"


def dedup_items(items):
    """子串级去重：条目互为包含时保留更完整的那条，消除关系文本的冗余重复"""
    out = []
    for it in items:
        it = str(it).strip()
        if not it:
            continue
        replaced = False
        for i, o in enumerate(out):
            if it == o:
                replaced = True
                break
            if it in o:          # 新条目是已有条目的子串 → 跳过
                replaced = True
                break
            if o in it:          # 已有条目是新条目的子串 → 用更完整的替换
                out[i] = it
                replaced = True
                break
        if not replaced:
            out.append(it)
    return out


def main():
    entities = load_json(os.path.join(BASE, "entities.jsonl"))
    relations = load_json(os.path.join(BASE, "relations.jsonl"))
    print(f"实体 {len(entities)} 条, 关系 {len(relations)} 条")

    # 实体索引: code -> entity
    by_code = {e.get("code"): e for e in entities}
    # 药品实体
    drugs = [e for e in entities if e.get("type") == "drug"]
    print(f"药品实体 {len(drugs)} 个")

    # 按药品聚合关系
    drug_relations = {}
    for r in relations:
        src_name = r.get("sourceName")
        if src_name in drug_relations:
            drug_relations[src_name].append(r)
        else:
            drug_relations[src_name] = [r]

    os.makedirs(OUT_DIR, exist_ok=True)
    written = 0
    for drug in drugs:
        name = drug.get("name")
        if not name:
            continue
        desc = drug.get("description") or ""
        rels = drug_relations.get(name, [])

        # 按 target 实体类型分组关系，转中文要点
        sections = {"成分": [], "适应症": [], "给药途径": [], "禁忌": [], "相互作用": [], "注意事项": []}
        for r in rels:
            rel_text = (r.get("relationship") or "").strip()
            if not rel_text:
                continue
            target_name = r.get("targetName") or ""
            target_type = by_code.get(r.get("target"), {}).get("type", "")
            # 用 target 类型归类，无类型时按 relationship 文本关键词归类
            if target_type == "ingredient":
                sections["成分"].append(target_name)
            elif "禁忌" in rel_text or "contraindication" in rel_text.lower() or target_type == "contraindication":
                sections["禁忌"].append(rel_text)
            elif "适应" in rel_text or "indication" in rel_text.lower():
                sections["适应症"].append(rel_text)
            elif "途径" in rel_text or "admin" in rel_text.lower() or "口服" in rel_text or "注射" in rel_text:
                sections["给药途径"].append(rel_text)
            elif "相互" in rel_text or "interaction" in rel_text.lower():
                sections["相互作用"].append(rel_text)
            elif "注意" in rel_text or "warning" in rel_text.lower():
                sections["注意事项"].append(rel_text)

        # 组装正文（先去重）
        lines = []
        if sections["成分"]:
            lines.append("【主要成分】" + "、".join(dedup_items(sections["成分"])) + "。")
        if sections["适应症"]:
            lines.append("【适应症】" + "；".join(dedup_items(sections["适应症"])) + "。")
        if sections["给药途径"]:
            lines.append("【给药途径】" + "；".join(dedup_items(sections["给药途径"])) + "。")
        if sections["禁忌"]:
            lines.append("【禁忌】" + "；".join(dedup_items(sections["禁忌"])) + "。")
        if sections["相互作用"]:
            lines.append("【药物相互作用】" + "；".join(dedup_items(sections["相互作用"])) + "。")
        if sections["注意事项"]:
            lines.append("【注意事项】" + "；".join(dedup_items(sections["注意事项"])) + "。")
        if desc and desc not in lines:
            lines.insert(0, "【说明书摘要】" + desc)

        if not lines:
            lines.append("暂无结构化信息，具体以药品说明书为准。")

        body = "\n".join(lines)
        md = (
            "---\n"
            f"title: {name}\n"
            "type: drug\n"
            f"source: {SOURCE_REF}\n"
            f"tags: [{name}, 药品]\n"
            "---\n"
            f"{body}\n"
        )
        fname = f"{written + 1:03d}-{safe_name(name)}.md"
        with open(os.path.join(OUT_DIR, fname), "w", encoding="utf-8") as f:
            f.write(md)
        written += 1

    print(f"已生成 {written} 个药品知识文件 → {OUT_DIR}")


if __name__ == "__main__":
    sys.exit(main())
