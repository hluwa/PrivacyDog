import com.google.gson.Gson;
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

public class PrivacyDog {

    static String androidJar = "/usr/share/android-sdk/platforms";
    static String targetPath = "/home/kali/Downloads/open_ad_sdk.aar";
    static String ruleFilePath = "privacydog.json";

    private static Rule[] rules;
    private static Logger logger = Logger.getLogger("PrivacyDog");

    private static void setupSoot(String taskPath) throws IOException {
        G.reset();
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_ignore_resolving_levels(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_validate(false);
        if(taskPath.endsWith(".apk") || taskPath.endsWith(".dex")){
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_process_multiple_dex(true);
        }

        if(taskPath.endsWith(".aar")){
            List<String> processList = new ArrayList();
            ZipFile zipFile = new ZipFile(taskPath);
            Path tempPath = Files.createTempDirectory(null);
            zipFile.extractAll(tempPath.toString());
            for(File file : Objects.requireNonNull(new File(tempPath.toString()).listFiles())){
                if(isCodeFile(file)){
                    processList.add(file.getPath());
                }
            }
            Options.v().set_process_dir(processList);
        }
        else{
            Options.v().set_process_dir(Collections.singletonList(taskPath));
        }
        Options.v().set_android_jars(androidJar);
        Options.v().set_include_all(true);
        Options.v().set_force_overwrite(true);
        Options.v().set_android_api_version(23);
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();
    }


    private static void setupRule() {
        File ruleFile = new File(ruleFilePath);
        if (!ruleFile.exists()) {
            logger.warning("Can't not found rule file，use default rules.");
            try {
                InputStream assetStream = PrivacyDog.class.getClassLoader().getResourceAsStream("privacydog.json");
                assert assetStream != null;
                rules = new Gson().fromJson(new InputStreamReader(assetStream), Rule[].class);
            }catch (Exception e){
                logger.warning("Can't read rules from assets.");
            }
            return;
        }

        try {
            rules = new Gson().fromJson(new FileReader(ruleFile), Rule[].class);
        } catch (Exception e) {
            logger.warning("parse rule fatal: " + e.toString());
        }
    }

    public static boolean isCodeFile(File file)
    {
        return file.isFile() && (file.getName().endsWith(".jar")
                || file.getName().endsWith(".apk")
                || file.getName().endsWith(".dex")
                || file.getName().endsWith(".aar"));
    }

    public static void main(String[] args) {

        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();


        options.addOption("t","target",true,"target file or dir.");
        options.addOption("s","android-sdk",true,"require a platforms path of android sdk, default is: /usr/share/android-sdk/platforms");
        options.addOption("r","rule",true,"rule file target, default is 'privacydog.json'.");


        try {
            CommandLine commandLine = parser.parse(options,args);
            if (!commandLine.hasOption("t")){
                System.err.println("Need input target file or dir with -t.");
                return;
            }
            targetPath = commandLine.getOptionValue("t");

            if(commandLine.hasOption("s")){
                androidJar = commandLine.getOptionValue("s");
            }

            if(commandLine.hasOption("r")){
                ruleFilePath = commandLine.getOptionValue("r");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (!new File(androidJar).exists() || !new File(androidJar).isDirectory()){
            System.err.println("Can't found Android SDK, Need input platforms path of android sdk with -s.");
            return;
        }


        setupRule();
        if (rules == null || rules.length == 0) {
            logger.warning("rule is empty");
            return;
        }

        File targetPath = new File(PrivacyDog.targetPath);
        if (!targetPath.exists()) {
            logger.warning("apk not exists");
            return;
        }
        List<File> files = new ArrayList<>();

        if (targetPath.isDirectory()) {
            for (File file : Objects.requireNonNull(new File(PrivacyDog.targetPath).listFiles())) {
                if (isCodeFile(file)) {
                    files.add(file);
                }
            }
        }
        else if (isCodeFile(targetPath)){
            files.add(targetPath);
        }
        for(File file:files){
            System.out.println("\n" + file.getName());

            try {
                setupSoot(file.getPath());
            }catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            PrivacyDetectionTransformer transformer = new PrivacyDetectionTransformer(rules);
            PackManager.v().getPack("jtp").add(new Transform("jtp.privacy_detection", transformer));
            PackManager.v().runPacks();

            Map<Rule, List<StmtLocation>> resultMap = transformer.getResultMap();
            for (Rule rule : resultMap.keySet()) {
                System.out.println("\t" + rule.getName());
                List<StmtLocation> locations = resultMap.get(rule);
                locations.sort(Comparator.comparing(stmtLocation -> stmtLocation.getBody().getMethod().getDeclaringClass().getName()));
                for (StmtLocation location : locations) {
                    System.out.println(String.format("\t\t%s->%s :%s",
                            location.getBody().getMethod().getDeclaringClass().getName(),
                            location.getBody().getMethod().getName(),
                            location.getStmt()));
                }
            }
        }
    }
}