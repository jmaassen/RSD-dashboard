package nl.esciencecenter.rsd;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class HighLevelMetrics {

    public int totalSoftwareEntries = 0;
    public int totalSoftwareReleases = 0;
    public int totalMentions = 0;
    public int totalStars = 0;
    public int totalForks = 0;
    public int totalIssues = 0;
    public int totalInPackageManager = 0;
    public int totalDownloads = 0;
    public int totalReverseDependencies = 0;
    public int totalContributors = 0;
    public int totalParticipatingOrganizations = 0;
    public int totalCommitActivity = 0;
    public int totalProgrammingLanguages = 0;
    public int totalSoftwareLicenses = 0;
    public int totalSoftwareMaintained = 0;

    public double averageSoftwareLifeTime = 0.0;

    public String convertToCSV() {
        StringBuilder b = new StringBuilder("metric name, value\n");
        b.append("total_software_entries, ").append(totalSoftwareEntries).append("\n");
        b.append("total_software_releases, ").append(totalSoftwareReleases).append("\n");
        b.append("total_software_maintained, ").append(totalSoftwareMaintained).append("\n");
        b.append("total_mentions, ").append(totalMentions).append("\n");
        b.append("total_stars, ").append(totalStars).append("\n");
        b.append("total_forks, ").append(totalForks).append("\n");
        b.append("total_issues, ").append(totalIssues).append("\n");
        b.append("total_in_package_manager, ").append(totalInPackageManager).append("\n");
        b.append("total_downloads, ").append(totalDownloads).append("\n");
        b.append("total_reverse_dependencies, ").append(totalReverseDependencies).append("\n");
        b.append("total_contributors, ").append(totalContributors).append("\n");
        b.append("total_participating_organizations, ").append(totalParticipatingOrganizations).append("\n");
        b.append("total_commit_activity, ").append(totalCommitActivity).append("\n");
        b.append("total_programming_languages, ").append(totalProgrammingLanguages).append("\n");
        b.append("total_software_licenses, ").append(totalSoftwareLicenses).append("\n");

        NumberFormat formatter = new DecimalFormat("#0.0");
        b.append("avg_software_lifetime, ").append(formatter.format(averageSoftwareLifeTime)).append("\n");

        return b.toString();
    }
}
