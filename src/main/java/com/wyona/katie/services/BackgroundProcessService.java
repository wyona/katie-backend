package com.wyona.katie.services;

import com.wyona.katie.models.BackgroundProcess;
import com.wyona.katie.models.BackgroundProcessStatusType;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.print.attribute.standard.MediaSize;
import java.io.*;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BackgroundProcessService {

    @Value("${background.processes.data_path}")
    private String processesDataPath;

    @Autowired
    private XMLService xmlService;

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
     * @param id Process Id
     */
    public void stopProcess(String id) {
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
     * Get status of a running process
     * @param id Process Id
     */
    public BackgroundProcess getStatusOfRunningProcess(String id) throws Exception {
        File processStatusFile = getStatusFileOfRunningProcess(id);
        return getStatus(id, processStatusFile);
    }

    /**
     * Get status of a completed process
     * @param id Process Id
     */
    public BackgroundProcess getStatusOfCompletedProcess(String id) throws Exception {
        File processStatusFile = getStatusFileOfCompletedProcess(id);
        return getStatus(id, processStatusFile);
    }

    /**
     * Get status of a running process
     * @param id Process Id
     */
    private BackgroundProcess getStatus(String id, File processStatusFile) throws Exception {
        Document doc = xmlService.read(processStatusFile);
        BackgroundProcess process = new BackgroundProcess(id);
        Element rootEl = doc.getDocumentElement();
        process.setUserId(rootEl.getAttribute("user-id"));
        process.setDescription(rootEl.getAttribute("description"));
        NodeList statusList = rootEl.getElementsByTagName("status");
        for (int i = 0; i < statusList.getLength(); i++) {
            Element statusEl = (Element) statusList.item(i);
            String type = statusEl.getAttribute("type");
            process.addStatusDescription(type + " --- " + statusEl.getAttribute("description"));
        }
        return process;
    }
}