/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.checkstyle;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.tools.SiteTool;
import org.apache.maven.doxia.util.DoxiaUtils;
import org.apache.maven.plugins.checkstyle.exec.CheckstyleResults;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

/**
 * Generate a report based on CheckstyleResults.
 *
 *
 */
public class CheckstyleReportRenderer extends AbstractMavenReportRenderer {
    private static final int NO_TEXT = 0;
    private static final int TEXT_SIMPLE = 1;
    private static final int TEXT_TITLE = 2;
    private static final int TEXT_ABBREV = 3;

    private final I18N i18n;

    private final Locale locale;

    private final MavenProject project;

    private final Configuration checkstyleConfig;

    private final boolean enableRulesSummary;

    private final boolean enableSeveritySummary;

    private final boolean enableFilesSummary;

    private final SiteTool siteTool;

    private String xrefLocation;

    private String xrefTestLocation;

    private List<File> testSourceDirectories = new ArrayList<>();

    private List<String> treeWalkerNames = Collections.singletonList("TreeWalker");

    private final String ruleset;

    private final CheckstyleResults results;

    public CheckstyleReportRenderer(
            Sink sink,
            I18N i18n,
            Locale locale,
            MavenProject project,
            SiteTool siteTool,
            String ruleset,
            boolean enableRulesSummary,
            boolean enableSeveritySummary,
            boolean enableFilesSummary,
            CheckstyleResults results) {
        super(sink);
        this.i18n = i18n;
        this.locale = locale;
        this.project = project;
        this.siteTool = siteTool;
        this.ruleset = ruleset;
        this.enableRulesSummary = enableRulesSummary;
        this.enableSeveritySummary = enableSeveritySummary;
        this.enableFilesSummary = enableFilesSummary;
        this.results = results;
        this.checkstyleConfig = results.getConfiguration();
    }

    @Override
    public String getTitle() {
        return getI18nString("title");
    }

    /**
     * @param key The key.
     * @return The translated string.
     */
    private String getI18nString(String key) {
        return i18n.getString("checkstyle-report", locale, "report.checkstyle." + key);
    }

    protected void renderBody() {
        startSection(getTitle());

        sink.paragraph();
        sink.text(getI18nString("checkstylelink") + " ");
        sink.link("https://checkstyle.org/");
        sink.text("Checkstyle");
        sink.link_();
        String version = getCheckstyleVersion();
        if (version != null) {
            sink.text(" ");
            sink.text(version);
        }
        sink.text(" ");
        sink.text(String.format(getI18nString("ruleset"), ruleset));
        sink.text(".");
        sink.paragraph_();

        renderSeveritySummarySection();

        renderFilesSummarySection();

        renderRulesSummarySection();

        renderDetailsSection();

        endSection();
    }

    /**
     * Get the value of the specified attribute from the Checkstyle configuration.
     * If parentConfigurations is non-null and non-empty, the parent
     * configurations are searched if the attribute cannot be found in the
     * current configuration. If the attribute is still not found, the
     * specified default value will be returned.
     *
     * @param config The current Checkstyle configuration
     * @param parentConfiguration The configuration of the parent of the current configuration
     * @param attributeName The name of the attribute
     * @param defaultValue The default value to use if the attribute cannot be found in any configuration
     * @return The value of the specified attribute
     */
    private String getConfigAttribute(
            Configuration config,
            ChainedItem<Configuration> parentConfiguration,
            String attributeName,
            String defaultValue) {
        String ret;
        try {
            ret = config.getAttribute(attributeName);
        } catch (CheckstyleException e) {
            // Try to find the attribute in a parent, if there are any
            if (parentConfiguration != null) {
                ret = getConfigAttribute(
                        parentConfiguration.value, parentConfiguration.parent, attributeName, defaultValue);
            } else {
                ret = defaultValue;
            }
        }
        return ret;
    }

    /**
     * Create the rules summary section of the report.
     *
     * @param results The results to summarize
     */
    private void renderRulesSummarySection() {
        if (!enableRulesSummary) {
            return;
        }
        if (checkstyleConfig == null) {
            return;
        }

        startSection(getI18nString("rules"));

        startTable();

        tableHeader(new String[] {
            getI18nString("rule.category"),
            getI18nString("rule"),
            getI18nString("violations"),
            getI18nString("column.severity")
        });

        // Top level should be the checker.
        if ("checker".equalsIgnoreCase(checkstyleConfig.getName())) {
            String category = null;
            for (ConfReference ref : sortConfiguration(results)) {
                renderRuleRow(ref, results, category);

                category = ref.category;
            }
        } else {
            tableRow(new String[] {getI18nString("norule")});
        }

        endTable();

        endSection();
    }

