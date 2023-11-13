package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.*;
import edu.hm.hafner.coverage.assertions.Assertions;
import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

class PitestHtmlParserTest extends AbstractParserTest {
    private static final String LOOKAHEAD_STREAM = "LookaheadStream.java";
    private static final String FILTERED_LOG = "FilteredLog.java";
    private static final String ENSURE = "Ensure.java";

    @Override
    CoverageParser createParser() {
        return new PitestHtmlParser();
    }


    @Test
    void shouldReadMutationCoverageScore() {
        ModuleNode tree = readReport("/home/devbox/coverage-model/src/test/resources/edu/hm/hafner/coverage/parser/analysis-model-pit-reports");
        assertThat(tree.getValue(MUTATION)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                c -> Assertions.assertThat(c).hasCovered(9487).hasMissed(13225 - 9487));

        assertThat(getPackageNodeByPackageName(tree, "edu.hm.hafner.analysis.parser.gendarme").getValue(MUTATION).get()).isInstanceOfSatisfying(Coverage.class,
                c -> Assertions.assertThat(c).hasCovered(92).hasMissed(116 - 92));

        assertThat(getFileNodeByPackageNameAndClassName(tree, "edu.hm.hafner.analysis.parser.ccm", "CcmParser").getValue(MUTATION).get()).isInstanceOfSatisfying(Coverage.class,
                c -> Assertions.assertThat(c).hasCovered(63).hasMissed(102 - 63));
    }




    @Test
    void shouldReadCoveredLines() {
        ModuleNode tree = readReport("/home/devbox/coverage-model/src/test/resources/edu/hm/hafner/coverage/parser/analysis-model-pit-reports");
        assertThat(getFileNodeByPackageNameAndClassName(tree, "edu.hm.hafner.analysis.parser.ccm", "CcmParser").getValue(LINE).get()).isInstanceOfSatisfying(Coverage.class,
                c -> Assertions.assertThat(c).hasCovered(52).hasMissed(55 - 52));
        assertThat(getPackageNodeByPackageName(tree, "edu.hm.hafner.analysis.parser.gendarme").getValue(LINE).get()).isInstanceOfSatisfying(Coverage.class,
                c -> Assertions.assertThat(c).hasCovered(70).hasMissed(75 - 70));

        // FIXME: find and fix reason for additional values: The display of the line coverage of the HTML report (light green, light pink)
        // contains sometimes more covered or uncovered lines than are displayed in the output in the respective Index.html.
        assertThat(tree.getValue(LINE)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                c -> Assertions.assertThat(c).hasCovered(5597).hasMissed(6641 - 5597));
    }



    @Test
    void shouldReadMutantsFromCoveredLines() {
        ModuleNode tree = readReport("/home/devbox/coverage-model/src/test/resources/edu/hm/hafner/coverage/parser/analysis-model-pit-reports");

    }

    private PackageNode getPackageNodeByPackageName(ModuleNode tree, String packageName) {
        return (PackageNode) tree.getChildren().stream()
                .filter(packageNode -> packageName.equals(packageNode.getName()))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    private FileNode getFileNodeByPackageNameAndClassName(ModuleNode tree, String packageName, String className) {
        return (FileNode) getPackageNodeByPackageName(tree, packageName)
                .getChildren().stream()
                .filter(fileNode -> (fileNode.getName()).endsWith(className))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }


}
