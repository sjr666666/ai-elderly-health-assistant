package com.example.backend.service;

import com.example.backend.model.entity.EmergencyContact;

public interface EmergencyContactService {

    /**
     * 根据老人ID获取紧急联系人
     * 优先返回主要联系人，如果没有则返回第一个联系人
     * @param elderId 老人ID
     * @return 紧急联系人，如果没有则返回null
     */
    EmergencyContact getContactByElderId(Long elderId);

    /**
     * 更新紧急联系人信息
     * @param contact 紧急联系人实体
     * @return 更新后的紧急联系人
     */
    EmergencyContact updateContact(EmergencyContact contact);

    /**
     * 保存新的紧急联系人
     * @param contact 紧急联系人实体
     * @return 保存后的紧急联系人
     */
    EmergencyContact saveContact(EmergencyContact contact);
}
