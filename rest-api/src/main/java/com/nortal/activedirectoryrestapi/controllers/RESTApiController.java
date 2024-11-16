package com.nortal.activedirectoryrestapi.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nortal.activedirectoryrestapi.exceptions.ADCommandExecutionException;
import com.nortal.activedirectoryrestapi.services.CommandWorker;
import com.nortal.activedirectoryrestapi.services.ErrorHandler;
import com.nortal.activedirectoryrestapi.services.JSONHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RESTApiController {
    private final CommandWorker commandWorker;
    private final JSONHandler jsonHandler;
    private final ErrorHandler errorHandler;


    @GetMapping("/users")
    public ResponseEntity<String> getUser(@RequestParam MultiValueMap<String, Object> queryParams){
        return commandWorker.submitJob("Get-ADUser", queryParams);
    }

    @PostMapping("/users")
    public ResponseEntity<String> newUser(@RequestBody String payload) {
        return commandWorker.submitJob("New-ADUser", payload);
    }

    @DeleteMapping("/users")
    public ResponseEntity<String> removeUser(@RequestParam MultiValueMap<String, Object> queryParams){
        queryParams.addAll("Confirm", List.of(false));
        return commandWorker.submitJob("Remove-ADUser", queryParams);
    }

    @PutMapping("/users")
    public ResponseEntity<String> updateUser(@RequestBody String payload) {
        return commandWorker.submitJob("Set-ADUser", payload);
    }

    @GetMapping("/groups")
    public ResponseEntity<String> getGroup(@RequestParam MultiValueMap<String, Object> queryParams){
        return commandWorker.submitJob("Get-ADGroup", queryParams);
    }

    @PostMapping("/groups")
    public ResponseEntity<String> newGroup(@RequestBody String payload) {
        return commandWorker.submitJob("New-ADGroup", payload);
    }

    @DeleteMapping("/groups")
    public ResponseEntity<String> removeGroup(@RequestParam MultiValueMap<String, Object> queryParams){
        queryParams.addAll("Confirm", List.of(false));
        return commandWorker.submitJob("Remove-ADGroup", queryParams);
    }

    @PatchMapping("/groups")
    public ResponseEntity<String> updateGroup(@RequestBody String payload) {
        return commandWorker.submitJob("Set-ADGroup", payload);
    }
}
