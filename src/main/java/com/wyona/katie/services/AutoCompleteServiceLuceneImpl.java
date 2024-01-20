package com.wyona.katie.services;

import com.wyona.katie.models.Context;
import lombok.extern.slf4j.Slf4j;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.search.suggest.InputIterator;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Arrays.asList;

import com.wyona.katie.models.Context;

import org.apache.commons.io.FileUtils;

@Slf4j
@Component
public class AutoCompleteServiceLuceneImpl implements AutoCompleteService {

    private static final String ENTRY_LIST_FILE_NAME = "autocomplete.txt";
    private static final String ENTRY_INDEX_DIR = "lucene-autocomplete";

    /**
     * @see AutoCompleteService#addEntries(Context, String[])
     */
    public void addEntries(Context domain, String[] values) throws Exception {
        List<String> pValues = new ArrayList<String>();
        for (String value : values) {
            if (value.trim().length() == 0) {
                log.warn("Entry value is empty, therefore do not add it!");
            } else {
                pValues.add(value.toLowerCase().trim());
            }
        }

        addEntities(domain, pValues.toArray(new String[0]));
        reindex(domain);
    }

    /**
     * @see AutoCompleteService#deleteEntry(Context, String)
     */
    public void deleteEntry(Context domain, String value) throws Exception {
        removeEntity(domain, value.toLowerCase().trim());
        reindex(domain);
    }

