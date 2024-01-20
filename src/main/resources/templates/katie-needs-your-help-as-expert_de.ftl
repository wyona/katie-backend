<#import "template-components/components.ftl" as components>

<!doctype html>
<html lang="de">
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

                                        <h1 style="margin: 32px 0 0;">Katie Report für die Domain '${katie__domain.name}'</h1>

                                        <p>Hallo ${user_firstname_lastname},</p>
                                        <p>Hier die Einsichten der letzten ${last_number_of_days} Tage:</p>

                                        <p>Anzahl der gestellten Fragen: ${insights.numberofaskedquestions}</p>

                                        <#if insights.numberofaskedquestionswithoutanswer gt 0>
                                            <p>Bei ${insights.numberofaskedquestionswithoutanswer} Frage(n) wusste Katie keine Antwort. Die Fragen ohne Antwort finden Sie <a href="${katie__base_url}/#/asked-questions/${katie__domain.id}">hier</a>. Sie können Katie helfen sich zu verbessern indem Sie entsprechende QnAs <a href="${katie__base_url}/#/domain/${katie__domain.id}/qna">hinzufügen</a>.</p>
                                        </#if>

                                        <#if faq_pageviews?size gt 0>
                                            <p>Anzahl FAQ Pageviews:</p>
                                            <ul>
                                                <#list faq_pageviews as pv>
                                                    <li>
                                                        ${pv.pageviews} (${pv.language})
                                                    </li>
                                                </#list>
                                            </ul>
                                        </#if>

                                        <p>Weitere Einsichten finden Sie <a href="${katie__base_url}/#/domain/${katie__domain.id}/insights">hier</a>.</p>

                                        <#if pqs?size gt 0 || qas?size gt 0>
                                            <p>Katie benötigt Ihre Unterstützung als Experte.</p>
                                        </#if>

                                        <#if pqs?size gt 0>
                                            <p>Bitte beantworten Sie die folgenden ${pqs?size} noch unbeantworteten Fragen:</p>

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
                                                Bitte lesen und beurteilen Sie die folgenden ${qas?size} Fragen und Antworten, um Katie zu helfen, die bestmöglichen Antworten zu geben.
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
                                        <p style="margin: 16px 0 0;">Herzlichen Dank, <br/>Team Katie</p>
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
