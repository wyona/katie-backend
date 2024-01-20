<html lang="en">
<body>
<h1>Please answer question ...</h1>

<p>
The user '<#if slack_user_name??>${slack_user_name}<#else>NOT SET</#if>' on the Slack channel (Id: '<#if slack_channel_id??>${slack_channel_id}<#else>NOT SET</#if>', Language: <#if user_language??>'${user_language}'<#else>NOT SET</#if>) has asked the following question:
</p>

<p>
${question}
</p>

<p>
<a href="${answer_question_link}">${answer_question_link}</a>
</p>
</body>
</html>
