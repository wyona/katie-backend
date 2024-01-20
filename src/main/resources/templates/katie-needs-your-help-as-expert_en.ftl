<#import "template-components/components.ftl" as components>

<!doctype html>
<html lang="en">
<head>
    <@components.globalHead title="Katies insights"/>
</head>
<body style="background-color: #f6f6f6; -webkit-font-smoothing: antialiased; margin: 0; padding: 0; -ms-text-size-adjust: 100%; -webkit-text-size-adjust: 100%;">
<!--<span class="preheader" style="color: transparent; display: none; height: 0; max-height: 0; max-width: 0; opacity: 0; overflow: hidden; mso-hide: all; visibility: hidden; width: 0;">This is preheader text. Some clients will show this text as a preview.</span>-->
<table role="presentation" class="body"
       style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; background-color: #f6f6f6; width: 100%;">
    <tr>
        <td class="container"
            style="vertical-align: top; display: block; max-width: 580px; padding: 10px; width: 580px; margin: 0 auto;">
            <div class="content"
                 style="box-sizing: border-box; display: block; margin: 0 auto; max-width: 580px; padding: 10px;">

                <!-- START CENTERED WHITE CONTAINER -->
                <table role="presentation" class="main"
                       style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; background: #ffffff; border-radius: 3px; width: 100%;"
                       width="100%">

                    <!-- START MAIN CONTENT AREA -->
                    <tr>
                        <td class="wrapper" style="vertical-align: top; box-sizing: border-box; padding: 48px;">
                            <table role="presentation"
                                   style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; width: 100%;">
                                <tr>
                                    <td style="vertical-align: top;">

                                        <!-- HEADER -->
                                        <@components.header/>

                                        <h1 style="margin: 32px 0 0;">Katie Report for the Domain '${katie__domain.name}'</h1>

                                        <p>Hello ${user_firstname_lastname},</p>
                                        <p>Here the insights of the last ${last_number_of_days} days:</p>

                                        <p>Number of asked questions: ${insights.numberofaskedquestions}</p>

                                        <#if insights.numberofaskedquestionswithoutanswer gt 0>
                                            <p>Katie did not know the answer to ${insights.numberofaskedquestionswithoutanswer} question(s). You can find the questions without an answer <a href="${katie__base_url}/#/asked-questions/${katie__domain.id}">here</a>. You can help Katie improve by <a href="${katie__base_url}/#/domain/${katie__domain.id}/qna">adding</a> appropriate QnAs.</p>
                                        </#if>

                                        <#if faq_pageviews?size gt 0>
                                            <p>Number of FAQ Pageviews:</p>
                                            <ul>
                                                <#list faq_pageviews as pv>
                                                    <li>
                                                        ${pv.pageviews} (${pv.language})
                                                    </li>
                                                </#list>
                                            </ul>
                                        </#if>

                                        <p>Please find more insights <a href="${katie__base_url}/#/domain/${katie__domain.id}/insights">here</a>.</p>

                                        <#if pqs?size gt 0 || qas?size gt 0>
                                            <p>Katie needs your help as expert.</p>
                                        </#if>

                                        <#if pqs?size gt 0>
                                            <p>Please answer the following ${pqs?size} pending questions below:</p>

                                            <ul>
                                                <#list pqs as pq>
                                                    <li>
                                                        <a href="${katie__base_url}/#/answer-question?uuid=${pq.uuid}">${pq.question}</a>
                                                    </li>
                                                </#list>
                                            </ul>

                                            <hr/>
                                        </#if>
                                        <#if qas?size gt 0>
                                            <p>
                                                Please review and rate the following ${qas?size} questions and answers below in order to help Katie to provide the best possible answers.
                                            </p>

                                            <ul>
                                                <#list qas as qa>
                                                    <li>
                                                        <a href="${read_answer_url}?domain-id=${katie__domain.id}&uuid=${qa.uuid}">${qa.question}</a>
                                                    </li>
                                                </#list>
                                            </ul>

                                            <hr/>
                                        </#if>
                                        <p style="margin: 16px 0 0;">Thanks, <br/>Team Katie</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    <!-- END MAIN CONTENT AREA -->
                </table>
                <!-- END CENTERED WHITE CONTAINER -->

                <!-- FOOTER -->
                <@components.footer/>
            </div>
        </td>
    </tr>
</table>
</body>
</html>

