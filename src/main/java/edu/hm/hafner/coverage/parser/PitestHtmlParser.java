package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.*;
import edu.hm.hafner.coverage.Mutation.MutationBuilder;
import edu.hm.hafner.util.FilteredLog;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;



/**
 * A parser which parses the html reports created by PITest into a Java object model.
 *
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class PitestHtmlParser extends CoverageParser{
    private static final long serialVersionUID = -8057352377715152391L;

    @Override
    protected ModuleNode parseReportDirectory(final File reportDirectory, final FilteredLog log) {
        System.out.println("******#READ report");

        if (!reportDirectory.isDirectory()) {
            throw new IllegalArgumentException("Non-directory file was provided for parsing report-directory");
        }

        var root = new ModuleNode("-");
        parseDirectory(reportDirectory, root, "", log);
        return root;
    }

    private ModuleNode parseDirectory(File directory, ModuleNode root, String path, FilteredLog log) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                root = parseDirectory(file, root,  file.getName(), log);
            } else if (file.isFile() && file.getName().endsWith(".html")) {
                try {
                    Document doc = Jsoup.parse(file, "UTF-8");
                    if (!path.endsWith("index.html")){
                        parseHtmlFile(doc, root, path);  }
                } catch (IOException e) {
                    log.logException(e, "Can't parse file: '%s'", file.getAbsolutePath());
                }
            }
        }
        return root;
    }

    private void parseHtmlFile(Document doc, ModuleNode root, String path){


        var builder = new MutationBuilder();

        Element reportTitle = doc.selectFirst("h1");
        String fileNameWithType = reportTitle.text();
        if (fileNameWithType.equals("Pit Test Coverage Report")){
            return;
        }

        int lastDotInFilename = fileNameWithType.lastIndexOf(".");
        String fileName = fileNameWithType.substring(0, lastDotInFilename);

        builder.setSourceFile(fileName);

        Element table = doc.selectFirst("table");
        Elements rowsContainingMutations = table.select("tr:has(td:contains(Mutations)) ~ tr" );
        for ( Element rowContainingMutation : rowsContainingMutations ){
            Element lineNumber = rowContainingMutation.selectFirst("a");

            if ( lineNumber != null &&  lineNumber.hasText() ){
                builder.setLine(lineNumber.text());
                Elements mutantsOnLine = rowContainingMutation.select("p");

                for (Element mutant : mutantsOnLine){
                    builder.setMutatedClass(String.join(".", List.of(path, fileName)));

                    String description = mutant.selectFirst("br") != null ? mutant.selectFirst("br").text() : "";
                    builder.setDescription(description);

                    builder.setStatus(MutationStatus.valueOf(mutant.className()));

                    List<MutationStatus> nonSurvivingStates = List.of(MutationStatus.KILLED, MutationStatus.MEMORY_ERROR, MutationStatus.RUN_ERROR, MutationStatus.TIMED_OUT, MutationStatus.NON_VIABLE);

                    MutationStatus mutationStatusInClassName = MutationStatus.valueOf(mutant.className().toString());
                    builder.setIsDetected(nonSurvivingStates.contains(mutationStatusInClassName));
                    builder.buildAndAddToModule(root, getTreeStringBuilder());
                }

            }
        }

        var coverageBuilder = new CoverageBuilder();
        coverageBuilder.setMetric(Metric.LINE);
        Optional<PackageNode> optionalPackageNode = root.findPackage(path.replace("/", "."));
        PackageNode node = null;

        if (optionalPackageNode.isPresent()) {
            node = optionalPackageNode.get();

            Optional<FileNode> fn = node.findFile(fileName);
            FileNode fileNode = null;
            if (fn.isPresent()) {
                fileNode = fn.get();

                int covered = 0;
                int missed = 0;

                Elements rows = table.select("tr");
                for (Element row : rows) {

                    Element firstCell = row.selectFirst("td");
                    if (firstCell != null && firstCell.hasText()) {

                        int line = Integer.parseInt(firstCell.text());
                        if (firstCell.hasClass("covered")) {
                            covered++;
                            fileNode.addCounters(line, 1, 0);
                        } else if (firstCell.hasClass("uncovered")) {
                            missed++;
                            fileNode.addCounters(line, 0, 1);
                        }
                    }
                }
                fileNode.addValue(coverageBuilder.setCovered(covered).setMissed(missed).build());
            }
        }
    }

    @Override
    protected ModuleNode parseReport(Reader reader, FilteredLog log) {
        throw new UnsupportedOperationException("");
    }
}
