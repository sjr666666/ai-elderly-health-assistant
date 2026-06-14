package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.EmergencyContactRepository;
import com.example.backend.model.entity.EmergencyContact;
import com.example.backend.service.EmergencyContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmergencyContactServiceImpl implements EmergencyContactService {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyContactServiceImpl.class);

    private final EmergencyContactRepository emergencyContactRepository;

    @Autowired
    public EmergencyContactServiceImpl(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
    }

    @Override
    public List<EmergencyContact> getContactsByElderId(Long elderId) {
        logger.info("获取所有紧急联系人 - elderId: {}", elderId);

        LambdaQueryWrapper<EmergencyContact> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmergencyContact::getElderId, elderId);
        queryWrapper.orderByDesc(EmergencyContact::getIsPrimary);

        List<EmergencyContact> contacts = emergencyContactRepository.selectList(queryWrapper);

        logger.info("找到 {} 个紧急联系人 - elderId: {}", contacts.size(), elderId);
        contacts.forEach(contact -> 
            logger.info("联系人 - id: {}, name: {}, isPrimary: {}", contact.getId(), contact.getName(), contact.getIsPrimary())
        );
        return contacts;
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

        // 重复校验：同一老人下相同姓名+电话不能重复
        LambdaQueryWrapper<EmergencyContact> check = new LambdaQueryWrapper<>();
        check.eq(EmergencyContact::getElderId, contact.getElderId())
                .eq(EmergencyContact::getName, contact.getName())
                .eq(EmergencyContact::getPhone, contact.getPhone());
        if (emergencyContactRepository.selectCount(check) > 0) {
            throw new IllegalArgumentException("该联系人已存在");
        }

        emergencyContactRepository.insert(contact);

        logger.info("紧急联系人保存成功 - id: {}", contact.getId());
        return contact;
    }

    @Override
    public boolean deleteContact(Long id) {
        logger.info("删除紧急联系人 - id: {}", id);

        int result = emergencyContactRepository.deleteById(id);

        if (result > 0) {
            logger.info("紧急联系人删除成功 - id: {}", id);
            return true;
        } else {
            logger.warn("删除紧急联系人失败 - id: {}", id);
            return false;
        }
    }
}