    /**
     * Create a summary for one Checkstyle rule.
     *
     * @param ref The configuration reference for the row
     * @param results The results to summarize
     * @param previousCategory The previous row's category
     */
    private void renderRuleRow(ConfReference ref, CheckstyleResults results, String previousCategory) {
        Configuration checkerConfig = ref.configuration;
        ChainedItem<Configuration> parentConfiguration = ref.parentConfiguration;
        String ruleName = checkerConfig.getName();

        sink.tableRow();

        // column 1: rule category
        sink.tableCell();
        String category = ref.category;
        if (!category.equals(previousCategory)) {
            sink.text(category);
        }
        sink.tableCell_();

        // column 2: Rule name + configured attributes
        sink.tableCell();
        if (!"extension".equals(category)) {
            sink.link("https://checkstyle.org/config_" + category + ".html#" + ruleName);
            sink.text(ruleName);
            sink.link_();
        } else {
            sink.text(ruleName);
        }

        List<String> attribnames = new ArrayList<>(Arrays.asList(checkerConfig.getAttributeNames()));
        attribnames.remove("severity"); // special value (deserves unique column)
        if (!attribnames.isEmpty()) {
            sink.list();
            for (String name : attribnames) {
                sink.listItem();

                sink.text(name);

                String value = getConfigAttribute(checkerConfig, null, name, "");
                // special case, Header.header and RegexpHeader.header
                if ("header".equals(name) && ("Header".equals(ruleName) || "RegexpHeader".equals(ruleName))) {
                    String[] lines = StringUtils.split(value, "\\n");
                    int linenum = 1;
                    for (String line : lines) {
                        sink.lineBreak();
                        sink.rawText("<span style=\"color: gray\">");
                        sink.text(linenum + ":");
                        sink.rawText("</span>");
                        sink.nonBreakingSpace();
                        sink.monospaced();
                        sink.text(line);
                        sink.monospaced_();
                        linenum++;
                    }
                } else if ("headerFile".equals(name) && "RegexpHeader".equals(ruleName)) {
                    sink.text(": ");
                    sink.monospaced();
                    sink.text("\"");
                    // Make the headerFile value relative to ${basedir}
                    String path =
                            siteTool.getRelativePath(value, project.getBasedir().getAbsolutePath());
                    sink.text(path.replace('\\', '/'));
                    sink.text(value);
                    sink.text("\"");
                    sink.monospaced_();
                } else {
                    sink.text(": ");
                    sink.monospaced();
                    sink.text("\"");
                    sink.text(value);
                    sink.text("\"");
                    sink.monospaced_();
                }
                sink.listItem_();
            }
            sink.list_();
        }

        sink.tableCell_();

        // column 3: rule violation count
        sink.tableCell();
        sink.text(String.valueOf(ref.violations));
        sink.tableCell_();

        // column 4: severity
        sink.tableCell();
        // Grab the severity from the rule configuration, this time use error as default value
        // Also pass along all parent configurations, so that we can try to find the severity there
        String severity = getConfigAttribute(checkerConfig, parentConfiguration, "severity", "error");
        iconSeverity(severity, TEXT_SIMPLE);
        sink.tableCell_();

        sink.tableRow_();
    }

    /**
     * Check if a violation matches a rule.
     *
     * @param event the violation to check
     * @param ruleName The name of the rule
     * @param expectedMessage A message that, if it's not null, will be matched to the message from the violation
     * @param expectedSeverity A severity that, if it's not null, will be matched to the severity from the violation
     * @return The number of rule violations
     */
    public boolean matchRule(AuditEvent event, String ruleName, String expectedMessage, String expectedSeverity) {
        if (!ruleName.equals(RuleUtil.getName(event))) {
            return false;
        }

        // check message too, for those that have a specific one.
        // like GenericIllegalRegexp and Regexp
        if (expectedMessage != null) {
            // event.getMessage() uses java.text.MessageFormat in its implementation.
            // Read MessageFormat Javadoc about single quote:
            // http://java.sun.com/j2se/1.4.2/docs/api/java/text/MessageFormat.html
            String msgWithoutSingleQuote = StringUtils.replace(expectedMessage, "'", "");

            if (!(expectedMessage.equals(event.getMessage()) || msgWithoutSingleQuote.equals(event.getMessage()))) {
                return false;
            }
        }
        // Check the severity. This helps to distinguish between
        // different configurations for the same rule, where each
        // configuration has a different severity, like JavadocMethod.
        // See also https://issues.apache.org/jira/browse/MCHECKSTYLE-41
        if (expectedSeverity != null) {
            if (!expectedSeverity.equals(event.getSeverityLevel().getName())) {
                return false;
            }
        }
        return true;
    }

