package edu.hm.hafner.coverage;

import java.io.File;
import java.io.Reader;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;
import edu.hm.hafner.util.TreeStringBuilder;

/**
 * Parses a file and returns the code coverage information in a tree of {@link Node} instances.
 *
 * @author Ullrich Hafner
 */
public abstract class CoverageParser implements Serializable {
    /**
     * Defines how to handle fatal errors during parsing.
     */
    public enum ProcessingMode {
        /** All fatal errors will be ignored and logged. */
        IGNORE_ERRORS,
        /** An exception will be thrown if a fatal error is detected. */
        FAIL_FAST
    }

    private static final long serialVersionUID = 3941742254762282096L;
    private transient TreeStringBuilder treeStringBuilder = new TreeStringBuilder();

    private final ProcessingMode processingMode; // since 0.26.0

    /**
     * Creates a new instance of {@link CoverageParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    protected CoverageParser(final ProcessingMode processingMode) {
        this.processingMode = processingMode;
    }

    /**
     * Creates a new instance of {@link CoverageParser} that will fail on all errors.
     */
    protected CoverageParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Returns whether to ignore errors or to fail fast.
     *
     * @return true if errors should be ignored, false if an exception should be thrown on errors
     */
    protected boolean ignoreErrors() {
        return processingMode == ProcessingMode.IGNORE_ERRORS;
    }

    /**
     * Parses a report provided by the given reader.
     *
     * @param reader
     *         the reader with the coverage information
     * @param log
     *         the logger to write messages to
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the XML content cannot be read
     */
    public ModuleNode parse(final Reader reader, final FilteredLog log) {
        var moduleNode = parseReport(reader, log);
        getTreeStringBuilder().dedup();
        return moduleNode;
    }

    /**
     * Parses a report directory provided by the given reader.
     *
     * @param reportDirectory
     *         the report directory with the coverage information
     * @param log
     *         the logger to write messages to
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the XML content cannot be read
     */
    public ModuleNode parse(final File reportDirectory, final FilteredLog log) {
        var moduleNode = parseReportDirectory(reportDirectory, log);
        getTreeStringBuilder().dedup();
        return moduleNode;
    }


    /**
     * Called after de-serialization to restore transient fields.
     *
     * @return this
     */
    @SuppressWarnings("PMD.NullAssignment")
    protected Object readResolve() {
        treeStringBuilder = new TreeStringBuilder();

        return this;
    }

    public final TreeStringBuilder getTreeStringBuilder() {
        return treeStringBuilder;
    }

    /**
     * Parses a report provided by the given reader.
     *
     * @param reader
     *         the reader with the coverage information
     * @param log
     *         the logger to write messages to
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the XML content cannot be read
     */
    protected abstract ModuleNode parseReport(Reader reader, FilteredLog log);


    /**
     * Parses a report provided by the given reader.
     *
     * @param reportDirectory
     *         the report directory with the coverage information in its files
     * @param log
     *         the logger to write messages to
     *
     * @return the root of the created tree
     * @throws ParsingException
     *         if the XML content cannot be read
     */
    protected abstract ModuleNode parseReportDirectory(File reportDirectory, FilteredLog log);

    protected static Optional<String> getOptionalValueOf(final StartElement element, final QName attribute) {
        Attribute value = element.getAttributeByName(attribute);
        if (value == null) {
            return Optional.empty();
        }

        return Optional.of(value.getValue());
    }

    protected static int getIntegerValueOf(final StartElement element, final QName attributeName) {
        try {
            return parseInteger(getValueOf(element, attributeName));
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }

    protected static String getValueOf(final StartElement element, final QName attribute) {
        return getOptionalValueOf(element, attribute).orElseThrow(
                () -> new NoSuchElementException(String.format(
                        "Could not obtain attribute '%s' from element '%s'", attribute, element)));
    }

    protected static int parseInteger(final String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }

    protected static ParsingException createEofException() {
        return new ParsingException("Unexpected end of file");
    }
}
