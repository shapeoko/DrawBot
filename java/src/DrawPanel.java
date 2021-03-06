import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;


	// Custom drawing panel written as an inner class to access the instance variables.
public class DrawPanel extends JPanel implements MouseListener, MouseInputListener  {
	static final long serialVersionUID=2;

	// arc smoothness - increase to make more smooth and run slower.
	double steps_per_degree=10;
	double paper_left,paper_right,paper_top,paper_bottom;
	double limit_left,limit_right,limit_top,limit_bottom;

	// progress
	long linesProcessed=0;
	boolean connected=false;
	boolean running=false;

	// motion control
	boolean mouseIn=false;
	int buttonPressed=MouseEvent.NOBUTTON;
	int oldx, oldy;

	// scale + position
	int cx,cy;
	double cameraOffsetX=0,cameraOffsetY=0;
	double cameraZoom=20;
	float drawScale=0.1f;
	
	
	public DrawPanel() {
		super();
        addMouseMotionListener(this);
        addMouseListener(this);
	}
	
	
	public void setPaperSize(double t,double b,double l,double r) {
		paper_top=t;
		paper_bottom=b;
		paper_left=l;
		paper_right=r;
		repaint();
	}
	
	
	public void setMachineLimits(double t,double b,double l,double r) {
		limit_top=t;
		limit_bottom=b;
		limit_left=l;
		limit_right=r;
		repaint();
	}
	
	
	public void setLinesProcessed(long c) {
		linesProcessed=c;
		if((linesProcessed%10)==0) repaint();
	}
	
	public void setConnected(boolean state) {
		connected=state;
	}
	
	public void setRunning(boolean state) {
		running=state;
	}
	
	// returns angle of dy/dx as a value from 0...2PI
	private double atan3(double dy,double dx) {
	  double a=Math.atan2(dy,dx);
	  if(a<0) a=(Math.PI*2.0)+a;
	  return a;
	}


	private void MoveCamera(int x,int y) {
		// scroll the gcode preview
		double dx=(x-oldx)/cameraZoom;
		double dy=(y-oldy)/cameraZoom;
    	cameraOffsetX-=dx;
    	cameraOffsetY+=dy;
	}
	private void ZoomCamera(int x,int y) {
		double amnt = (double)(y-oldy)*0.01;
		cameraZoom += amnt;
		if(cameraZoom<0.1) cameraZoom=0.1f;
	}
	
	public void mousePressed(MouseEvent e) {
		buttonPressed=e.getButton();
    	oldx=e.getX();
    	oldy=e.getY();
	}
    public void mouseReleased(MouseEvent e) {
    	buttonPressed=MouseEvent.NOBUTTON;
    }
    public void mouseClicked(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {
    	int x=e.getX();
    	int y=e.getY();
    	if(buttonPressed==MouseEvent.BUTTON1) {
    		MoveCamera(x,y);
    	} else if(buttonPressed==MouseEvent.BUTTON3) {
    		ZoomCamera(x,y);
    	}
    	oldx=x;
    	oldy=y;
    	repaint();
    }
    public void mouseMoved(MouseEvent e) {}

    
    private double TX(double a) {
    	return cx+(int)((a-cameraOffsetX)*cameraZoom);
    }
    private double TY(double a) {
    	return cy-(int)((a-cameraOffsetY)*cameraZoom);
    }
    private double ITX(double a) {
    	return TX(a); // TX(a*imageScale-imageOffsetX);
    }
    private double ITY(double a) {
    	return TY(a); // TY(a*imageScale-imageOffsetY);
    }
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);    // paint background
		Graphics2D g2d = (Graphics2D)g;
	   
		cx = this.getWidth()/2;
		cy = this.getHeight()/2;
		
		// draw background
		if(!connected) {			
			setBackground(Color.GRAY);
			g2d.setColor(Color.WHITE);
			g2d.drawRect((int)TX(paper_left),(int)TY(paper_top),
					(int)((paper_right-paper_left)*cameraZoom),
					(int)((paper_top-paper_bottom)*cameraZoom));
		} else {
			setBackground(Color.GRAY);
			g2d.setColor(new Color(194.0f/255.0f,133.0f/255.0f,71.0f/255.0f));
			g2d.fillRect((int)TX(limit_left),(int)TY(limit_top),
					(int)((limit_right-limit_left)*cameraZoom),
					(int)((limit_top-limit_bottom)*cameraZoom));
			g2d.setColor(Color.WHITE);
			g2d.fillRect((int)TX(paper_left),(int)TY(paper_top),
					(int)((paper_right-paper_left)*cameraZoom),
					(int)((paper_top-paper_bottom)*cameraZoom));

		}

