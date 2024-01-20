<html lang="de">
<body>
<h1>Katies Antwort auf Ihre Frage</h1>

<p>
Ihre Frage: <strong>${question}</strong>
</p>

<#if answer_is_encrypted == 'true'>
Antwort ist clientseitig verschlüsselt. Bitte verwenden Sie den folgenden Link, um die Antwort zu entschlüsseln

<p>
<a href="${answer_link}">${answer_link}</a>
</p>
<#else>
<p>Antwort:</p>

<div style="border: 1px solid black; padding-left: 10px; padding-top: 10px; padding-bottom: 10px;">
${answer}
</div>

<p>
Sie können Katies Antwort bewerten unter:<br/><br/>
<a href="${answer_link}">${answer_link}</a>
</p>
</#if>

<p>
Danke für die Verwendung von Katie!
</p>
</body>
</html>
