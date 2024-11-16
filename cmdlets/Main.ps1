# Define constants for assembly paths
$NpgsqlPath = "./npgsql/lib/net8.0/Npgsql.dll"
$LoggingAbstractionsPath = "./microsoft.extentions.logging.abstractions/lib/net8.0/Microsoft.Extensions.Logging.Abstractions.dll"

# Function to load required assemblies
function Load-Assemblies {
    try {
        Add-Type -Path $NpgsqlPath
        Add-Type -Path $LoggingAbstractionsPath
        Write-Verbose "Assemblies loaded successfully."
    } catch {
        Throw "Failed to load assemblies: $_"
    }
}

# Function to create and open a PostgreSQL connection
function Get-PostgreSQLConnection {
    param (
        [Parameter(Mandatory)]
        [string]$User,

        [Parameter(Mandatory)]
        [string]$Password,

        [Parameter(Mandatory)]
        [string]$Database,

        [Parameter(Mandatory)]
        [string]$Server,

        [int]$Port = 5432
    )

    try {
        $connString = "Host=$Server;Port=$Port;Username=$User;Password=$Password;Database=$Database"
        $conn = New-Object Npgsql.NpgsqlConnection($connString)
        $conn.Open()

        if ($conn.State -eq 'Open') {
            Write-Verbose "Connected to database '$Database' on server '$Server'."
            return $conn
        } else {
            Throw "Failed to open connection to the database."
        }
    } catch {
        Throw "Error connecting to PostgreSQL: $_"
    }
}

# Function to retrieve pending commands from the database
function Get-PendingCommands {
    param (
        [Parameter(Mandatory)]
        [Npgsql.NpgsqlConnection]$Connection
    )

    $query = "SELECT id, command, arguments FROM commands WHERE command_status = 'PENDING';"

    try {
        $cmd = $Connection.CreateCommand()
        $cmd.CommandText = $query
        $reader = $cmd.ExecuteReader()

        $data = @()
        while ($reader.Read()) {
            $data += [PSCustomObject]@{
                Id      = $reader["id"]
                Command = $reader["command"]
                Arguments = $reader["arguments"] | ConvertFrom-Json -AsHashtable
            }
        }
        $reader.Close()

        Write-Verbose "$($data.Count) pending command(s) retrieved."
        return $data
    } catch {
        Throw "Error retrieving commands: $_"
    }
}

# Function to update the status of a command
function Update-CommandStatus {
    param (
        [Parameter(Mandatory)]
        [Npgsql.NpgsqlConnection]$Connection,

        [Parameter(Mandatory)]
        [int]$CommandId,

        [Parameter(Mandatory)]
        [ValidateSet("PENDING", "PROCESSING", "COMPLETED")]
        [string]$Status,

        [string]$Result = $null,
        [int] $ExitCode = $null
    )

    try {
        $query = "UPDATE commands SET command_status = @Status"

        if ($Result -ne $null) {
            $query += ", result = @Result"
        }
        if ($ExitCode -ne $null) {
            $query += ", exit_code = @ExitCode"
        }

        $query += ", timestamp = @TimeStamp WHERE id = @CommandId;"
        $cmd = $Connection.CreateCommand()
        $cmd.CommandText = $query
        $cmd.Parameters.Add((New-Object Npgsql.NpgsqlParameter("@Status", $Status))) | Out-Null
        $cmd.Parameters.Add((New-Object Npgsql.NpgsqlParameter("@CommandId", $CommandId))) | Out-Null

        if ($Result -ne $null) {
            $cmd.Parameters.Add((New-Object Npgsql.NpgsqlParameter("@Result", $Result))) | Out-Null
        }
        if ($ExitCode -ne $null) {
            $cmd.Parameters.Add((New-Object Npgsql.NpgsqlParameter("@ExitCode", $ExitCode))) | Out-Null
        }
        $cmd.Parameters.Add((New-Object Npgsql.NpgsqlParameter("@TimeStamp", (Get-Date)))) | Out-Null


        $cmd.ExecuteNonQuery() | Out-Null
        Write-Verbose "Command ID $CommandId updated to status '$Status'."
    } catch {
        Throw "Error updating command status: $_"
    }
}