    private void renderSeveritySummarySection() {
        if (!enableSeveritySummary) {
            return;
        }

        startSection(getI18nString("summary"));

        startTable();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text(getI18nString("files"));
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        iconSeverity("info", TEXT_TITLE);
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        iconSeverity("warning", TEXT_TITLE);
        sink.tableHeaderCell_();

        sink.tableHeaderCell();
        iconSeverity("error", TEXT_TITLE);
        sink.tableHeaderCell_();
        sink.tableRow_();

        tableRow(new String[] {
            String.valueOf(results.getFileCount()),
            String.valueOf(results.getSeverityCount(SeverityLevel.INFO)),
            String.valueOf(results.getSeverityCount(SeverityLevel.WARNING)),
            String.valueOf(results.getSeverityCount(SeverityLevel.ERROR))
        });

        endTable();

        endSection();
    }

    private void renderFilesSummarySection() {
        if (!enableFilesSummary) {
            return;
        }

        startSection(getI18nString("files"));

        startTable();

        sink.tableRow();
        sink.tableHeaderCell();
        sink.text(getI18nString("file"));
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconSeverity("info", TEXT_ABBREV);
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconSeverity("warning", TEXT_ABBREV);
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        iconSeverity("error", TEXT_ABBREV);
        sink.tableHeaderCell_();
        sink.tableRow_();

        // Sort the files before writing them to the report
        List<String> fileList = new ArrayList<>(results.getFiles().keySet());
        Collections.sort(fileList);

        for (String filename : fileList) {
            List<AuditEvent> violations = results.getFileViolations(filename);
            if (violations.isEmpty()) {
                // skip files without violations
                continue;
            }

            sink.tableRow();

            sink.tableCell();
            sink.link("#" + DoxiaUtils.encodeId(filename));
            sink.text(filename);
            sink.link_();
            sink.tableCell_();

            sink.tableCell();
            sink.text(String.valueOf(results.getSeverityCount(violations, SeverityLevel.INFO)));
            sink.tableCell_();

            sink.tableCell();
            sink.text(String.valueOf(results.getSeverityCount(violations, SeverityLevel.WARNING)));
            sink.tableCell_();

            sink.tableCell();
            sink.text(String.valueOf(results.getSeverityCount(violations, SeverityLevel.ERROR)));
            sink.tableCell_();

            sink.tableRow_();
        }

        endTable();

        endSection();
    }

    private void renderDetailsSection() {
        startSection(getI18nString("details"));

        // Sort the files before writing their details to the report
        List<String> fileList = new ArrayList<>(results.getFiles().keySet());
        Collections.sort(fileList);

        for (String file : fileList) {
            List<AuditEvent> violations = results.getFileViolations(file);

            if (violations.isEmpty()) {
                // skip files without violations
                continue;
            }

            startSection(file);

            startTable();

            tableHeader(new String[] {
                getI18nString("column.severity"),
                getI18nString("rule.category"),
                getI18nString("rule"),
                getI18nString("column.message"),
                getI18nString("column.line")
            });

            renderFileEvents(violations, file);

            endTable();

            endSection();
        }

        endSection();
    }

    private void renderFileEvents(List<AuditEvent> eventList, String filename) {
        for (AuditEvent event : eventList) {
            SeverityLevel level = event.getSeverityLevel();

            sink.tableRow();

            sink.tableCell();
            iconSeverity(level.getName(), TEXT_SIMPLE);
            sink.tableCell_();

            sink.tableCell();
            String category = RuleUtil.getCategory(event);
            if (category != null) {
                sink.text(category);
            }
            sink.tableCell_();

            sink.tableCell();
            String ruleName = RuleUtil.getName(event);
            if (ruleName != null) {
                sink.text(ruleName);
            }
            sink.tableCell_();

            sink.tableCell();
            sink.text(event.getMessage());
            sink.tableCell_();

            sink.tableCell();

            int line = event.getLine();
            String effectiveXrefLocation = getEffectiveXrefLocation(eventList);
            if (effectiveXrefLocation != null && line != 0) {
                sink.link(effectiveXrefLocation + "/" + filename.replaceAll("\\.java$", ".html") + "#L" + line);
                sink.text(String.valueOf(line));
                sink.link_();
            } else if (line != 0) {
                sink.text(String.valueOf(line));
            }
            sink.tableCell_();

            sink.tableRow_();
        }
    }

    private String getEffectiveXrefLocation(List<AuditEvent> eventList) {
        String absoluteFilename = eventList.get(0).getFileName();
        if (isTestSource(absoluteFilename)) {
            return getXrefTestLocation();
        } else {
            return getXrefLocation();
        }
    }

