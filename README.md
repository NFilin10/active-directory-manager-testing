# Active Directory Manager

A software component designed to send queries to the Active Directory and retrieve the responses.

---

## How to Use the Application

### 1. PostgreSQL Database Setup
Ensure that a PostgreSQL database is running.

### 2. Set Environment Variables
Before starting the application, set the following environment variables. Make sure to update your database credentials.

```bash
db_user=adm
db_password=password
db_host=localhost
db_name=active_directory_commands
db_port=5432
```
### 3. Start the Spring Boot Application
Run the Spring Boot application.

### 4. Build Docker Image
Go to the `cmdlets` folder and build the Docker image using the following command (`docker` is substitutable with `podman` in this and the following steps):

```bash
docker build -t adm .
```

### 5. Run the Docker Container
Make sure that you are executing this step only when necessary table is already created by Spring App. Otherwise the program will crash.
Run the container, which will execute the added command with the following command:

```bash
docker run -it --rm \
-e ADServer="server-ip" \
-e ADUsername="user" \
-e ADPassword="password" \
-e db_user="adm" \
-e db_password="password" \
-e db_host="localhost" \
-e db_name="active_directory_commands" \
-e db_port="5432" \
--network host adm
```
**NB!** Do not forget to change your database and Windows Server (AD) credentials. Also if you are using Docker Desktop do following steps: 
1. Sign in to your Docker account in Docker Desktop.
2. Navigate to Settings.
3. Under the Resource tab, select Network.
4. Check the Enable host networking option.
5. Select Apply and restart.

### 6. Access Swagger UI
You can access the Swagger UI at:

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### 7. Provide Payload for POST Request

- **Example for Adding a New User:**

```json
{
  "command": "New-ADUser -Name 'Alice Johnson' -GivenName 'Alice' -Surname 'Johnson' -SamAccountName 'ajohnson012' -UserPrincipalName 'ajohnnson02@domain.com' -Path 'CN=Users,DC=Domain,DC=ee' -AccountPassword (ConvertTo-SecureString 'ComplexP@ssw0rd4567' -AsPlainText -Force) -Enabled $true"
}
```

- **Example for getting all users:**

```json
{"command":"Get-ADUser -Filter * -SearchBase \"DC=Domain,DC=ee\""}
```


### 8. View Command Output
After command execution, you will see the command output in the Swagger UI response.
