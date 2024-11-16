package com.nortal.activedirectoryrestapi;

import com.nortal.activedirectoryrestapi.controllers.RESTApiController;
import com.nortal.activedirectoryrestapi.entities.Commands;
import com.nortal.activedirectoryrestapi.exceptions.ADCommandExecutionException;
import com.nortal.activedirectoryrestapi.services.CommandService;
import com.nortal.activedirectoryrestapi.services.CommandWorker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CommandIntegrationTest {

    @LocalServerPort
    private int port;

    @InjectMocks
    private RESTApiController restApiController;

    @Mock
    private CommandWorker commandWorker;

    @Mock
    private CommandService commandService;  // Inject CommandService to query the database

    private TestRestTemplate restTemplate;

    private String createdSamAccountName;  // Store the SamAccountName of the created user


    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    public void setUp() {
        restTemplate = new TestRestTemplate();
    }

    @Test
    @Order(1)
    public void testCreateNewUser() throws Exception {
        // Create the payload for the POST request (user creation data)
        String payload = "{"
                + "\"Name\": \"Test3 User\","
                + "\"GivenName\": \"Test3\","
                + "\"Surname\": \"User\","
                + "\"SamAccountName\": \"testuser3\","
                + "\"UserPrincipalName\": \"testuser3@domain.com\","
                + "\"Path\": \"CN=Users,DC=Domain,DC=ee\","
                + "\"Enabled\": true,"
                + "\"AccountPassword\": \"ComplexP@ssw0rd4567\""
                + "}";

        // Mock the result of the command execution
        String mockResult = "Command completed without output";
        Commands mockCommand = new Commands();
        mockCommand.setCommand("New-ADUser");
        mockCommand.setArguments(payload);
        mockCommand.setResult(mockResult);
        mockCommand.setExitCode(0);

        // Set a mock ID for the command to simulate it being saved in the database
        mockCommand.setId(1L);  // Set a mock ID value

        // Mock the execution of the command to return the mockCommand
        when(commandWorker.executeCommand("New-ADUser", payload)).thenReturn(mockCommand);

        // Mock commandService.getCommand to return the mock command when queried with its ID
        when(commandService.getCommand(mockCommand.getId())).thenReturn(mockCommand);

        // Send POST request to /users endpoint with the user creation payload
        String url = getBaseUrl() + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);  // Set correct content type for JSON
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,  // Pass entity with payload and headers
                String.class
        );

        // Assert the correct response and verify the command was processed and returned successfully
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResult, response.getBody());

        // Now verify that the command was stored in the database
        Commands savedCommand = commandService.getCommand(mockCommand.getId());

        // Verify the attributes of the saved command
        assertNotNull(savedCommand);
        assertEquals("New-ADUser", savedCommand.getCommand());
        assertEquals(payload, savedCommand.getArguments());
        assertEquals(mockResult, savedCommand.getResult());
        assertEquals(0, savedCommand.getExitCode());

        // Save the created user's SamAccountName for later deletion
    }



    @Test
    @Order(2)
    public void testUpdateUser() throws Exception {
        // Assuming the user already exists, so we will modify their data (e.g., change the surname)
        String updatePayload = "{"
                + "\"Identity\": \"testuser3\","
                + "\"GivenName\": \"Test3\","
                + "\"Surname\": \"User\","
                + "\"SamAccountName\": \"testuser3update\","
                + "\"UserPrincipalName\": \"testuser3@domain.com\","
                + "\"Enabled\": true"
                + "}";

        // Mock the result of the update command execution
        String mockResult = "Command completed without output";
        Commands mockCommand = new Commands();
        mockCommand.setCommand("Set-ADUser");
        mockCommand.setArguments(updatePayload);
        mockCommand.setResult(mockResult);
        mockCommand.setExitCode(0);

        // Set a mock ID for the command to simulate it being saved in the database
        mockCommand.setId(1L);

        // Mock the execution of the command to return the mockCommand
        when(commandWorker.executeCommand("Set-ADUser", updatePayload)).thenReturn(mockCommand);

        // Mock commandService.getCommand to return the mock command when queried with its ID
        when(commandService.getCommand(mockCommand.getId())).thenReturn(mockCommand);

        // Send PUT request to /users endpoint with the user update payload
        String url = getBaseUrl() + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(updatePayload, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,  // Pass entity with payload and headers
                String.class
        );

        // Assert the correct response and verify the command was processed and returned successfully
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResult, response.getBody());

        // Now verify that the command was stored in the database
        Commands savedCommand = commandService.getCommand(mockCommand.getId());

        // Verify the attributes of the saved command
        assertNotNull(savedCommand);
        assertEquals("Set-ADUser", savedCommand.getCommand());
        assertEquals(updatePayload, savedCommand.getArguments());
        assertEquals(mockResult, savedCommand.getResult());
        assertEquals(0, savedCommand.getExitCode());

        // Save the updated user's SamAccountName for later deletion (if necessary)
        createdSamAccountName = "testuser3update";  // Store the same SamAccountName for consistency in cleanup

        String deleteUrl = getBaseUrl() + "/users";
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
        params.add("Identity", createdSamAccountName);  // Use the Identity parameter for the filter

        HttpEntity<MultiValueMap<String, Object>> deleteEntity = new HttpEntity<>(params);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                deleteUrl,
                HttpMethod.DELETE,
                deleteEntity,
                String.class
        );

        // Assert that the user was deleted successfully
        assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
    }

    @Test
    public void testGetUsers() throws Exception {
        MultiValueMap<String, Object> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("Filter", "*");
        queryParams.add("SearchBase", "DC=Domain,DC=ee");

        // Prepare mock data for the command execution
        String mockCommand = "Get-ADUser";

        // Create a mock Commands object that mimics a successful command execution
        Commands command = new Commands();
        command.setCommand(mockCommand);
        command.setArguments(queryParams.toString());  // Storing query params as a string
        command.setExitCode(0);

        // Mock the JSON conversion to ensure that the queryParams are converted correctly
        String mockJson = "{\"Filter\":\"*\",\"SearchBase\":\"DC=Domain,DC=ee\"}";
        // Mock the service calls
        when(commandWorker.executeCommand(mockCommand, mockJson)).thenReturn(command);

        // Send GET request to /users endpoint with query parameters
        String url = getBaseUrl() + "/users?Filter=*&SearchBase=DC=Domain,DC=ee";
        ResponseEntity<String> response = restTemplate.exchange(
                url,  // Make GET request to the /users endpoint with query parameters in the URL
                HttpMethod.GET,
                new HttpEntity<>(null),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(Objects.requireNonNull(response.getBody()).contains("testuser2"));
    }


}