    /**
     * @see AutoCompleteService#getEntries(Context, int, int)
     */
    public String[] getEntries(Context domain, int offset, int limit) {
        List<String> entries = new ArrayList<String>();

        try {
            List<Item> items = getEntities(domain);
            // TODO: Implement offset and limit
            for (Item item : items) {
                entries.add(item.getSuggestibleText());
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return entries.toArray(new String[0]);
    }

    /**
     * @see AutoCompleteService#getSuggestions(Context, String)
     */
    @Override
    public String[] getSuggestions(Context domain, String incompleteQuestion) throws Exception {
        if (incompleteQuestion.trim().isEmpty()) {
            log.info("Incomplete question is empty.");
            return new String[0];
        }

        List<String> suggestions = new ArrayList<String>();

        Lookup suggester = getSuggester(domain);
        //log.info("The autocompletion index of domain '" + domain.getId() + "' currently contains " + suggester.getCount() + " terms.");

        List<Lookup.LookupResult> results;
        HashSet<BytesRef> contexts = new HashSet<>();
        log.info("Get suggestions for domain '" + domain.getId() + "' ...");
        contexts.add(new BytesRef(domain.getId().getBytes("UTF8")));
        contexts.add(new BytesRef("public".getBytes("UTF8")));

        try {
            results = suggester.lookup(incompleteQuestion.toLowerCase(), contexts, true, 10);

            for (final Lookup.LookupResult result : results) {
                suggestions.add("" + result.key);
                //log.debug("weight:: " + result.value + " key:: " + result.key + " payload:: " + result.payload.utf8ToString());
            }
            //suggester.close();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return suggestions.toArray(new String[0]);
    }

    /**
     * Get suggester
     * @param domain Domain object
     */
    private Lookup getSuggester(Context domain) throws Exception {

        //MMapDirectory indexDir = new MMapDirectory();
        //ByteBuffersDirectory indexDir = new ByteBuffersDirectory();
        Path indexDirPath = getIndexDirPath(domain);
        Directory indexDir = FSDirectory.open(indexDirPath);

        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();

        AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(indexDir, analyzer, analyzer, 3, true);

        if (!indexDirPath.toFile().isDirectory() || indexDirPath.toFile().list().length == 0) {
            log.info("Autocomplete index does not exist yet, therefore let's build the index '" + indexDirPath + "' ...");

            suggester.build(new ItemIterator(getEntities(domain).iterator()));
        }

        return suggester;
    }

    /**
     *
     */
    private List<Item> getEntities(Context domain) throws Exception {
        List<Item> entities = new ArrayList<Item>();

        File autocompleteList = new File(domain.getContextDirectory(), ENTRY_LIST_FILE_NAME);
        if (autocompleteList.isFile()) {
            FileInputStream fis = new FileInputStream(autocompleteList.getAbsoluteFile());
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while((line = br.readLine()) != null) {
                entities.add(new Item(line, "", asList("public", domain.getId()), 1));
            }
            br.close();
            fis.close();
        } else {
            log.info("No custom autocomplete list exists, therefore load default list ...");
            entities.add(new Item("katie", "", asList("public", domain.getId()), 1));
        }

        return entities;
    }

    /**
     *
     */
    private void addEntities(Context domain, String[] values) throws Exception {
        log.info("Add " + values.length + " entries to autocompletion index of domain '" + domain.getId() + "' ...");

        List<String> entries = new ArrayList<String>();

        // INFO: Read current index
        File autocompleteList = new File(domain.getContextDirectory(), ENTRY_LIST_FILE_NAME);
        if (autocompleteList.isFile()) {
            FileInputStream fis = new FileInputStream(autocompleteList.getAbsoluteFile());
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while((line = br.readLine()) != null) {
                entries.add(line);
            }
            fis.close();
            br.close();
        } else {
            log.info("No autocompletion file exists yet, let's create one ...");
        }

        // INFO: Add new entry
        for (int i = 0; i < values.length; i++) {
            boolean alreadyExists = false;
            for (String entry : entries) {
                if (entry.equals(values[i])) {
                    alreadyExists = true;
                }
            }
            if (alreadyExists) {
                log.info("Entry '" + values[i] + "' already exists, therefore do not add to index.");
            } else {
                entries.add(values[i]);
            }
        }

        // INFO: Sort alphabetically
        Collections.sort(entries);

        // INFO: Save new index
        log.info("Save autocompletion list at: " + autocompleteList.getAbsolutePath());
        FileOutputStream out = new FileOutputStream(autocompleteList);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        for (String entry : entries) {
            bw.write(entry);
            bw.newLine();
        }
        bw.close();
        out.close();
    }

    /**
     *
     */
    private void removeEntity(Context domain, String value) throws Exception {
        log.info("Remove entry '" + value + "' from autocompletion index of domain '" + domain.getId() + "' ...");
        List<String> entries = new ArrayList<String>();

        File autocompleteList = new File(domain.getContextDirectory(), ENTRY_LIST_FILE_NAME);
        if (autocompleteList.isFile()) {
            FileInputStream fis = new FileInputStream(autocompleteList.getAbsoluteFile());
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;
            while((line = br.readLine()) != null) {
                if (line.equals(value)) {
                    log.info("Remove entry '" + value + "'.");
                } else {
                    entries.add(line);
                }
            }
            fis.close();
            br.close();
        } else {
            log.info("No autocompletion file exists yet, let's create one ...");
        }

        log.info("Save autocompletion list at: " + autocompleteList.getAbsolutePath());
        FileOutputStream out = new FileOutputStream(autocompleteList);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        for (String entry : entries) {
            bw.write(entry);
            bw.newLine();
        }
        bw.close();
        out.close();
    }

    /**
     * Get directory path containing Lucene autocomplete index
     * @param domain Domain associated with Lucene autocomplete index
     * @return absolute directory path containing Lucene autocomplete index
     */
    private Path getIndexDirPath(Context domain) {
        File indexDir = new File(domain.getContextDirectory(), ENTRY_INDEX_DIR);
        if (!indexDir.isDirectory()) {
            indexDir.mkdirs();
        }
        return Paths.get(indexDir.getAbsolutePath());
    }

    /**
     *
     */
    private void reindex(Context domain) throws Exception {
        // TODO: Check whether there is a better way to reindex than just deleting the index directory

        File indexDir = new File(domain.getContextDirectory(), ENTRY_INDEX_DIR);
        if (indexDir.isDirectory()) {
            FileUtils.deleteDirectory(indexDir);
            // INFO: Index will be re-created automatically when getSuggester() is called
        }
    }
}

/**
 *
 */
class Item {
    private final String suggestibleText;
    private final String arbitraryData; // additional arbitrary data you want to store in the index
    /**
     * These are data you can use for additional filtering
     */
    private final Collection<String> contexts;
    /*
     * order suggestion results. Higher weight are returned first.
     */
    private final int weight;

    /**
     * @param suggestibleText Text suggestion, e.g. "deckungsprüfung" or "deckung"
     * @param arbitraryData TODO
     * @param contexts Collection of contexts / domains which are allowed to access the suggested text
     * @param weight TODO
     */
    public Item(String suggestibleText, String arbitraryData, Collection<String> contexts, int weight) {
        this.suggestibleText = suggestibleText;
        this.arbitraryData = arbitraryData;
        this.contexts = contexts;
        this.weight = weight;
    }

    /**
     *
     */
    public String getSuggestibleText() {
        return suggestibleText;
    }

    /**
     * TODO
     */
    public String getArbitraryData() { return arbitraryData; }

    /**
     *
     */
    public Collection<String> getContexts() {
        return contexts;
    }

    /**
     *
     */
    public int getWeight() {
        return weight;
    }
}

/**
 *
 */
@Slf4j
class ItemIterator implements InputIterator {

    private final Iterator<Item> entityIterator;
    private Item currentItem;

    public ItemIterator(final Iterator<Item> entityIterator) {
        this.entityIterator = entityIterator;
    }

    @Override
    public boolean hasContexts() {
        return true;
    }

    @Override
    public boolean hasPayloads() {
        return true;
    }

    @Override
    public BytesRef next() {
        if (entityIterator.hasNext()) {
            currentItem = entityIterator.next();
            try {
                return new BytesRef(currentItem.getSuggestibleText().getBytes("UTF8"));
            } catch (final UnsupportedEncodingException e) {
                throw new java.lang.Error("Couldn't convert to UTF-8");
            }
        } else { // returning null is fine for lucene...
            return null;
        }
    }

    @Override
    public BytesRef payload() { // returns null if no payload from Item
        try {
            return new BytesRef(currentItem.getArbitraryData().getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            throw new java.lang.Error("Could not convert to UTF-8");
        }
    }

    @Override
    public Set<BytesRef> contexts() { // returns null if no context from Item
        try {
            final Set<BytesRef> contexts = new HashSet<>();
            if (currentItem.getContexts() != null) {
                for (final String context : currentItem.getContexts()) {
                    contexts.add(new BytesRef(context.getBytes("UTF8")));
                }
            } else {
                log.warn("No contexts set.");
            }
            return contexts;
        } catch (final UnsupportedEncodingException e) {
            throw new java.lang.Error("Couldn't convert to UTF-8");
        }
    }

    @Override
    public long weight() {
        return currentItem.getWeight();
    }
}
