
import java.util.*;

public class ModuleIdentifier {
    private Map<String, Map<String, Integer>> couplingGraph;
    private Map<String, Set<String>> clusters;
    private int maxModules;
    private double minCouplingAverage;

    public ModuleIdentifier(Map<String, Map<String, Integer>> couplingGraph, int totalClasses, double cp) {
        this.couplingGraph = couplingGraph;
        this.clusters = new HashMap<>();
        this.maxModules = totalClasses / 2; // M/2 modules maximum
        this.minCouplingAverage = cp; // CP, le couplage minimum moyen par module

        // Initialiser chaque classe comme un cluster individuel
        for (String className : couplingGraph.keySet()) {
            Set<String> initialCluster = new HashSet<>();
            initialCluster.add(className);
            clusters.put(className, initialCluster);
        }
    }

    public void identifyModules() {
        int step = 1;
        while (clusters.size() > maxModules) {
            String[] closestPair = findClosestClusters();
            if (closestPair == null) break;

            Set<String> mergedCluster = new HashSet<>(clusters.get(closestPair[0]));
            mergedCluster.addAll(clusters.get(closestPair[1]));

            if (mergedCluster.size() > 1) {
                double mergedCouplingAverage = calculateAverageCoupling(closestPair[0], closestPair[1]);

                if (mergedCouplingAverage >= minCouplingAverage) {
                    System.out.println("Étape " + step + ": Fusion des clusters " + closestPair[0] + " et " + closestPair[1]);
                    mergeClusters(closestPair[0], closestPair[1]);
                    printClusterCoupling(); // Afficher le couplage des clusters à chaque étape
                    step++;
                } else {
                    System.out.println("Étape " + step + ": Le couplage moyen " + mergedCouplingAverage + " est inférieur à la valeur minimale " + minCouplingAverage + ", donc fusion ignorée.");
                    break;
                }
            }
        }
    }


    // Méthode pour afficher le couplage moyen des clusters à chaque étape
    private void printClusterCoupling() {
        System.out.println("Couplage moyen des clusters actuels :");
        for (String clusterKey : clusters.keySet()) {
            double averageCoupling = calculateAverageCouplingForCluster(clusterKey);
            System.out.println("  Cluster " + clusterKey + " : Couplage moyen = " + averageCoupling);
        }
        System.out.println();
    }


    // Calculer la moyenne du couplage d'un cluster individuel
    private double calculateAverageCouplingForCluster(String clusterKey) {
        Set<String> cluster = clusters.get(clusterKey);
        int totalCoupling = 0;
        int totalPairs = 0;

        for (String classA : cluster) {
            for (String classB : cluster) {
                if (!classA.equals(classB)) {
                    totalCoupling += couplingGraph.getOrDefault(classA, new HashMap<>()).getOrDefault(classB, 0);
                    totalPairs++;
                }
            }
        }
        return totalPairs == 0 ? 0 : (double) totalCoupling / totalPairs;
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

    // Calculer la moyenne du couplage d'un cluster fusionné
    private double calculateAverageCoupling(String clusterA, String clusterB) {
        Set<String> mergedCluster = new HashSet<>(clusters.get(clusterA));
        mergedCluster.addAll(clusters.get(clusterB));

        int totalCoupling = 0;
        int totalPairs = 0;
        for (String classA : mergedCluster) {
            for (String classB : mergedCluster) {
                if (!classA.equals(classB)) {
                    totalCoupling += couplingGraph.getOrDefault(classA, new HashMap<>()).getOrDefault(classB, 0);
                    totalCoupling += couplingGraph.getOrDefault(classB, new HashMap<>()).getOrDefault(classA, 0);
                    totalPairs++;
                }
            }
        }
        return totalPairs == 0 ? 0 : (double) totalCoupling / totalPairs;
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
        System.out.println("État actuel des clusters:");
        int i = 1;
        for (Set<String> cluster : clusters.values()) {
            System.out.println("  Cluster " + i + ": " + cluster);
            i++;
        }
        System.out.println();
    }

    public void printFinalModules() {
        System.out.println("Modules finaux après identification:");
        int i = 1;
        for (Set<String> module : clusters.values()) {
            System.out.println("Module " + i + ": " + module);
            i++;
        }
    }
}

