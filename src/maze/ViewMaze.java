package maze;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import occlusion.BspOcclusionSpanNode;

/**
 *
 * @author Leo
 */
public class ViewMaze extends JPanel {

    
    private class Wall implements Comparable<Wall> {
        public int type;
        public double x1;
        public double y1;
        public double x2;
        public double y2;
        public double cx;
        public double cy;
        private Color[] colors = { Color.BLUE, Color.RED };
        
        public boolean rendered;
        public boolean occluded;
        
        // wallType 1=horizontal 2=vertical
        public Wall(int col, int row, int wallType) {
            type = wallType - 1;
            this.x1 = col * 16;
            this.y1 = row * 16;
            if (wallType == 1) {
                this.x2 = x1 + 16;
                this.y2 = y1;
            }
            else if (wallType == 2) {
                this.x2 = x1;
                this.y2 = y1 + 16;
            }
            cx = (x1 + x2) / 2;
            cy = (y1 + y2) / 2;
        }
        
        public Wall(double x1, double y1, double x2, double y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            cx = (x1 + x2) / 2;
            cy = (y1 + y2) / 2;
        }

        public double getDistanceFromPlayer() {
            double dx = playerX - cx;
            double dy = playerY - cy;
            double dist = (dx * dx + dy * dy);  // Math.sqrt
            return dist;
        }
        
        public void drawTopView(Graphics2D g) {
            if (rendered) {
                g.setColor(Color.GREEN);
            }
            else if (occluded) {
                g.setColor(Color.ORANGE);
            }
            else {
                g.setColor(Color.BLACK);
            }
            g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        }
        
        private static final Polygon polygon = new Polygon();
        
        public void draw3D(Graphics2D g) {
            rendered = false;
            occluded = false;
            
            double vx = Math.cos(playerDirection);
            double vy = Math.sin(playerDirection);
            double vperpx = Math.cos(playerDirection + Math.toRadians(90));
            double vperpy = Math.sin(playerDirection + Math.toRadians(90));

            double cx1 = x1 - playerX;
            double cy1 = y1 - playerY;
            double cx2 = x2 - playerX;
            double cy2 = y2 - playerY;
            
            double dz1 = vx * cx1 + vy * cy1; 
            double dx1 = vperpx * cx1 + vperpy * cy1; 
            double dz2 = vx * cx2 + vy * cy2; 
            double dx2 = vperpx * cx2 + vperpy * cy2; 
            
            if (dz1 <= 0 && dz2 <= 0) {
                totalBehindWalls++;
                return;
            }
            else if (dz1 <= 0) {
                double s = -dz1 / (dz2 - dz1);
                dz1 = 0.01;
                dx1 = dx1 + s * (dx2 - dx1);        
            }
            else if (dz2 <= 0) {
                double s = -dz2 / (dz1 - dz2);
                dz2 = 0.01;
                dx2 = dx2 + s * (dx1 - dx2);
            }
            
            double sx1 = 350 * dx1 / dz1;
            double sh1 = 350 * 8 / dz1;
            double sx2 = 350 * dx2 / dz2;
            double sh2 = 350 * 8 / dz2;
            
            int rx1 = (int) (400 + sx1);
            int rx2 = (int) (400 + sx2);
            
            if (rx1 > rx2) {
                int tmp = rx1;
                rx1 = rx2;
                rx2 = tmp;
                double tmp2 = sh1;
                sh1 = sh2;
                sh2 = tmp2;
            }
            
            // check occlusion
            occlusionResult.clear();
            occlusion.addSpan(rx1, rx2, occlusionResult);
            if (occlusionResult.isEmpty()) {
                occluded = true;
                totalOccludedWalls++;
                return; 
            }
            
            rendered = true;
            totalRenderedWalls++;
            
            polygon.reset();
            polygon.addPoint(rx1, (int) (300 + sh1));
            polygon.addPoint(rx1, (int) (300 - sh1));
            polygon.addPoint(rx2, (int) (300 - sh2));
            polygon.addPoint(rx2, (int) (300 + sh2));
            
            Composite oc = g.getComposite();
            g.setComposite(AlphaComposite.DstOver);            
            
            g.setStroke(stroke);
            
            g.setColor(colors[type]);
            g.fillPolygon(polygon);
            g.setColor(Color.BLACK);
            g.drawPolygon(polygon);
            
            g.setComposite(oc);
        }
        
