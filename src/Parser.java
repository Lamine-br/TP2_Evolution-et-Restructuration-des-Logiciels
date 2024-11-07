import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class Parser {
    public static final String projectPath = "E:\\PDC\\TP_PDC";
    public static final String projectSourcePath = projectPath + "\\src";
    public static final String jrePath = "C:\\Program Files\\Java\\jre1.8.0_51\\lib\\rt.jar";

    public static void main(String[] args) throws IOException, InterruptedException {
        // Lire les fichiers java
        final File folder = new File(projectSourcePath);
        ArrayList<File> javaFiles = listJavaFilesForFolder(folder);

        Map<String, List<String>> callGraph = new HashMap<>(); // Stockage du graphe d'appels
        Map<String, String> methodToClassMap = new HashMap<>(); // Association méthode-classe

        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry, "UTF-8");
            CompilationUnit parse = parse(content.toCharArray());

            // Créez une instance de CallGraphVisitor pour construire le graphe
            CallGraphVisitor visitor = new CallGraphVisitor(callGraph, methodToClassMap);
            parse.accept(visitor);
        }

        // Imprimer le graphe d'appels
        System.out.println("Graphe d'appels :");
        for (Map.Entry<String, List<String>> entry : callGraph.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        // Calculer et afficher le couplage entre deux classes spécifiques
        String classA = "Main"; // Remplacez par le nom de votre première classe
        String classB = "SimpleFile"; // Remplacez par le nom de votre deuxième classe

        // Calculer et imprimer le graphe de couplage pondéré
        CouplingCalculator calculator = new CouplingCalculator(callGraph, methodToClassMap);
        
        // Calculate coupling between the two classes
        double coupling = calculator.calculateCouplingBetweenClasses(classA, classB);
        System.out.println("\nCoupling(" + classA + ", " + classB + ") = " + coupling);
        
        Map<String, Map<String, Integer>> weightedCouplingGraph = calculator.calculateWeightedCoupling();
        calculator.printWeightedCouplingGraph(weightedCouplingGraph);
        System.out.println("\n");

        // Exécuter le clustering hiérarchique
        HierarchicalClustering clustering = new HierarchicalClustering(weightedCouplingGraph);
        clustering.performClustering();
        
        // Afficher les clusters finaux
        clustering.printFinalClusters();
        
        // Exécuter l'algorithme d'identification de modules
        ModuleIdentifier moduleIdentifier = new ModuleIdentifier(weightedCouplingGraph, weightedCouplingGraph.size(), 1);
        moduleIdentifier.identifyModules();
        
        // Afficher les modules finaux
        moduleIdentifier.printFinalModules();

        // Générer le fichier DOT pour le graphe de couplage
        String dotFilePath = projectPath + "\\coupling_graph.dot";
        calculator.generateDotFile(weightedCouplingGraph, dotFilePath);

        // Convertir le fichier DOT en image
        String imageFilePath = projectPath + "\\coupling_graph.png";
        convertDotToImage(dotFilePath, imageFilePath);

        // Afficher l'image dans une interface graphique
        SwingUtilities.invokeLater(() -> {
            CouplingGraphApp gui = new CouplingGraphApp(imageFilePath);
            gui.setVisible(true);
        });
    }

    public static ArrayList<File> listJavaFilesForFolder(final File folder) {
        ArrayList<File> javaFiles = new ArrayList<>();
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                javaFiles.addAll(listJavaFilesForFolder(fileEntry));
            } else if (fileEntry.getName().endsWith(".java")) {
                javaFiles.add(fileEntry);
            }
        }
        return javaFiles;
    }

    private static CompilationUnit parse(char[] classSource) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);

        // Set compiler options for Java 8
        Map<String, String> options = JavaCore.getOptions();
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
        parser.setCompilerOptions(options);

        parser.setUnitName("");
        String[] sources = { projectSourcePath };
        String[] classpath = { jrePath };
        parser.setEnvironment(classpath, sources, new String[] { "UTF-8" }, true);
        parser.setSource(classSource);

        return (CompilationUnit) parser.createAST(null);
    }

    private static class CallGraphVisitor extends ASTVisitor {
        private Map<String, List<String>> callGraph;
        private Map<String, String> methodToClassMap;
        private String currentClassName;

        public CallGraphVisitor(Map<String, List<String>> callGraph, Map<String, String> methodToClassMap) {
            this.callGraph = callGraph;
            this.methodToClassMap = methodToClassMap;
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            if (node.isInterface()) {
                return false;
            }
            currentClassName = node.getName().getFullyQualifiedName();
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            if (Modifier.isAbstract(node.getModifiers())) {
                return false; // Ignore abstract methods
            }
            String methodName = currentClassName + "." + node.getName().getFullyQualifiedName();
            methodToClassMap.put(methodName, currentClassName);
            callGraph.putIfAbsent(methodName, new ArrayList<>());
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodInvocation node) {
            String invokedMethodName;
            if (node.getExpression() != null) {
                invokedMethodName = node.getExpression() + "." + node.getName().getFullyQualifiedName();
            } else {
                invokedMethodName = currentClassName + "." + node.getName().getFullyQualifiedName();
            }

            MethodDeclaration parentMethod = getParentMethodDeclaration(node);
            if (parentMethod != null) {
                String callingMethodName = currentClassName + "." + parentMethod.getName().getFullyQualifiedName();

                // Associez la méthode appelée à sa classe, si ce n'est pas déjà fait
                methodToClassMap.putIfAbsent(invokedMethodName, currentClassName);

                // Récupérez la classe appelante et la classe appelée
                String callingClass = methodToClassMap.getOrDefault(callingMethodName, "Unknown");
                String calledClass = methodToClassMap.getOrDefault(invokedMethodName, "Unknown");

                // Enregistrez l'appel dans le graphe d'appels
                callGraph.computeIfAbsent(callingMethodName, k -> new ArrayList<>()).add(invokedMethodName);
            }
            return super.visit(node);
        }

        private MethodDeclaration getParentMethodDeclaration(ASTNode node) {
            ASTNode current = node.getParent();
            while (current != null && !(current instanceof MethodDeclaration)) {
                current = current.getParent();
            }
            return (MethodDeclaration) current;
        }
    }

    public static void convertDotToImage(String dotFilePath, String imageFilePath) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("dot", "-Tpng", dotFilePath, "-o", imageFilePath);
        Process process = processBuilder.start();
        process.waitFor();
    }
}
