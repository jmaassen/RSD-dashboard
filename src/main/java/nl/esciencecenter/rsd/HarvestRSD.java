package nl.esciencecenter.rsd;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class HarvestRSD {

    public static final String RSD_API = "https://research-software-directory.org/api/v1";
    public static final String NLESC_UUID = "35c17f17-6b5f-4385-aa8b-6b1d33a10157";
    public static final String MENTION_QUERY = "/mention";
    public static final String RSD_ORG_STATS_QUERY = "/rpc/organisation_stats?organisation_id=%s";

    private HighLevelMetrics metrics = new HighLevelMetrics();
    private AggregatedStatistics stats = new AggregatedStatistics();
    private HashSet<String> usedMentions = new HashSet<>();

    private String get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Error fetching data from the endpoint: " + response.body());
        }
        return response.body();
    }

    private int asInt(JsonObject sw, String field) {

        JsonElement e = sw.get(field);

        if (!e.isJsonNull()) {
            return e.getAsInt();
        }

        return 0;
    }

    private void processMentions(JsonObject sw) {

        JsonElement e = sw.get("mentions");

        if (e.isJsonNull()) {
            stats.inc("software_mentions", "no mentions");
            return;
        }

        JsonArray array = e.getAsJsonArray();

        if (array.size() == 0) {
            stats.inc("software_mentions", "no mentions");
            return;
        }

        int uniqueMentions = 0;

        for (int i = 0; i < array.size(); i++) {
            JsonObject pm = array.get(i).getAsJsonObject();

            String id = pm.get("mention_id").getAsString();

            if (!usedMentions.contains(id)) {
                // Found an unused mention
                usedMentions.add(id);
                uniqueMentions++;
            }
        }

        metrics.totalMentions += uniqueMentions;
        stats.inc("software_mentions", "has mentions");
    }

    private void processPackageManagers(JsonObject sw) {

        JsonElement e = sw.get("package_managers");

        if (e.isJsonNull()) {
            stats.inc("in_package_manager", "no");
            return;
        }

        JsonArray a = e.getAsJsonArray();

        if (a.size() == 0) {
            stats.inc("in_package_manager", "no");
            return;
        }

        stats.inc("in_package_manager", "yes");

        metrics.totalInPackageManager += 1;

        for (int i = 0; i < a.size(); i++) {
            JsonObject pm = a.get(i).getAsJsonObject();
            metrics.totalDownloads += asInt(pm, "download_count");
            metrics.totalReverseDependencies += asInt(pm, "reverse_dependency_count");
            stats.inc("package_manager_type", pm.get("package_manager_type").getAsString());
        }
    }

    private void processCommitActivity(JsonObject sw) {

        JsonElement e = sw.get("commit_activity");

        if (e.isJsonNull()) {
            stats.inc("software_maintenance_status", "unknown");
            return;
        }

        Set<Entry<String, JsonElement>> elements = e.getAsJsonObject().entrySet();

        Instant now = Instant.now();

        long oneYearAgo = now.minusSeconds(60 * 60 * 24 * 356).getEpochSecond();
        long twoYearsAgo = now.minusSeconds(60 * 60 * 24 * 356 * 2).getEpochSecond();

        long activityInLastYear = 0;
        long activityInLastTwoYears = 0;

        long firstCommit = Long.MAX_VALUE;
        long lastCommit = Long.MIN_VALUE;

        for (Entry<String, JsonElement> elt : elements) {
            long timestamp = Long.parseLong(elt.getKey());
            int activity = elt.getValue().getAsInt();

            if (activity > 0) {
                metrics.totalCommitActivity += elt.getValue().getAsInt();

                if (timestamp < firstCommit) {
                    firstCommit = timestamp;
                }

                if (timestamp > lastCommit) {
                    lastCommit = timestamp;
                }

                if (timestamp >= oneYearAgo) {
                    activityInLastYear++;
                }

                if (timestamp >= twoYearsAgo) {
                    activityInLastTwoYears++;
                }
            }
        }

        if (firstCommit != Long.MAX_VALUE && lastCommit != Long.MIN_VALUE) {

            Date start = Date.from(Instant.ofEpochSecond(firstCommit));
            Date end = Date.from(Instant.ofEpochSecond(lastCommit));

            String label = "active development";

            if (firstCommit < oneYearAgo) {
                if (activityInLastTwoYears == 0) {
                    label = "not maintained";
                } else if (activityInLastTwoYears < 10) {
                    label = "maintenance only";
                }
            }

            stats.inc("software_maintenance_status", label);
            System.err.println("Lifetime " + start + " - " + end + " " + activityInLastYear + " " + activityInLastTwoYears + " " + label);
        } else {
            stats.inc("software_maintenance_status", "unknown");
            System.err.println("Lifetime UNKNOWN");
        }
    }

    private void processContributors(JsonObject sw) {

        int contributors = sw.get("contributor_cnt").getAsInt();

        if (contributors == 1) {
            stats.inc("software_contributors", "single contributor");
        } else if (contributors > 1 && contributors <= 5) {
            stats.inc("software_contributors", "small team (2-5)");
        } else if (contributors > 5 && contributors <= 20) {
            stats.inc("software_contributors", "large team (6-20)");
        } else if (contributors > 20) {
            stats.inc("software_contributors", "huge team (20+)");
        }
    }

    private void processDoi(JsonObject sw) {

        JsonElement e = sw.get("concept_doi");

        if (e.isJsonNull()) {
            stats.inc("software_doi", "not citable");
        } else {
            stats.inc("software_doi", "is citable");
        }
    }

    private void processRepository(JsonObject sw) {
        JsonElement e = sw.get("repository_url");

        if (e.isJsonNull()) {
            stats.inc("software_repository", "none");
        } else {
            String url = e.getAsString();

            if (url.startsWith("https://github.com/")) {
                stats.inc("software_repository", "github");
            } else if (url.startsWith("https://gitlab.com/")) {
                stats.inc("software_repository", "gitlab");
            } else {
                stats.inc("software_repository", "unknown");
            }
        }
    }

    private void processReleases(JsonObject sw) {
        JsonElement e = sw.get("releases");

        if (e.isJsonNull()) {
            stats.inc("software_releases", "no releases");
            return;
        }

        int releases = e.getAsJsonArray().size();

        metrics.totalSoftwareReleases += releases;

        if (releases > 0) {
            stats.inc("software_releases", "has releases");
        } else {
            stats.inc("software_releases", "no releases");
        }
    }

    private void processLanguages(JsonObject sw) {
        JsonElement e = sw.get("prog_lang");

        if (e.isJsonNull()) {
            return;
        }

        JsonArray a = e.getAsJsonArray();

        for (int i = 0; i < a.size(); i++) {
            String language = a.get(i).getAsString();
            stats.inc("language_count", language);
        }
    }

    private void processLicenses(JsonObject sw) {
        JsonElement e = sw.get("licenses");

        if (e.isJsonNull()) {
            stats.inc("software_license", "none");
            return;
        }

        JsonArray a = e.getAsJsonArray();

        for (int i = 0; i < a.size(); i++) {
            String license = a.get(i).getAsString();
            stats.inc("software_license", license);
        }

        for (int i = 0; i < a.size(); i++) {
            String license = a.get(i).getAsString();

            String abbrev = "other";

            if (license.startsWith("GPL")) {
                abbrev = "copyleft";
            } else if (license.startsWith("LGPL")) {
                abbrev = "copyleft";
            } else if (license.startsWith("AGPL")) {
                abbrev = "copyleft";
            } else if (license.startsWith("Apache")) {
                abbrev = "permissive";
            } else if (license.startsWith("BSD")) {
                abbrev = "permissive";
            } else if (license.startsWith("MIT")) {
                abbrev = "permissive";
            }

            stats.inc("software_license_type", abbrev);
        }
    }

    private void processSoftware(String jsonArray) {

        JsonArray software = JsonParser.parseString(jsonArray).getAsJsonArray();

        metrics.totalSoftwareEntries = software.size();

        for (int i = 0; i < software.size(); i++) {

            JsonObject sw = software.get(i).getAsJsonObject();

            String slug = sw.get("slug").getAsString();

            System.err.println(slug);

            metrics.totalStars += asInt(sw, "star_count");
            metrics.totalForks += asInt(sw, "fork_count");
            metrics.totalIssues += asInt(sw, "open_issue_count");

            processReleases(sw);

            processDoi(sw);

            processRepository(sw);

            processMentions(sw);

            processPackageManagers(sw);

            processCommitActivity(sw);

            processLanguages(sw);

            processLicenses(sw);

            processContributors(sw);
        }

        metrics.totalProgrammingLanguages = stats.countEntries("language_count");
        metrics.totalSoftwareLicenses = stats.countEntries("software_license");

        ArrayList<Entry<String, Integer>> lang = stats.getSorted("language_count");

        System.err.println("SIZE " + lang.size());

        int y = 0;
        int other_count = 0;

        while (y < lang.size()) {

            Entry<String, Integer> e = lang.get(y);

            String language = e.getKey();

            if (language.equals("Makefile") || language.equals("HTML") || language.equals("CSS") || language.equals("Dockerfile") || language.equals("TeX")) {
                other_count += e.getValue();
                lang.remove(y);
            } else {
                y++;
            }
        }

        int remove = lang.size() - 8;

        System.err.println("REMOVING " + remove);

        for (int x = 0; x < remove; x++) {

            Entry<String, Integer> e = lang.remove(0);

            System.err.println("REMOVED " + e.getKey() + " " + e.getValue());
            other_count += e.getValue();
        }

        for (Entry<String, Integer> e : lang) {
            System.err.println("ADDING " + e.getKey() + " " + e.getValue());
            stats.add("language_count_abbrev", e.getKey(), e.getValue());
        }

        if (other_count > 0) {
            stats.add("language_count_abbrev", "other", other_count);
        }
    }

    private void writeData(String filename, String data) {

        try {
            Writer w = new BufferedWriter(new FileWriter(filename));
            w.write(data);
            w.close();
        } catch (Exception e) {
            System.err.println("Failed to write data");
            e.printStackTrace(System.err);
        }
    }

    private void processMentions(String jsonArray) {

        JsonArray mentions = JsonParser.parseString(jsonArray).getAsJsonArray();

        for (int i = 0; i < mentions.size(); i++) {

            JsonObject sw = mentions.get(i).getAsJsonObject();

            String id = sw.get("id").getAsString();

            if (usedMentions.contains(id)) {
                // This mention has been used by software
                String type = sw.get("mention_type").getAsString();
                stats.inc("mention_types", type);

                JsonElement e = sw.get("doi");

                if (e.isJsonNull() || e.getAsString().isEmpty()) {
                    stats.inc("mention_citable", "no");
                } else {
                    stats.inc("mention_citable", "yes");
                }
            }
        }
    }

    private void retrieveMentions() {

        URI uri = URI.create(RSD_API + MENTION_QUERY);

        String result = get(uri);

        processMentions(result);
    }

    public void harvest(String orgUUID) {

        URI uri = URI.create(String.format(RSD_API + RSD_ORG_STATS_QUERY, orgUUID));

        String result = get(uri);

        processSoftware(result);

        retrieveMentions();

        writeData("data/high_level_metrics.csv", metrics.convertToCSV());
        writeData("data/aggregated_stats.csv", stats.convertToCSV());
    }

    public static void main(String[] args) {

        new HarvestRSD().harvest(NLESC_UUID);

    }

}
