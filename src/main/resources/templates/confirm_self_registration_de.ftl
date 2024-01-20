<#import "template-components/components.ftl" as components>

<!doctype html>
<html lang="de">
<head>
    <@components.globalHead title="Bitte bestätigen Sie Ihre E-Mail Adresse"/>
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

                                        <h1 style="margin: 32px 0 0;">Willkommen bei Katie!</h1>
                                        <p style="margin: 16px 0 0;">
                                        Vielen Dank, dass Du Dich bei Katie registriert hast, ${firstname}.
                                        </p>
                                        <p style="margin: 16px 0 0;">
                                        Bitte bestätige Deine E-Mail Adresse, um Dein Account-Setup abzuschliessen:  <a href="${katie__base_url}/#/confirm-registration?token=${token}">${katie__base_url}/#/confirm-registration</a>
                                        </p>
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
