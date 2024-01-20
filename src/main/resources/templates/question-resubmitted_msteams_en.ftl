<html lang="en">
<body>
<h1>Please answer question ...</h1>

<p>
The user <#if user_firstname_lastname??>'${user_firstname_lastname}', <#else>TODO</#if> (Id: '${user_id}', Language: <#if user_language??>'${user_language}', <#else>NOT SET</#if>) has asked the following question on MS Teams (Id: 'TODO'):
</p>

<p>
${question}
</p>

<p>
<a href="${answer_question_link}">${answer_question_link}</a>
</p>
</body>
</html>
