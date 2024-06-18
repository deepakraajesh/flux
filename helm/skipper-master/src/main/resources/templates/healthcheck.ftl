<!DOCTYPE html>
<html>
<head>
    <title>Healthcheck</title>
</head>

<body>
    <div class="parent-status" style="background-image: url('');>
        <p>The status of services: </p>
        <table border="1">
            <#list healthcheck.healthCheck as configResponse>
                <tr><td> ${configResponse.service} <td> ${configResponse.code} <td> ${configResponse.response} <td> ${configResponse.healthCheckUrl}
            </#list>
        </table>
    </div>
</body>

</html>