        @Override
        public int compareTo(Wall o) {
            return (int) Math.signum(getDistanceFromPlayer() - o.getDistanceFromPlayer());
        }

    }
    private static final int ROWS = 4000;
    private static final int COLS = 4000;
    private int[][] maze = new int[ROWS][COLS];
    private Wall[][][] mazeWall = new Wall[ROWS][COLS][2];
    
    private int totalWalls;
    private int totalOccludedWalls;
    private int totalRenderedWalls;
    private int totalBehindWalls;
    
    private double playerX = 40;
    private double playerY = 40;
    private double playerDirection;
    
    private List<Wall> walls = new ArrayList<Wall>();
    private List<Wall> checkedWalls = new ArrayList<Wall>();
    private final BufferedImage offscreen;
    private final Graphics2D offscreenG;
    
    private BspOcclusionSpanNode occlusion = new BspOcclusionSpanNode(0, 799);
    private List<Integer> occlusionResult = new ArrayList<>();
    private Stroke stroke = new BasicStroke(5.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    private Font font = new Font("Arial", Font.PLAIN, 20);
    
    public ViewMaze() {
        setBackground(Color.GRAY);
        offscreen = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        offscreenG = (Graphics2D) offscreen.getGraphics();
        offscreenG.setBackground(new Color(0, 0, 0, 0));
        
        createRandomMaze();
        addKeyListener(new KeyHandler());
    }

    private void createRandomMaze() {
        for (int row = 0; row < maze.length; row++) {
            int[] cols = maze[row];
            for (int col = 0; col < cols.length; col++) {
                cols[col] = (int) (4 * Math.random());
                if (cols[col] > 3) {
                    cols[col] = 0;
                }
                
                if (col == 0 || col == cols.length - 1) {
                    cols[col] = 3;
                }
                if (row == 0 || row == maze.length - 1) {
                    cols[col] = 3;
                }
                Wall wall = null;
                if (cols[col] == 1) {
                    totalWalls++;
                    walls.add(wall = new Wall(col, row, 1));
                    mazeWall[row][col][0] = wall;
                }
                if (cols[col] == 2) {
                    totalWalls++;
                    walls.add(wall = new Wall(col, row, 2));
                    mazeWall[row][col][1] = wall;
                }
                else if (cols[col] == 3) {
                    totalWalls += 2;
                    walls.add(wall = new Wall(col, row, 1));
                    mazeWall[row][col][0] = wall;
                    walls.add(wall = new Wall(col, row, 2));
                    mazeWall[row][col][1] = wall;
                }
                
                
            }
        }
        System.out.println("total walls: " + totalWalls);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        drawMaze3DView();
        g.drawImage(offscreen, 0, 0, null);
        drawMazeTopView(g2d);
        drawPlayer(g2d);
        drawStatistics(g2d);
    }

    private void drawStatistics(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(font);
        g.drawString("--- Statistics ---", 400, 20);
        g.drawString("Total Walls: " + totalWalls, 400, 50);
        g.drawString("Checked Walls: " + checkedWalls.size(), 400, 70);
        g.setColor(Color.BLACK);
        g.drawString("Behind Player Walls: " + totalBehindWalls, 400, 90);
        g.setColor(Color.ORANGE);
        g.drawString("Occluded Walls: " + totalOccludedWalls, 400, 110);
        g.setColor(Color.GREEN);
        g.drawString("Rendered Walls: " + totalRenderedWalls, 400, 130);
    }

    private void drawPlayer(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval((int) (playerX - 3), (int) (playerY - 3), 6, 6);
        double dx = Math.cos(playerDirection);
        double dy = Math.sin(playerDirection);
        g.setColor(Color.WHITE);
        g.drawLine((int) playerX, (int) playerY
                , (int) (playerX + 30 * dx), (int) (playerY + 30 * dy));
    }

    private void drawMazeTopView(Graphics2D g) {
        g.setStroke(stroke);
        Collections.sort(walls);
        for (int i = 0; i < checkedWalls.size(); i++) {
            Wall wall = checkedWalls.get(i);
            wall.drawTopView(g);
        }
    }
    
    private void drawMaze3DView() {
        offscreenG.clearRect(0, 0, 800, 600);
        occlusion.reset();
        
        totalBehindWalls = 0;
        totalOccludedWalls = 0;
        totalRenderedWalls = 0;
        
        checkedWalls.clear();
        int playerCol = (int) (playerX / 16);
        int playerRow = (int) (playerY / 16);
        int col = playerCol;
        int row = playerRow;
        int distance = 0;
        boolean processing = true;
        outer:
        while (processing) {
            walls.clear();
            if (distance == 0) {
                try {
                    if (mazeWall[row][col][0] != null ) walls.add(mazeWall[row][col][0]);
                    if (mazeWall[row][col][1] != null ) walls.add(mazeWall[row][col][1]);
                }
                catch (Exception e) {}
            }
            for (int i = 0; i < distance; i++) {
                col++;
                if (getMazeWall(row, col, 0) != null ) walls.add(getMazeWall(row, col, 0));
                if (getMazeWall(row, col, 1) != null ) walls.add(getMazeWall(row, col, 1));
            }
            for (int i = 0; i < distance; i++) {
                row++;
                if (getMazeWall(row, col, 0) != null ) walls.add(getMazeWall(row, col, 0));
                if (getMazeWall(row, col, 1) != null ) walls.add(getMazeWall(row, col, 1));
            }
            for (int i = 0; i < distance; i++) {
                col--;
                if (getMazeWall(row, col, 0) != null ) walls.add(getMazeWall(row, col, 0));
                if (getMazeWall(row, col, 1) != null ) walls.add(getMazeWall(row, col, 1));
            }
            for (int i = 0; i < distance; i++) {
                row--;
                if (getMazeWall(row, col, 0) != null ) walls.add(getMazeWall(row, col, 0));
                if (getMazeWall(row, col, 1) != null ) walls.add(getMazeWall(row, col, 1));
            }
            col -= 1;
            row -= 1;
            distance += 2;
            Collections.sort(walls);
            for (int i = 0; i < walls.size(); i++) {
                Wall wall = walls.get(i);
                wall.draw3D(offscreenG);
                checkedWalls.add(wall);
                if (occlusion.isOccluded()) {
                    break outer;
                }
            }
        }
    }
    
    private Wall getMazeWall(int row, int col, int w) {
        if (row < 0 || col < 0 
                || row > mazeWall.length - 1 || col > mazeWall[0].length - 1) {
            
            return null;
        }
        return mazeWall[row][col][w];        
    }
    
    private void drawMazeTopView2(Graphics2D g) {
        int playerCol = (int) (playerX / 16);
        int playerRow = (int) (playerY / 16);
        g.setColor(Color.ORANGE);
        g.fillRect(playerCol * 16, playerRow * 16, 16, 16);
                
        g.setColor(Color.BLACK);
        for (int row = 0; row < maze.length; row++) {
            int[] cols = maze[row];
            for (int col = 0; col < cols.length; col++) {
                int m = cols[col];
                switch (m) {
                    case 1:
                        g.drawLine(col * 16, row * 16, col * 16 + 16, row * 16);
                        break;
                    case 2:
                        g.drawLine(col * 16, row * 16, col * 16, row * 16 + 16);
                        break;
                    case 3:
                        g.drawLine(col * 16, row * 16, col * 16 + 16, row * 16);
                        g.drawLine(col * 16, row * 16, col * 16, row * 16 + 16);
                        break;
                }
            }
        }
    }    

    private void draw3D(Graphics2D g) {
        int playerCol = (int) (playerX / 16);
        int playerRow = (int) (playerY / 16);
        for (int i = 0; i < 5; i++) {
            int cCol = playerCol - i;
            int cRow = playerRow - i;
            
        }
    }
    
    private class KeyHandler extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                playerDirection -= 0.1;
            }
            else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                playerDirection += 0.1;
            }

            if (e.getKeyCode() == KeyEvent.VK_UP) {
                movePlayerForward(1);
            }
            else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                movePlayerForward(-1);
            }
            
            repaint();
        }

    }
    
    private void movePlayerForward(double speed) {
        double dx = Math.cos(playerDirection);
        double dy = Math.sin(playerDirection);
        playerX += speed * dx;
        playerY += speed * dy;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ViewMaze view = new ViewMaze();
            view.setPreferredSize(new Dimension(800, 600));
            JFrame frame = new JFrame();
            frame.setTitle("Bsp Front to Back Occlusion Culling Spans - Test #2");
            frame.getContentPane().add(view);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            view.requestFocus();
        });
    }
    
}
