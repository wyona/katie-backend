<html lang="en">
<body>
<h1>Please answer question ...</h1>

<p>
A user (Language: <#if user_language??>'${user_language}', <#else>NOT SET</#if>) via an unknown channel (neither email nor FCM token nor Slack channel Id nor MS Teams information nor Matrix user ID) has asked the following question:
</p>

<p>
${question}
</p>

<p>
<a href="${answer_question_link}">${answer_question_link}</a>
</p>
</body>
</html>
