package uk.gov.moj.util;


import static org.apache.commons.cli.Option.builder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLineUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineUtil.class);

    private CommandLineUtil() {
        // should not create an instance of this class
    }

    public static void parseCommandLineOptions(String[] commandLineArgs) throws MissingArgumentException {

        final Options options = getCliOptions();
        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        CommandLine commandLine = null;

        try {
            commandLine = parser.parse(options, commandLineArgs);
        } catch (final ParseException e) {
            LOGGER.error("Error parsing command line options", e);
            formatter.printHelp("Dlq message helper: ", options);
            System.exit(1);
        }

        final String artemisHost = commandLine.getOptionValue("a", null);
        final String operation = commandLine.getOptionValue("o", null);

        if (commandLine.hasOption("h")) {
            formatter.printHelp("Dlq message helper: ", options);
            System.exit(0);
        }

        setArtemisUri(artemisHost);
        setRequestType(operation);
    }

    private static Options getCliOptions() {
        final Options options = new Options();

        final Option artemisOption = builder("a").longOpt("artemisUri").required(false)
                .hasArg()
                .desc("Artemis broker host names (as comma separated values)").build();
        final Option request = builder("o").longOpt("operation").required(false)
                .hasArg()
                .desc("ex: listMessages | removeMessages | countMessages | listAndRemove")
                .build();
        final Option help = builder("h").longOpt("help").required(false).desc("Help information").build();

        options.addOption(artemisOption);
        options.addOption(request);
        options.addOption(help);
        return options;
    }

    private static void setArtemisUri(final String artemisHost) throws MissingArgumentException {
        if (System.getProperty("ARTEMIS_URI") == null) {
            if (artemisHost == null) {
                throw new MissingArgumentException("CLI option artemisUri or env variable ARTEMIS_URI is not set");
            }
            System.setProperty("ARTEMIS_URI", artemisHost);
        }
    }

    private static void setRequestType(final String requestType) throws MissingArgumentException {
        if (requestType != null) {
            System.setProperty("REQUESTED_OPERATION", requestType);
        } else {
            throw new MissingArgumentException("CLI option operation or env variable REQUESTED_OPERATION is not set");
        }
    }

}
