
import java.util.*;

public class HierarchicalClustering {
    private Map<String, Map<String, Integer>> couplingGraph;
    private Map<String, Set<String>> clusters;

    public HierarchicalClustering(Map<String, Map<String, Integer>> couplingGraph) {
        this.couplingGraph = couplingGraph;
        this.clusters = new HashMap<>();

        // Initialiser chaque classe comme un cluster individuel
        for (String className : couplingGraph.keySet()) {
            Set<String> initialCluster = new HashSet<>();
            initialCluster.add(className);
            clusters.put(className, initialCluster);
        }
    }

    public void performClustering() {
        int step = 1;
        while (clusters.size() > 1) {
            String[] closestPair = findClosestClusters();
            if (closestPair == null) break; // Si plus de couplage entre les clusters

            System.out.println("Étape " + step + ": Fusion des clusters " + closestPair[0] + " et " + closestPair[1]);

            // Fusionner les clusters et afficher l'état actuel
            mergeClusters(closestPair[0], closestPair[1]);
            printCurrentClusters();
            step++;
        }
    }

    // Trouver les deux clusters les plus couplés
    private String[] findClosestClusters() {
        String[] closestPair = null;
        int maxCoupling = Integer.MIN_VALUE;

        for (String clusterA : clusters.keySet()) {
            for (String clusterB : clusters.keySet()) {
                if (!clusterA.equals(clusterB)) {
                    int coupling = calculateCouplingBetweenClusters(clusterA, clusterB);
                    if (coupling > maxCoupling) {
                        maxCoupling = coupling;
                        closestPair = new String[] { clusterA, clusterB };
                    }
                }
            }
        }
        return closestPair;
    }

    // Calculer le couplage total entre deux clusters
    private int calculateCouplingBetweenClusters(String clusterA, String clusterB) {
        int totalCoupling = 0;
        for (String classA : clusters.get(clusterA)) {
            for (String classB : clusters.get(clusterB)) {
                totalCoupling += couplingGraph.getOrDefault(classA, new HashMap<>()).getOrDefault(classB, 0);
                totalCoupling += couplingGraph.getOrDefault(classB, new HashMap<>()).getOrDefault(classA, 0);
            }
        }
        return totalCoupling;
    }

    // Fusionner deux clusters
    private void mergeClusters(String clusterA, String clusterB) {
        Set<String> mergedCluster = new HashSet<>(clusters.get(clusterA));
        mergedCluster.addAll(clusters.get(clusterB));
        clusters.remove(clusterB);
        clusters.put(clusterA, mergedCluster);
    }

    // Afficher l'état actuel des clusters
    private void printCurrentClusters() {
        int i = 1;
        for (Set<String> cluster : clusters.values()) {
            System.out.println("  Cluster " + i + ": " + cluster);
            i++;
        }
        System.out.println("-------------");
    }

    public void printFinalClusters() {
        System.out.println("Clusters finaux après clustering hiérarchique:");
        int i = 1;
        for (Set<String> cluster : clusters.values()) {
            System.out.println("  Cluster " + i + ": " + cluster);
            i++;
        }
        System.out.println("------FIN-------\n");
    }
}
