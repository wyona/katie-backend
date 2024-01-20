<html lang="en">
<body>
<h1>Please answer question ...</h1>

<p>
A user with the email address '${user_email}' (<#if user_firstname_lastname??>Name: '${user_firstname_lastname}', <#else></#if>Language: <#if user_language??>'${user_language}', <#else>NOT SET</#if>) has asked the following question:
</p>

<p>
${question}
</p>

<p>
<a href="${answer_question_link}">${answer_question_link}</a>
</p>
</body>
</html>
