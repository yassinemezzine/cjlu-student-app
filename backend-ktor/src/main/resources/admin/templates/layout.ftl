<#macro adminPage title bodyClass="">
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title}</title>
    <link rel="stylesheet" href="/admin/static/admin.css">
</head>
<body<#if bodyClass?has_content> class="${bodyClass}"</#if>>
<#nested>
<script src="/admin/static/admin.js" defer></script>
</body>
</html>
</#macro>