		// draw calibration point
		g2d.setColor(Color.RED);
		g2d.drawLine((int)TX(-0.25),(int)TY( 0.00), (int)TX(0.25),(int)TY(0.00));
		g2d.drawLine((int)TX(0),    (int)TY(-0.25), (int)TX(0.00),(int)TY(0.25));
/*
		if(img!=null) {
			int w=img.getWidth();
			int h=img.getHeight();
			g.drawImage(img, 
					(int)ITX(-w/2), (int)ITY(h/2), (int)ITX(w/2), (int)ITY(-h/2), 
					0, 0, w, h,
					null);
			return;
		}*/

		// draw image
		ArrayList<String> instructions = DrawbotGUI.getSingleton().getGcode();
		if(instructions==null) return;
		
		double px=TX(0),py=TY(0),pz=90;
		int i,j;

		for(i=0;i<instructions.size();++i) {
			if(running && i<linesProcessed) {
				g2d.setColor( Color.RED );
			} else if(running && i>linesProcessed && i<=linesProcessed+20) {
				g2d.setColor( Color.GREEN );
			} else {
				g2d.setColor( Color.BLACK );
			}
			
			String line=instructions.get(i);
			
			if(line.contains("G20")) {
				drawScale=0.393700787f;
			} else if(line.contains("G21")) {
				drawScale=0.1f;
			} else if(line.startsWith("G00 ") || line.startsWith("G0 ") || 
				line.startsWith("G01 ") || line.startsWith("G1 ")) {
				// draw a line
				double x=px;
				double y=py;
				double z=pz;
				String[] tokens = line.split("\\s");
				for(j=0;j<tokens.length;++j) {
					if(tokens[j].startsWith("X")) x = Float.valueOf(tokens[j].substring(1)) * drawScale;
					if(tokens[j].startsWith("Y")) y = Float.valueOf(tokens[j].substring(1)) * drawScale;
					if(tokens[j].startsWith("Z")) z = Float.valueOf(tokens[j].substring(1)) * drawScale;
				}

				if(z<5) g2d.drawLine((int)ITX(px),(int)ITY(py),(int)ITX(x),(int)ITY(y));
				px=x;
				py=y;
				pz=z;
			} else if(line.startsWith("G02 ") || line.startsWith("G2 ") ||
				line.startsWith("G03 ") || line.startsWith("G3 ")) {
				// draw an arc
				int dir = (line.startsWith("G02") || line.startsWith("G2")) ? -1 : 1;
				double x=px;
				double y=py;
				double z=pz;
				double ai=px;
				double aj=py;
				String[] tokens = line.split("\\s");
				for(j=0;j<tokens.length;++j) {
					if(tokens[j].startsWith("X")) x = Float.valueOf(tokens[j].substring(1)) * drawScale;
					if(tokens[j].startsWith("Y")) y = Float.valueOf(tokens[j].substring(1)) * drawScale;
					if(tokens[j].startsWith("Z")) z = Float.valueOf(tokens[j].substring(1)) * drawScale;
					if(tokens[j].startsWith("I")) ai = px + Float.valueOf(tokens[j].substring(1)) * drawScale;
					if(tokens[j].startsWith("J")) aj = py + Float.valueOf(tokens[j].substring(1)) * drawScale;
				}

				if(z<5) {
					double dx=px - ai;
					double dy=py - aj;
					double radius=Math.sqrt(dx*dx+dy*dy);

					// find angle of arc (sweep)
					double angle1=atan3(dy,dx);
					double angle2=atan3(y-aj,x-ai);
					double theta=angle2-angle1;

					if(dir>0 && theta<0) angle2+=2.0*Math.PI;
					else if(dir<0 && theta>0) angle1+=2.0*Math.PI;

					theta=Math.abs(angle2-angle1);

					// Draw the arc from a lot of little line segments.
					for(int k=0;k<=theta*steps_per_degree;++k) {
						double angle3 = (angle2-angle1) * ((double)k/(theta*steps_per_degree)) + angle1;
						float nx = (float)(ai + Math.cos(angle3) * radius);
					    float ny = (float)(aj + Math.sin(angle3) * radius);

					    g2d.drawLine((int)ITX(px),(int)ITY(py),(int)ITX(nx),(int)ITY(ny));
						px=nx;
						py=ny;
					}
				    g2d.drawLine((int)ITX(px),(int)ITY(py),(int)ITX(x),(int)ITY(y));
				}
				px=x;
				py=y;
				pz=z;
			}
		}  // for ( each instruction )
	}
}

