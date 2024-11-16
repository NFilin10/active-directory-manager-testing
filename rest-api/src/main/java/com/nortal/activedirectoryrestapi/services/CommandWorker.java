package com.nortal.activedirectoryrestapi.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nortal.activedirectoryrestapi.entities.Commands;
import com.nortal.activedirectoryrestapi.exceptions.ADCommandExecutionException;
import com.nortal.activedirectoryrestapi.misc.ErrorObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import static java.lang.Thread.sleep;

@Service
@RequiredArgsConstructor
public class CommandWorker {

    private final CommandService commandService;
    private final JSONHandler jsonHandler;
    private final ErrorHandler errorHandler;

    public Commands checkCommandStatus(Long id) throws InterruptedException {
        while (true) {
            Commands refreshedEntity = commandService.getCommand(id);  // Refresh from DB

            if (refreshedEntity.getCommandStatus().equals("COMPLETED")) {
                return refreshedEntity;
            }

            sleep(1000);  // Polling interval
        }
    }

    public Commands executeCommand(String command, String payload) throws ADCommandExecutionException, InterruptedException {
            Long id = commandService.saveCommand(command, payload);
            Commands entity = this.checkCommandStatus(id);
            if (entity.getExitCode() == 0){
                return entity;
            } else {
                throw new ADCommandExecutionException(entity.getResult(), entity.getTimestamp());
            }
    }

    public ResponseEntity<String> submitJobMeta(String command, String validJson){
        try {
            return ResponseEntity.ok().body(this.executeCommand(command, validJson).getResult());
        } catch (ADCommandExecutionException e) {
            return errorHandler.createErrorResponse(e);
        } catch (InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    public ResponseEntity<String> submitJob(String command, String payload){
        try {
            jsonHandler.validateJson(payload);
            return submitJobMeta(command, payload);
        }catch (JsonProcessingException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    public ResponseEntity<String> submitJob(String command, MultiValueMap<String, Object> params){
        try {
            String json = jsonHandler.convertToJson(params);
            return submitJobMeta(command, json);
        }catch (JsonProcessingException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }





}
