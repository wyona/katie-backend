<#import "template-components/components.ftl" as components>

<!doctype html>
<html lang="en">
<head>
    <@components.globalHead title="Benchmark Results"/>
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

                                        <h1 style="margin: 32px 0 0;">Benchmark Results</h1>
                                        <p style="margin: 16px 0 0;">
                                          Dataset name: ${info.datasetName} (Reference: ${reference_info.datasetName})
                                          <br/>
                                          Dataset size: ${info.datasetSize} (Reference: ${reference_info.datasetSize})
                                        </p>
                                        <p style="margin: 16px 0 0;">
                                            Raw benchmark data <a href="${raw_data_link}">here</a>
                                            <br/>
                                            Raw reference benchmark data <a href="${raw_reference_data_link}">here</a>
                                        </p>

                                        <h2>Implementation Results</h2>
                                        <hr/>
                                        <#list results as result>
                                          <h3>${result.systemName} ${result.systemVersion}</h3>
                                          <p>${result.systemMeta}</p>
                                          <p>Accuracy: ${result.accuracy} <#if result.accuracyDeviationInPercentage lt 0><span style="color: red">WORSE</span><#else><span style="color: green">BETTER</span></#if>: ${result.accuracyDeviationInPercentage}%</p>
                                          <p>Total number of failed / tested questions: ${result.failedQuestions?size} / ${result.totalNumberOfQuestions}</p>
                                          <p>Recall: ${result.recall} <#if result.recallDeviationInPercentage lt 0><span style="color: red">WORSE</span><#else><span style="color: green">BETTER</span></#if>: ${result.recallDeviationInPercentage}%</p>
                                          <p>Precision: ${result.precision} <#if result.precisionDeviationInPercentage lt 0><span style="color: red">WORSE</span><#else><span style="color: green">BETTER</span></#if>: ${result.precisionDeviationInPercentage}%</p>
                                          <p>Indexing time: ${result.indexingTimeInSeconds} secs <#if result.indexingTimeDeviationInPercentage lt 0><span style="color: green">FASTER</span><#else><span style="color: red">SLOWER</span></#if>: ${result.indexingTimeDeviationInPercentage}%</p>
                                          <p>Inference time: ${result.inferenceTimeInSeconds} secs <#if result.inferenceTimeDeviationInPercentage lt 0><span style="color: green">FASTER</span><#else><span style="color: red">SLOWER</span></#if>: ${result.inferenceTimeDeviationInPercentage}%</p>
                                          <hr/>
                                        </#list>

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
