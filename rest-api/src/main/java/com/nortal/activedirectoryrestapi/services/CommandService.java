package com.nortal.activedirectoryrestapi.services;

import com.nortal.activedirectoryrestapi.entities.Commands;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

@Service
public class CommandService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Long saveCommand(String command, String params) {
        Commands commandsEntity = new Commands();
        commandsEntity.setCommand(command);
        commandsEntity.setArguments(params);
        commandsEntity.setCommandStatus("PENDING");
        entityManager.persist(commandsEntity);
        entityManager.flush();  // Ensure the entity is saved immediately
        return commandsEntity.getId();
    }

    @Transactional(readOnly = true)
    public Commands getCommand(Long id) {
        Commands entity = entityManager.find(Commands.class, id);
        entityManager.refresh(entity);  // Refresh to get latest data
        return entity;
    }

}

