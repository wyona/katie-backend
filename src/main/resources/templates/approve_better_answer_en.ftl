<#import "template-components/components.ftl" as components>

<!doctype html>
<html lang="en">
<head>
    <@components.globalHead title="Review and approve better answer"/>
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

                                        <h1 style="margin: 32px 0 0;">Review and approve better answer</h1>
                                        <p style="margin: 16px 0 0;">
                                            A user has provided a better answer to the following question <span
                                                    style="font-weight: bold">"${userquestion}"</span></p>
                                        <table role="presentation"
                                               class="btn btn-primary"
                                               style="margin: 24px 0 0; border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; box-sizing: border-box; width: 100%;">
                                            <tbody>
                                            <tr>
                                                <td style="vertical-align: top; padding-bottom: 15px;">
                                                    <table role="presentation"
                                                           style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; width: auto;">
                                                        <tbody>
                                                        <tr>
                                                            <td>
                                                                <@components.buttonFilled link="${qna_link}" text="More info"/>
                                                            </td>
                                                        </tr>
                                                        </tbody>
                                                    </table>
                                                </td>
                                            </tr>
                                            </tbody>
                                        </table>
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
