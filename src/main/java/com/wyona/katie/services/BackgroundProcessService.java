package com.wyona.katie.services;

import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class BackgroundProcessService {

    @Value("${new.context.mail.body.host}")
    private String katieHost;

    @Value("${mail.subject.tag}")
    private String mailSubjectTag;

    @Value("${background.processes.data_path}")
    private String processesDataPath;

    @Autowired
    private XMLService xmlService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private IAMService iamService;

    private static final String NAMESPACE_1_0_0 = "http://katie.qa/background-process/1.0.0";

    /**
     * @param id Process Id
     * @param description Process description
     * @param userId User Id who started process
     */
    public void startProcess(String id, String description, String userId) {
        if (id == null) {
            log.warn("No process Id provided.");
            return;
        }
        File processStatusFile = getStatusFileOfRunningProcess(id);
        Document doc = xmlService.createDocument(NAMESPACE_1_0_0, "process");
        Element rootEl = doc.getDocumentElement();
        rootEl.setAttribute("id", id);
        rootEl.setAttribute("user-id", userId);
        rootEl.setAttribute("description", description);
        xmlService.save(doc, processStatusFile);
    }

    /**
     * @param id Process Id
     * @param statusDescription Status description
     */
    public void updateProcessStatus(String id, String statusDescription) {
        updateProcessStatus(id, statusDescription, BackgroundProcessStatusType.INFO);
    }

    /**
     * @param id Process Id
     * @param statusDescription Status description
     */
    public void updateProcessStatus(String id, String statusDescription, BackgroundProcessStatusType type) {
        if (id == null) {
            log.warn("No process Id provided.");
            return;
        }
        File processStatusFile = getStatusFileOfRunningProcess(id);
        try {
            Document doc = xmlService.read(processStatusFile);
            Element stepElement = doc.createElement("status");
            stepElement.setAttribute("description", statusDescription);
            stepElement.setAttribute("type", type.toString());
            doc.getDocumentElement().appendChild(stepElement);
            xmlService.save(doc, processStatusFile);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Stop background process
     * @param id Process Id
     * @param domainId Optional domain Id
     */
    public void stopProcess(String id, String domainId) {
        if (id == null) {
            log.warn("No process Id provided.");
            return;
        }
        File processStatusFile = getStatusFileOfRunningProcess(id);
        File archivedProcessStatusFile = getStatusFileOfCompletedProcess(id);

        try {
            Files.move(processStatusFile.toPath(), archivedProcessStatusFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        //processStatusFile.delete();

        if (true) { // TODO: Make configurable
            notifyUsersWhenErrorOccured(id, domainId);
            // DEBUG: notifyUsersWhenErrorOccured("977f8bf9-9677-4e32-b902-0ccd85bcc3cb", domainId);
        }
    }

    /**
     * Notify users when error(s) occured during the execution of a background process
     * @param id Background process Id
     * @param domainId Domain Id
     */
    private void notifyUsersWhenErrorOccured(String id, String domainId) {
        boolean errorOccured = false;

        try {
            log.info("Check background process log '" + id + "' whether errors occured ...");
            BackgroundProcess backgroundProcess = getStatus(id);
            for (BackgroundProcessStatus status : backgroundProcess.getStatusDescriptions()) {
                //log.debug("Description: " + status.getDescription());
                if (status.getType() == BackgroundProcessStatusType.ERROR) {
                    errorOccured = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (errorOccured) {
            log.warn("Error(s) occured during the execution of the background process '" + id + "'.");
            String subject = mailSubjectTag + " WARNING: Error(s) occurred during the execution of a background process";
            StringBuilder body = new StringBuilder("Error(s) occurred during the execution of the background process " + id + "");
            body.append("\n\n");
            body.append(katieHost + "/swagger-ui/#/background-process-controller/getStatusOfCompletedProcessUsingGET");

            if (domainId != null) {
                try {
                    User[] users = xmlService.getMembers(domainId, false, false);
                    for (User user : users) {
                        if (user.getDomainRole().equals(RoleDomain.ADMIN) || user.getDomainRole().equals(RoleDomain.OWNER)) {
                            user = iamService.getUserByIdWithoutAuthCheck(user.getId());
                            log.info("Notify user " + user.getFirstname() + " (" + user.getEmail()+ ") about occured error(s) ...");
                            mailerService.send(user.getEmail(), null, subject, body.toString(), false);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                mailerService.notifyAdministrator(subject, body.toString(), null, false);
            }
        } else {
            log.info("No error(s) occured during the execution of the background process '" + id + "'.");
        }
    }

    /**
     * Get file containing status of running process
     * @param id Process Id
     */
    private File getStatusFileOfRunningProcess(String id) {
        File processesDir = new File(processesDataPath);
        if (!processesDir.isDirectory()) {
            processesDir.mkdirs();
        }
        return new File(processesDir, id + ".xml");
    }

    /**
     * Get file containing status of completed process
     * @param id Process Id
     */
    private File getStatusFileOfCompletedProcess(String id) {
        File processesDir = new File(processesDataPath);
        if (!processesDir.isDirectory()) {
            processesDir.mkdirs();
        }

        File completedProcessesDir = new File(processesDir, "done");
        if (!completedProcessesDir.isDirectory()) {
            completedProcessesDir.mkdirs();
        }

        return new File(completedProcessesDir, id + ".xml");
    }

    /**
     * Get status of a background process
     * @param id Process Id
     */
    public BackgroundProcess getStatus(String id) throws Exception {
        File completedProcessStatusFile = getStatusFileOfCompletedProcess(id);
        if (completedProcessStatusFile.isFile()) {
            return getStatusLog(id, completedProcessStatusFile, "COMPLETED");
        } else {
            return getStatusLog(id, getStatusFileOfRunningProcess(id), "IN_PROGRESS");
        }
    }

    /**
     * Get status log of a running or completed background process
     * @param id Background process Id
     * @param processStatusFile File containing status information
     * @param status Process status, either "IN_PROGRESS" or "COMPLETED"
     */
    private BackgroundProcess getStatusLog(String id, File processStatusFile, String status) throws Exception {
        Document doc = xmlService.read(processStatusFile);
        BackgroundProcess process = new BackgroundProcess(id);
        Element rootEl = doc.getDocumentElement();
        process.setUserId(rootEl.getAttribute("user-id"));
        process.setDescription(rootEl.getAttribute("description"));
        NodeList statusList = rootEl.getElementsByTagName("status");
        for (int i = 0; i < statusList.getLength(); i++) {
            Element statusEl = (Element) statusList.item(i);
            BackgroundProcessStatusType type = BackgroundProcessStatusType.valueOf(statusEl.getAttribute("type"));
            String description = statusEl.getAttribute("description");
            process.addStatusDescription(new BackgroundProcessStatus(description, type));
        }
        process.setStatus(status);
        return process;
    }
}