    private boolean isTestSource(final String absoluteFilename) {
        for (File testSourceDirectory : testSourceDirectories) {
            if (absoluteFilename.startsWith(testSourceDirectory.getAbsolutePath())) {
                return true;
            }
        }

        return false;
    }

    public String getXrefLocation() {
        return xrefLocation;
    }

    public void setXrefLocation(String xrefLocation) {
        this.xrefLocation = xrefLocation;
    }

    public String getXrefTestLocation() {
        return xrefTestLocation;
    }

    public void setXrefTestLocation(String xrefTestLocation) {
        this.xrefTestLocation = xrefTestLocation;
    }

    public void setTestSourceDirectories(List<File> testSourceDirectories) {
        this.testSourceDirectories = testSourceDirectories;
    }

    public void setTreeWalkerNames(List<String> treeWalkerNames) {
        this.treeWalkerNames = treeWalkerNames;
    }

    /**
     * Render an icon of given level with associated text.
     * @param level one of <code>INFO</code>, <code>WARNING</code> or <code>ERROR</code> constants
     * @param textType one of <code>NO_TEXT</code>, <code>TEXT_SIMPLE</code>, <code>TEXT_TITLE</code> or
     * <code>TEXT_ABBREV</code> constants
     */
    private void iconSeverity(String level, int textType) {
        sink.figureGraphics("images/icon_" + level + "_sml.gif");

        if (textType > NO_TEXT) {
            sink.nonBreakingSpace();
            String suffix;
            switch (textType) {
                case TEXT_TITLE:
                    suffix = "s";
                    break;
                case TEXT_ABBREV:
                    suffix = "s.abbrev";
                    break;
                default:
                    suffix = "";
            }
            sink.text(getI18nString(level + suffix));
        }
    }

    /**
     * Get the effective Checkstyle version at runtime.
     * @return the MANIFEST implementation version of Checkstyle API package (can be <code>null</code>)
     */
    private String getCheckstyleVersion() {
        Package checkstyleApiPackage = Configuration.class.getPackage();

        return (checkstyleApiPackage == null) ? null : checkstyleApiPackage.getImplementationVersion();
    }

    public List<ConfReference> sortConfiguration(CheckstyleResults results) {
        List<ConfReference> result = new ArrayList<>();

        sortConfiguration(result, checkstyleConfig, null, results);

        Collections.sort(result);

        return result;
    }

    private void sortConfiguration(
            List<ConfReference> result,
            Configuration config,
            ChainedItem<Configuration> parent,
            CheckstyleResults results) {
        for (Configuration childConfig : config.getChildren()) {
            String ruleName = childConfig.getName();

            if (treeWalkerNames.contains(ruleName)) {
                // special sub-case: TreeWalker is the parent of multiple rules, not an effective rule
                sortConfiguration(result, childConfig, new ChainedItem<>(config, parent), results);
            } else {
                String fixedmessage = getConfigAttribute(childConfig, null, "message", null);
                // Grab the severity from the rule configuration. Do not set default value here as
                // it breaks our rule aggregate section entirely.  The counts are off but this is
                // not appropriate fix location per MCHECKSTYLE-365.
                String configSeverity = getConfigAttribute(childConfig, null, "severity", null);

                // count rule violations
                long violations = 0;
                AuditEvent lastMatchedEvent = null;
                for (List<AuditEvent> errors : results.getFiles().values()) {
                    for (AuditEvent event : errors) {
                        if (matchRule(event, ruleName, fixedmessage, configSeverity)) {
                            lastMatchedEvent = event;
                            violations++;
                        }
                    }
                }

                if (violations > 0) // forget rules without violations
                {
                    String category = RuleUtil.getCategory(lastMatchedEvent);

                    result.add(new ConfReference(category, childConfig, parent, violations, result.size()));
                }
            }
        }
    }

    private static class ConfReference implements Comparable<ConfReference> {
        private final String category;
        private final Configuration configuration;
        private final ChainedItem<Configuration> parentConfiguration;
        private final long violations;
        private final int count;

        ConfReference(
                String category,
                Configuration configuration,
                ChainedItem<Configuration> parentConfiguration,
                long violations,
                int count) {
            this.category = category;
            this.configuration = configuration;
            this.parentConfiguration = parentConfiguration;
            this.violations = violations;
            this.count = count;
        }

        public int compareTo(ConfReference o) {
            int compare = category.compareTo(o.category);
            if (compare == 0) {
                compare = configuration.getName().compareTo(o.configuration.getName());
            }
            return (compare == 0) ? (o.count - count) : compare;
        }
    }

    private static class ChainedItem<T> {
        private final ChainedItem<T> parent;

        private final T value;

        ChainedItem(T value, ChainedItem<T> parent) {
            this.parent = parent;
            this.value = value;
        }
    }
}