# Function to execute an AD command on a remote server
function Execute-ADCommand {
    param (
        [Parameter(Mandatory)]
        [string]$ADCommand,
        [hashtable]$Arguments,
        [Parameter(Mandatory)]
        [string]$ADServer,
        [Parameter(Mandatory)]
        [string]$ADUsername,
        [Parameter(Mandatory)]
        [string]$ADPassword
    )

    # Define AD server and credentials (Consider securing credentials)


    try {
        $securePassword = ConvertTo-SecureString $ADPassword -AsPlainText -Force
        $credential = New-Object System.Management.Automation.PSCredential ($ADUsername, $securePassword)

        $scriptBlock = {
            param($cmd, $arguments)
            $result = ""
            $exitCode = 0
            try {
                $output = & $cmd @arguments
                $result = $output
                Write-Output @($result, $exitCode)
            }
            catch {
                Write-Host "Error while executing command"
                $result = $_.Exception.Message.Trim()
                $exitCode = 1
                Write-Output @($result, $exitCode)
            }
        }

        if ($Arguments.ContainsKey('AccountPassword')) {
            $Arguments['AccountPassword'] = ConvertTo-SecureString $Arguments['AccountPassword'] -AsPlainText -Force
        }
        $invokeResult = Invoke-Command -ComputerName $ADServer -Credential $credential -ScriptBlock $scriptBlock -ArgumentList $ADCommand, $Arguments
        $result = $invokeResult[0]
        $exitCode = $invokeResult[1]
        # Convert the result to a string representation
        $resultString = if ($result) { 
            ($result | ConvertTo-Json).Trim('"')
        } else { 
            "Command completed without output" 
        }

        return @{Result = $resultString; ExitCode = $exitCode}
    } catch {
        Write-Error "Failed to execute AD command: $_"
        return $_.Exception.Message
    }
}


try {
    Load-Assemblies
    $conn = Get-PostgreSQLConnection -User "$Env:db_user" -Password "$Env:db_password" -Database "$Env:db_name" -Server "$Env:db_host" -Port "$Env:db_port"

    Write-Host "Welcome! The script will proceed with polling database for pending commands to execute"
    Write-Host "In order to quit please press 'q'..."
    Write-Host ""
    Write-Host "Listening to the database..."
    try {
        while ($true) {  
            $commands = Get-PendingCommands -Connection $conn
            if ($commands.Count -gt 0) {
                foreach ($cmd in $commands) {
                    # Update status to PROCESSING
                    Update-CommandStatus -Connection $conn -CommandId $cmd.Id -Status 'PROCESSING'

                    Write-Host "Executing command ID $($cmd.Id): $($cmd.Command)"
                    $executionResult = Execute-ADCommand -ADCommand $cmd.Command -Arguments $($cmd.Arguments) -ADServer $Env:ADServer -ADUsername $Env:ADUsername -ADPassword $Env:ADPassword

                    # Update status to DONE with result
                    Update-CommandStatus -Connection $conn -CommandId $cmd.Id -Status 'COMPLETED' -Result $executionResult.Result -ExitCode $executionResult.ExitCode

                    Write-Host "Command ID $($cmd.Id) executed and updated with result."
                }
            }
        
            # Check if the user presses 'q' to exit
            if ([console]::KeyAvailable) {
                $key = [console]::ReadKey($true)
                if ($key.Key -eq 'Q') {
                    Write-Host "Exiting..."
                    break
                }
            }
            Start-Sleep 1
        } 
    } finally {
        if ($conn.State -eq 'Open') {
            $conn.Close()
            Write-Verbose "Database connection closed."
        }
    }
} catch {
    Write-Error "An error occurred: $_"
}