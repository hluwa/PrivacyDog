import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import soot.*;
import soot.options.Options;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static soot.options.Options.output_format_jimple;

public class PrivacyDog {

    static String targetPath = "";
    static String outputPath = null;
    static String ruleFilePath = "privacydog.json";

    private static Rule[] rules;
    private static final Logger logger = Logger.getLogger("PrivacyDog");

    private static void setupSoot(String taskPath) throws IOException {
        G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_allow_phantom_elms(true);
        Options.v().set_ignore_resolving_levels(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_whole_program(false);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_soot_classpath(Scene.defaultJavaClassPath());
        Options.v().set_output_format(output_format_jimple);
        Options.v().setPhaseOption("cg", "all-reachable:true");
        Options.v().setPhaseOption("jb.dae", "enabled:false");
        Options.v().setPhaseOption("jb.uce", "enabled:false");
        Options.v().setPhaseOption("jj.dae", "enabled:false");
        Options.v().setPhaseOption("jj.uce", "enabled:false");
        if (taskPath.endsWith(".apk") || taskPath.endsWith(".dex")) {
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_process_multiple_dex(true);
        }

        if (taskPath.endsWith(".aar")) {
            List<String> processList = new ArrayList<>();
            ZipFile zipFile = new ZipFile(taskPath);
            Path tempPath = Files.createTempDirectory(null);
            zipFile.extractAll(tempPath.toString());
            for (File file : Objects.requireNonNull(new File(tempPath.toString()).listFiles())) {
                if (isCodeFile(file)) {
                    processList.add(file.getPath());
                }
            }
            Options.v().set_process_dir(processList);
        } else {
            Options.v().set_process_dir(Collections.singletonList(taskPath));
        }
        Scene.v().loadNecessaryClasses();
    }


    private static void setupRule() {
        File ruleFile = new File(ruleFilePath);
        if (!ruleFile.exists()) {
            logger.warning("Unable to found rule file，Using inner default rules.");
            try {
                InputStream assetStream = PrivacyDog.class.getClassLoader().getResourceAsStream("privacydog.json");
                assert assetStream != null;
                rules = new Gson().fromJson(new InputStreamReader(assetStream), Rule[].class);
            } catch (Exception e) {
                logger.warning("Can't read rules from assets.");
            }
            return;
        }

        try {
            rules = new Gson().fromJson(new FileReader(ruleFile), Rule[].class);
        } catch (Exception e) {
            logger.warning("Parse rule fatal: " + e.toString());
        }
    }

    public static boolean isCodeFile(File file) {
        return file.isFile() && (file.getName().endsWith(".jar")
                || file.getName().endsWith(".apk")
                || file.getName().endsWith(".dex")
                || file.getName().endsWith(".aar"));
    }

    public static void main(String[] args) {

        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();


        options.addOption("t", "target", true, "target file or folder path;");
        options.addOption("r", "rule", true, "rule file path, default is 'privacydog.json';");
        options.addOption("o", "output", true, "output json to folder;");


        try {
            CommandLine commandLine = parser.parse(options, args);
            if (!commandLine.hasOption("t")) {
                System.err.println("Please input target path from '-t' option.");
                return;
            }
            targetPath = commandLine.getOptionValue("t");

            if (commandLine.hasOption("r")) {
                ruleFilePath = commandLine.getOptionValue("r");
            }

            if (commandLine.hasOption("o")) {
                String o = commandLine.getOptionValue("o");
                if (!(new File(o).exists() && new File(o).isDirectory())) {
                    System.err.println("Output need directory path;");
                    return;
                }
                outputPath = o;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        setupRule();
        if (rules == null || rules.length == 0) {
            logger.warning("The rule is empty");
            return;
        }

        File targetPath = new File(PrivacyDog.targetPath);
        if (!targetPath.exists()) {
            logger.warning("The target path is not exists");
            return;
        }
        List<File> files = new ArrayList<>();

        if (targetPath.isDirectory()) {
            for (File file : Objects.requireNonNull(new File(PrivacyDog.targetPath).listFiles())) {
                if (isCodeFile(file)) {
                    files.add(file);
                }
            }
        } else if (isCodeFile(targetPath)) {
            files.add(targetPath);
        }
        for (File file : files) {
            System.out.println("\n" + file.getName());

            try {
                setupSoot(file.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            PrivacyDetectionTransformer transformer = new PrivacyDetectionTransformer(rules);
            PackManager.v().getPack("jtp").add(new Transform("jtp.privacy_detection", transformer));
            PackManager.v().runPacks();

            Map<Rule, List<StmtLocation>> resultMap = transformer.getResultMap();
            Map<String, Map<String, List<String>>> jsonObject = new HashMap<>();
            for (Rule rule : resultMap.keySet()) {
                System.out.println("\t" + rule.getName());
                Map<String, List<String>> locationMap = new HashMap<>();
                jsonObject.put(rule.getName(), locationMap);
                List<StmtLocation> locations = resultMap.get(rule);
                locations.sort(Comparator.comparing(StmtLocation::getClassName));
                for (StmtLocation location : locations) {
                    System.out.printf("\t\t%s->%s :%s%n",
                            location.getBody().getMethod().getDeclaringClass().getName(),
                            location.getBody().getMethod().getName(),
                            location.getStmt());
                    String locationSig = String.format("%s->%s", location.getBody().getMethod().getDeclaringClass().getName(), location.getBody().getMethod().getName());
                    if (!locationMap.containsKey(locationSig)) {
                        locationMap.put(locationSig, new ArrayList<>());
                    }
                    locationMap.get(locationSig).add(location.getStmt().toString());

                }
            }
            String jsonData = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(jsonObject);
            System.out.println(jsonData);
            if (outputPath != null) {
                try {
                    FileWriter writer = new FileWriter(new File(outputPath, file.getName() + ".json"));
                    writer.write(jsonData);
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
