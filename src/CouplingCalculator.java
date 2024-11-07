import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CouplingCalculator {

    private Map<String, List<String>> callGraph;
    private Map<String, String> methodToClassMap;

    public CouplingCalculator(Map<String, List<String>> callGraph, Map<String, String> methodToClassMap) {
        this.callGraph = callGraph;
        this.methodToClassMap = methodToClassMap;
    }

    // Calculate the coupling between two specific classes
    public double calculateCouplingBetweenClasses(String classA, String classB) {
        if (classA == null || classB == null) {
            throw new IllegalArgumentException("Class names cannot be null");
        }

        int relationCount = 0; // Number of relations between methods of class A and class B
        int totalRelations = 0; // Total number of binary relations between methods of any two classes

        // Count relations between classA and classB
        for (Map.Entry<String, List<String>> entry : callGraph.entrySet()) {
            String callingMethod = entry.getKey();
            String callingClass = methodToClassMap.get(callingMethod);
            List<String> calledMethods = entry.getValue();

            if (callingClass == null) {
                continue; // Skip this method if the calling class is not found
            }

            for (String calledMethod : calledMethods) {
                String calledClass = methodToClassMap.get(calledMethod);

                if (calledClass == null) {
                    continue; // Skip if called class is not found
                }

                // Count relations between classA and classB
                if (callingClass.equals(classA) && calledClass.equals(classB)) {
                    relationCount++;
                }

                // Count total relations (excluding self-calls)
                if (!callingClass.equals(calledClass)) {
                    totalRelations++;
                }
            }
        }

        // Return the coupling value
        return totalRelations > 0 ? (double) relationCount / totalRelations : 0.0;
    }

    // Calculate weighted coupling between all classes
    public Map<String, Map<String, Integer>> calculateWeightedCoupling() {
        Map<String, Map<String, Integer>> classCouplingGraph = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : callGraph.entrySet()) {
            String callingMethod = entry.getKey();
            List<String> calledMethods = entry.getValue();

            // Retrieve the calling class
            String callingClass = methodToClassMap.get(callingMethod);

            if (callingClass == null) {
                continue; // Skip if the calling class is not found
            }

            classCouplingGraph.putIfAbsent(callingClass, new HashMap<>());
            Map<String, Integer> relations = classCouplingGraph.get(callingClass);

            // For each called method
            for (String calledMethod : calledMethods) {
                String calledClass = methodToClassMap.get(calledMethod);

                if (calledClass == null || calledClass.equals(callingClass)) {
                    continue; // Ignore if called class is not found or self-calls
                }

                // Increment the relation count
                relations.put(calledClass, relations.getOrDefault(calledClass, 0) + 1);
            }
        }

        return classCouplingGraph;
    }

    // Display the weighted coupling graph
    public void printWeightedCouplingGraph(Map<String, Map<String, Integer>> couplingGraph) {
        System.out.println("\nWeighted coupling graph between all classes:");
        for (Map.Entry<String, Map<String, Integer>> entry : couplingGraph.entrySet()) {
            String callingClass = entry.getKey();
            Map<String, Integer> relations = entry.getValue();

            if (relations.isEmpty()) {
                System.out.println(callingClass + " --(0)--> No calls");
            } else {
                for (Map.Entry<String, Integer> relation : relations.entrySet()) {
                    String calledClass = relation.getKey();
                    int weight = relation.getValue();
                    System.out.println(callingClass + " --(" + weight + ")--> " + calledClass);
                }
            }
        }
    }

    // Generate the DOT file
    public void generateDotFile(Map<String, Map<String, Integer>> couplingGraph, String dotFilePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dotFilePath))) {
            writer.write("digraph G {\n");
            for (Map.Entry<String, Map<String, Integer>> entry : couplingGraph.entrySet()) {
                String callingClass = entry.getKey();
                for (Map.Entry<String, Integer> relation : entry.getValue().entrySet()) {
                    String calledClass = relation.getKey();
                    int weight = relation.getValue();
                    writer.write("    \"" + callingClass + "\" -> \"" + calledClass + "\" [label=\"" + weight + "\"];\n");
                }
            }
            writer.write("}\n");
        }
    }
}
