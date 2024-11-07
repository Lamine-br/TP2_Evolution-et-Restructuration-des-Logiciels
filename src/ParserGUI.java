
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class ParserGUI {
    public static final String projectSourcePath = "E:\\PDC\\Visitor Pattern\\src";
    public static final String jrePath = "C:\\Program Files\\Java\\jre1.8.0_51\\lib\\rt.jar";

    private JFrame frame;
    private JTextArea outputArea;
    private JButton loadButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ParserGUI().createAndShowGUI());
    }

    public void createAndShowGUI() {
        frame = new JFrame("Call Graph Generator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        loadButton = new JButton("Load Java Files and Generate Graph");
        loadButton.addActionListener(new LoadButtonListener());

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(loadButton, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        frame.add(panel);
        frame.setVisible(true);
    }

    private class LoadButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Map<String, List<String>> callGraph = buildCallGraph();
                String dotFilePath = "call_graph.dot";
                generateDotFile(callGraph, dotFilePath);
                displayGraph(dotFilePath);
            } catch (IOException ex) {
                outputArea.append("Error: " + ex.getMessage() + "\n");
            }
        }
    }

    private Map<String, List<String>> buildCallGraph() throws IOException {
        final File folder = new File(projectSourcePath);
        ArrayList<File> javaFiles = listJavaFilesForFolder(folder);

        Map<String, List<String>> callGraph = new HashMap<>();

        for (File fileEntry : javaFiles) {
            String content = FileUtils.readFileToString(fileEntry);
            CompilationUnit parse = parse(content.toCharArray());

            CallGraphVisitor visitor = new CallGraphVisitor(callGraph);
            parse.accept(visitor);
        }
        return callGraph;
    }

    public ArrayList<File> listJavaFilesForFolder(final File folder) {
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

    private CompilationUnit parse(char[] classSource) {
        ASTParser parser = ASTParser.newParser(AST.JLS4);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setBindingsRecovery(true);

        Map options = JavaCore.getOptions();
        parser.setCompilerOptions(options);
        parser.setUnitName("");

        String[] sources = { projectSourcePath };
        String[] classpath = { jrePath };

        parser.setEnvironment(classpath, sources, new String[]{"UTF-8"}, true);
        parser.setSource(classSource);

        return (CompilationUnit) parser.createAST(null);
    }

    private class CallGraphVisitor extends ASTVisitor {
        private Map<String, List<String>> callGraph;

        public CallGraphVisitor(Map<String, List<String>> callGraph) {
            this.callGraph = callGraph;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            String methodName = node.getName().getFullyQualifiedName();
            callGraph.putIfAbsent(methodName, new ArrayList<>());
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodInvocation node) {
            String invokedMethodName = node.getName().getFullyQualifiedName();
            MethodDeclaration parentMethod = getParentMethodDeclaration(node);
            if (parentMethod != null) {
                String callingMethodName = parentMethod.getName().getFullyQualifiedName();
                callGraph.get(callingMethodName).add(invokedMethodName);
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

    private void generateDotFile(Map<String, List<String>> callGraph, String filePath) throws IOException {
        StringBuilder dotContent = new StringBuilder();
        dotContent.append("digraph CallGraph {\n");

        for (Map.Entry<String, List<String>> entry : callGraph.entrySet()) {
            String caller = entry.getKey();
            for (String callee : entry.getValue()) {
                dotContent.append("    \"" + caller + "\" -> \"" + callee + "\";\n");
            }
        }

        dotContent.append("}\n");

        // Écrire le contenu dans un fichier
        FileUtils.writeStringToFile(new File(filePath), dotContent.toString(), "UTF-8");
        outputArea.append("DOT file generated: " + filePath + "\n");
    }

    private void displayGraph(String dotFilePath) {
        try {
            // Commande pour générer le fichier image avec Graphviz
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFilePath, "-o", "call_graph.png");
            Process process = pb.start();
            process.waitFor();

            // Charge et affiche l'image dans l'interface
            JLabel imageLabel = new JLabel();
            imageLabel.setIcon(new ImageIcon("call_graph.png"));
            JFrame imageFrame = new JFrame("Call Graph");
            imageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            imageFrame.add(imageLabel);
            imageFrame.pack();
            imageFrame.setVisible(true);
        } catch (IOException | InterruptedException ex) {
            outputArea.append("Error displaying graph: " + ex.getMessage() + "\n");
        }
    }
}


