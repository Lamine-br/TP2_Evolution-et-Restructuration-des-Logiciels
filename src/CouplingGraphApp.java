
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class CouplingGraphApp extends JFrame {
    public CouplingGraphApp(String imagePath) {
        setTitle("Graphe de Couplage");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Créez un label pour afficher l'image
        JLabel label = new JLabel();
        ImageIcon icon = new ImageIcon(imagePath);
        label.setIcon(icon);

        // Ajoutez le label à la fenêtre
        add(label, BorderLayout.CENTER);
        setLocationRelativeTo(null); // Centrer la fenêtre
    }

    public static void main(String[] args) {
        // Exemple d'utilisation
        SwingUtilities.invokeLater(() -> {
            CouplingGraphApp app = new CouplingGraphApp("E:\\PDC\\Visitor Pattern\\coupling_graph.png");
            app.setVisible(true);
        });
    }
}
