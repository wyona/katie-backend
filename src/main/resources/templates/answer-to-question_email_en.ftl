<html lang="en">
<body>
<h1>Katie's answer to your question</h1>

<p>
Your question: <strong>${question}</strong>
</p>

<#if answer_is_encrypted == 'true'>
Answer is client side encrypted. Please use the following link to decrypt answer

<p>
<a href="${answer_link}">${answer_link}</a>
</p>
<#else>
<p>Answer:</p>

<div style="border: 1px solid black; padding-left: 10px; padding-top: 10px; padding-bottom: 10px;">
${answer}
</div>

<p>
You can rate Katie's answer at:<br/><br/>
<a href="${answer_link}">${answer_link}</a>
</p>
</#if>

<p>
Thank you for using Katie!
</p>
</body>
</html>
