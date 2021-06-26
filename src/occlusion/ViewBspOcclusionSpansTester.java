package occlusion;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Bsp front to back occlusion culling spans tester
 * 
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class ViewBspOcclusionSpansTester extends JPanel {

    private final BspOcclusionSpanNode bsp = new BspOcclusionSpanNode(0, 512);
    
    private boolean mousePressed = false;
    private final Point mousePressedPoint = new Point();
    private final Point mouseReleasedPoint = new Point();
    private final Color selectedColor = new Color(0, 255, 0, 64);
    private final int offsetX = 50;
    
    public ViewBspOcclusionSpansTester() {
        addInputListeners();
    }
    
    private void addInputListeners() {
        MouseHandler mouseHandler = new MouseHandler();
        addKeyListener(new KeyHandler());
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        g.translate(offsetX, 0);
        
        g.drawString("'R' key to Reset", 0, 50);
        
        g.drawLine(0, 90, 0, 500);
        g.drawString("" + bsp.getStart(), -2, 80);
        g.drawLine(512, 90, 512, 500);
        g.drawString("" + bsp.getEnd(), 500, 80);
        
        drawBspOcclusionNodes(bsp, (Graphics2D) g);
        
        if (mousePressed) {
            g.setColor(selectedColor);
            g.fillRect(mousePressedPoint.x, 100 - 2
                    , mouseReleasedPoint.x - mousePressedPoint.x, 300);
            
            g.setColor(Color.GREEN);
            g.fillRect(mousePressedPoint.x, 100 - 2
                    , mouseReleasedPoint.x - mousePressedPoint.x, 4);
        }
    }

    public void drawBspOcclusionNodes(BspOcclusionSpanNode node, Graphics2D g) {
        int y = node.getLevel() * 20;
        g.setColor(Color.BLACK);
        g.drawLine(node.getStart(), 100 + y, node.getEnd(), 100 + y);
        
        if (node.isOccluded()) {
            g.setColor(Color.BLUE);
            g.fillRect(node.getStart(), 100 + y - 2, node.getEnd() - node.getStart(), 4);
            return;
        }
        
        if (node.getPartitionPoint() != null) {
            g.setColor(Color.RED);
            g.fillOval((node.getPartitionPoint() - 3), 100 + y, 6, 6);
        }
        
        if (node.isPartitioned()) {
            drawBspOcclusionNodes(node.getLeft(), g);
            drawBspOcclusionNodes(node.getRight(), g);
        }
    }

    private class KeyHandler extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                bsp.reset();
            }
            repaint();
        }

    }
    
    private class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            mousePressed = true;
            mousePressedPoint.setLocation(e.getX() - offsetX, e.getY());
            mouseReleasedPoint.setLocation(e.getX() - offsetX, e.getY());
            repaint();
        }

        List<Integer> r = new ArrayList<>();

        @Override
        public void mouseReleased(MouseEvent e) {
            mousePressed = false;
            mouseReleasedPoint.setLocation(e.getX() - offsetX, e.getY());
            
            r.clear();
            bsp.addSpan(mousePressedPoint.x, mouseReleasedPoint.x, r);
            String spans = "";
            for (int i = 0; i < r.size(); i+=2) {
                spans += "(" + r.get(i) + "~" + r.get(i + 1) + ") ";
            }
            if (r.isEmpty()) {
                spans = "completely occluded";
            }
            JOptionPane.showMessageDialog(ViewBspOcclusionSpansTester.this, "added span: (" 
                    + mousePressedPoint.x + "~" + mouseReleasedPoint.x 
                    + ") \n" + "visible spans: " + spans);
            
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseReleasedPoint.setLocation(e.getX() - offsetX, e.getY());
            repaint();
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ViewBspOcclusionSpansTester view = new ViewBspOcclusionSpansTester();
            view.setPreferredSize(new Dimension(600, 400));
            JFrame frame = new JFrame();
            frame.setTitle("Bsp Front to Back Occlusion Culling Spans - Test #1");
            frame.getContentPane().add(view);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            view.requestFocus();
        });
    }
    
}
