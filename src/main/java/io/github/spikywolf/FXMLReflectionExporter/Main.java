package io.github.spikywolf.FXMLReflectionExporter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javafx.application.Platform;
import org.w3c.dom.*;
import tools.jackson.databind.ObjectMapper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class Main {
    static void main(String[] args) {
        if (args.length!=2){
            System.err.println("""
                    =-=-=-=-=-=-=-=
                    [ERROR] This util requires arguments to be passed.
                    [INFO] The expected arguments are "FXML Directory path" "Compiler".
                    [INFO] To use the defaults use "-d" for either argument. Note that the compiler defaults to GraalVM.
                    =-=-=-=-=-=-=-=
                    """);
            System.exit(1);
        }
        PrintStream ogErr = System.err;
        System.setErr(new PrintStream(new java.io.OutputStream(){public void write(int b){}}));
        Platform.startup(() -> {});
        System.setErr(ogErr);
        Path fxmlDir = (args[0].equals("-d")) ? Paths.get("src/main/resources") : Paths.get(args[0]);
        Path outputPath = null;
        String compiler = args[1].toLowerCase();
        if (compiler.equals("graalvm")||compiler.equals("-d")){
            outputPath = Paths.get("src/main/resources/META-INF/native-image/reflection-config.json");
        } else if (compiler.equals("gluon")) {
            outputPath = Paths.get("src/main/resources/META-INF/substrate/config/reflectionconfig.json");
        } else {
            System.err.println("[ERROR] Invalid compiler! The currently supported compilers are GraalVM and Gluon, or use \"-d\" to default to GraalVM.");
            System.exit(1);
        }
        reflectionExporter(fxmlDir, outputPath);
        System.exit(0);
    }
    public static void reflectionExporter(Path fxmlDir, Path outputPath) {
        try {
            List<Path> fxmlFileArray;
            try(Stream<Path> stream = Files.walk(fxmlDir)){fxmlFileArray = stream.toList();}
            Set<String> imports = new HashSet<>();
            Map<String, GraalClass> registry = new HashMap<>();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ObjectMapper mapper = new ObjectMapper();
            for (Path path : fxmlFileArray) {
                if (path.toString().endsWith(".fxml")) {
                    File file = new File(path.toUri());
                    Document document = builder.parse(file);
                    document.getDocumentElement().normalize();
                    NodeList rootChildren = document.getChildNodes();

                    for (int i = 0; i < rootChildren.getLength(); i++){
                        Node node = rootChildren.item(i);
                        if(node.getNodeType()==Node.PROCESSING_INSTRUCTION_NODE){
                            ProcessingInstruction prInst = (ProcessingInstruction) node;
                            if(prInst.getTarget().equals("import")){
                                imports.add(prInst.getData());
                            }
                        }
                    }
                    crawler(document.getDocumentElement(), imports, registry);
                }
            }
            List<GraalClass> payload = new ArrayList<>(registry.values());
            payload.sort(Comparator.comparing(c -> c.name));
            for (GraalClass gClass : payload) {gClass.methods.sort(Comparator.comparing(m -> m.name));}
            String prettyPayload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload).replaceAll("\\{\\s+\"name\"", "{\"name\"")
                                                                                                    .replaceAll(",\\s+\"parameterTypes\"", ", \"parameterTypes\"")
                                                                                                    .replaceAll("]\\s+},", "]},\n ")
                                                                                                    .replaceAll("\\[\\s+\\{\"name\"","[\n  {\"name\"")
                                                                                                    .replaceAll("]\\s+}\\s+", "]}\n  ")
                                                                                                    .replaceAll("\\[\\s+]","[]")
                                                                                                    .replaceAll("\\[\\s+(\".+\")\\s+]","[$1]")
                                                                                                    .replaceAll("\\s.\\{(.+\\s+.*true,\\s?)", "{\n$1")
                                                                                                    .replaceAll("]},\\n\\{","]\n},\n{")
                                                                                                    .replaceAll("\\h\\h","    ")
                                                                                                    .replaceAll("\\h\\h\\h\\h\\{","        {");
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, prettyPayload);
        } catch (Exception e) {
            throw new RuntimeException("[FxmlReflectionExporter] Fatal error during FXML parsing or JSON generation!", e);
        }
    }

    private static void crawler(Node node, Set<String> imports, Map<String, GraalClass> registry) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String className = node.getNodeName();
            NodeList children = node.getChildNodes();
            if (Character.isLowerCase(className.charAt(0)) || className.contains(".")) {
                for (int i = 0; i < children.getLength(); i++) {
                    crawler(children.item(i), imports, registry);
                }
                return;
            }
            NamedNodeMap attributes = node.getAttributes();
            Set<String> attributeSet = new HashSet<>();
            Class<?> currentClass = resolveClass(className, imports);
            Method[] classMethods;

            for (int i = 0; i < attributes.getLength(); i++) {attributeSet.add(attributes.item(i).getNodeName());}
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE){
                    String childName = child.getNodeName();
                    if (Character.isLowerCase(childName.charAt(0)) || childName.contains(".")) {
                        attributeSet.add(childName);
                    }
                }
                crawler(child, imports, registry);
            }
            if (currentClass == null) {System.out.println("[WARNING] Could not resolve class for " + className);}
            else {
                GraalClass gClass = registry.get(currentClass.getName());
                if (gClass==null){
                    gClass = new GraalClass();
                    gClass.name = currentClass.getName();
                    registry.put(gClass.name, gClass);
                }
                classMethods = currentClass.getMethods();
                for (String attr : attributeSet) {
                    matchMethods(attr, classMethods, imports, registry, gClass);
                }
            }
        }
    }
    private static void matchMethods(String attribute, Method[] classMethods, Set<String> imports, Map<String, GraalClass> registry, GraalClass gClass){
        if (attribute.contains(".")) {
            String[] split = attribute.split("\\.");
            String splitClass = split[0];
            String splitProp = split[1];
            Class<?> attributeClass = resolveClass(splitClass, imports);
            if (attributeClass != null) {
                gClass = registry.get(attributeClass.getName());
                if (gClass == null) {
                    gClass = new GraalClass();
                    gClass.name = attributeClass.getName();
                    registry.put(gClass.name, gClass);
                }
                classMethods = attributeClass.getMethods();
                matchMethods(splitProp, classMethods, imports, registry, gClass);

            }
        }
        String baseName = attribute.substring(0, 1).toUpperCase() + attribute.substring(1);
        String setter = "set" + baseName;
        String getter = "get" + baseName;
        String bool = "is" + baseName;
        String property = attribute + "Property";
        for (Method method : classMethods) {
            String mName = method.getName();
            if (mName.equals(setter)||mName.equals(getter)||mName.equals(bool)||mName.equals(property)){
                boolean exists = false;
                for (GraalMethod existing : gClass.methods) {
                    if (existing.name.equals(mName)) { exists = true; break; }
                }
                if (!exists){
                    GraalMethod gMethod = new GraalMethod();
                    gMethod.name = mName;
                    for (Class<?> param : method.getParameterTypes()){
                        gMethod.parameterTypes.add(param.getName());
                    }
                    gClass.methods.add(gMethod);
                }
            }
        }
    }
    @JsonPropertyOrder({"name", "parameterTypes"})
    static class GraalMethod {
        public String name;
        public List<String> parameterTypes = new ArrayList<>();
    }
    @JsonPropertyOrder({"name","allDeclaredConstructors" , "methods"})
    static class GraalClass {
        public String name;
        @SuppressWarnings("unused")
        public boolean allDeclaredConstructors = true;
        public List<GraalMethod> methods = new ArrayList<>();
    }

    private static Class<?> resolveClass(String className, Set<String> imports) {
        for (String imp : imports) {
            String fullPath;
            if (imp.endsWith(".*")) {
                fullPath = imp.replace(".*", "."+className);
            } else if (imp.endsWith(className)) {
                fullPath = imp;
            } else {
                continue;
            }
            try {
                return Class.forName(fullPath);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }
}