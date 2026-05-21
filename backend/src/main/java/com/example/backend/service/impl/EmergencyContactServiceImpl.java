package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.EmergencyContactRepository;
import com.example.backend.model.entity.EmergencyContact;
import com.example.backend.service.EmergencyContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmergencyContactServiceImpl implements EmergencyContactService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyContactServiceImpl.class);

    private final EmergencyContactRepository emergencyContactRepository;

    @Autowired
    public EmergencyContactServiceImpl(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
    }

    @Override
    public EmergencyContact getContactByElderId(Long elderId) {
        logger.info("获取紧急联系人 - elderId: {}", elderId);

        LambdaQueryWrapper<EmergencyContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmergencyContact::getElderId, elderId);
        queryWrapper.orderByDesc(EmergencyContact::getIsPrimary);
        queryWrapper.last("LIMIT 1");

        EmergencyContact contact = emergencyContactRepository.selectOne(queryWrapper);

        if (contact == null) {
            logger.info("未找到紧急联系人 - elderId: {}", elderId);
        } else {
            logger.info("找到紧急联系人 - id: {}, name: {}", contact.getId(), contact.getName());
        }

        return contact;
    }

    @Override
    public EmergencyContact updateContact(EmergencyContact contact) {
        logger.info("更新紧急联系人 - id: {}, elderId: {}", contact.getId(), contact.getElderId());

        emergencyContactRepository.updateById(contact);

        logger.info("紧急联系人更新成功 - id: {}", contact.getId());
        return contact;
    }

    @Override
    public EmergencyContact saveContact(EmergencyContact contact) {
        logger.info("保存紧急联系人 - elderId: {}, name: {}", contact.getElderId(), contact.getName());

        emergencyContactRepository.insert(contact);

        logger.info("紧急联系人保存成功 - id: {}", contact.getId());
        return contact;
    }
}
