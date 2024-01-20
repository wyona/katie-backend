<#macro header>
    <header>
        <a href="${katie__base_url}">
            <img alt="Katie logo" style="width: 150px;" src="https://app.katie.qa/assets/img/katie_logo.svg">
        </a>
    </header>
</#macro>

<#macro footer>
    <footer class="footer" style="clear: both; margin-top: 10px; text-align: center; width: 100%;">
        <table role="presentation" style="border-collapse: separate; mso-table-lspace: 0; mso-table-rspace: 0; width: 100%;" width="100%">
            <tr>
                <td class="content-block" style="vertical-align: top; padding-bottom: 10px; padding-top: 10px; color: #999999; font-size: 14px; text-align: center;">
                    Replies to this email will not reach us.<#if katie__members_link??> If you don't want to receive these updates anymore, visit your <a href="${katie__members_link}">notification preferences</a>.<#else></#if>
                </td>
            </tr>
            <tr>
                <td class="content-block" style="vertical-align: top; padding-bottom: 10px; padding-top: 10px; color: #999999; font-size: 14px; text-align: center;">
                    <p>&#169; Wyona</p>
                </td>
            </tr>
        </table>
    </footer>
</#macro>

<#macro globalHead title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>${title}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500&display=swap" rel="stylesheet">
    <style>
        html, body {
            font-family: 'DM Sans', sans-serif;
            font-size: 16px;
        }

        h1 {
            font-size: 32px;
            font-weight: bold;
        }

        .button--filled {
            text-align: center;
        }

        .button--filled:hover {
            background-color: rgb(19, 102, 197) !important;
            border-color: rgb(19, 102, 197) !important;
        }

        @media only screen and (max-width: 620px) {
            h1 {
                font-size: 28px;
            }

            table.body .wrapper,
            table.body {
                padding: 0 !important;
            }

            table.body .content {
                padding: 0 !important;
            }

            table.body .container {
                padding: 0 !important;
                width: 100% !important;
            }

            table.body .main {
                padding: 16px;
                border-left-width: 0 !important;
                border-radius: 0 !important;
                border-right-width: 0 !important;
            }

            table.body .btn table {
                width: 100% !important;
            }

            table.body .btn a {
                width: 100% !important;
            }

            table.body {
                height: auto !important;
                max-width: 100% !important;
                width: auto !important;
            }
        }

        @media all {
            /* Overwrite Outlook.com’s Embedded CSS */
            .ExternalClass p,
            .ExternalClass span,
            .ExternalClass font,
            .ExternalClass td,
            .ExternalClass div {
                line-height: 100%;
            }

            /* Overwrite Samsung's Embedded CSS */
            #MessageViewBody a {
                color: inherit;
                text-decoration: none;
                font-size: inherit;
                font-weight: inherit;
                line-height: inherit;
            }
        }
    </style>
</#macro>

<#macro buttonFilled link text>
    <a href="${link}" target="_blank" class="button--filled" style="border-radius: 24px; box-sizing: border-box; cursor: pointer; display: inline-block; font-size: 16px; font-weight: bold; margin: 0; padding: 8px 24px; text-decoration: none; text-transform: capitalize; background-color: rgb(17, 115, 228); border: 1px solid rgb(17, 115, 228); color: #ffffff;">
        ${text}
    </a>
</#macro>
