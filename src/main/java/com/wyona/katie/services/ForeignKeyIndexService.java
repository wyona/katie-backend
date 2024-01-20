package com.wyona.katie.services;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.KnowledgeSourceMeta;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class ForeignKeyIndexService {

    /**
     * @param key Foreign key, e.g. "23"
     * @param uuid Katie UUID, e.g. "899cae4c-be2c-4e24-a830-bb8ad0284528"
     */
    public void addForeignKey(Context domain, KnowledgeSourceMeta ksMeta, String key, String uuid) throws Exception {
        File indexDir = getIndexDirectory(domain, ksMeta);

        File keyDir = new File(indexDir, key);
        if (!keyDir.isDirectory()) {
            keyDir.mkdir();
            File uuidFile = new File(keyDir, uuid);
            uuidFile.createNewFile();
        } else {
            log.warn("Key directory '" + keyDir.getAbsolutePath() + "' already exists!");
            // TODO: Check whether key and UUID match
        }
    }

    /**
     * @param key Foreign key, e.g. "23"
     * @return Katie UUID, e.g. "899cae4c-be2c-4e24-a830-bb8ad0284528"
     */
    public String getUUID(Context domain, KnowledgeSourceMeta ksMeta, String key) {
        File indexDir = getIndexDirectory(domain, ksMeta);

        File keyDir = new File(indexDir, key);
        if (keyDir.isDirectory()) {
            String[] fileNames = keyDir.list();
            if (fileNames != null && fileNames.length == 1) {
                String uuid = fileNames[0];
                // TODO: Check whether QnA exists and if not, then return null
                // WARN: Do NOT use ContextService#existsQnA(), because ContextService is using ForeignKeyIndexService
                return uuid;
            } else {
                log.error("Key directory '" + keyDir.getAbsolutePath() + "' does not contain exactly one UUID file!");
                return null;
            }
        } else {
            log.warn("No Katie UUID for foreign key '" + key + "' of knowledge source '" + domain.getId() + " / " + ksMeta.getId() + "'!");
            return null;
        }
    }

    /**
     * Check whether foreign key exists
     * @param key Foreign key, e.g. "23"
     * @return true when foreign key exists
     */
    public boolean existsUUID(Context domain, KnowledgeSourceMeta ksMeta, String key) {
        File indexDir = getIndexDirectory(domain, ksMeta);

        File keyDir = new File(indexDir, key);
        if (keyDir.isDirectory()) {
            return true;
        } else {
            log.debug("No Katie UUID for foreign key '" + key + "' of knowledge source '" + domain.getId() + " / " + ksMeta.getId() + "'!");
            return false;
        }
    }

    /**
     * @param key Foreign key, e.g. "23"
     */
    public void deleteForeignKey(Context domain, KnowledgeSourceMeta ksMeta, String key) {
        File indexDir = getIndexDirectory(domain, ksMeta);
        File keyDir = new File(indexDir, key);
        if (keyDir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(keyDir);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Get directory containing foreign key index
     */
    private File getIndexDirectory(Context domain, KnowledgeSourceMeta ksMeta) {
        File contextDir = domain.getContextDirectory();
        File allKsDir = new File(contextDir, "knowledge-sources");
        File ksDir = new File(allKsDir, ksMeta.getId());
        File foreignKeyIndexDir = new File(ksDir, "foreign-key-index");

        if (!foreignKeyIndexDir.isDirectory()) {
            foreignKeyIndexDir.mkdirs();
        }

        return foreignKeyIndexDir;
    }
}
