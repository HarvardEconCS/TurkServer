/**
 * 
 */
package edu.harvard.econcs.turkserver;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.Serializable;

import javax.swing.Icon;

public class ColorIcon implements Icon, Serializable  
{  	
	private static final long serialVersionUID = 8358129494572174056L;
	
	public static final int HEIGHT = 14;  
	public static final int WIDTH = 14;  

	private Color color;
	private boolean hasBorder;

	public ColorIcon(Color color, boolean hasBorder) {	
		this.color = color;
		this.hasBorder = hasBorder;
	}
	
	public ColorIcon(Color color)  
	{  
		this(color, true);  
	}  

	public int getIconHeight()  
	{  
		return HEIGHT;  
	}  

	public int getIconWidth()  
	{  
		return WIDTH;  
	}  

	public void paintIcon(Component c, Graphics g, int x, int y)  
	{  
		g.setColor(color);  
		g.fillRect(x, y, WIDTH - 1, HEIGHT - 1);  

		if( hasBorder ) {
			g.setColor(Color.black);
			g.drawRect(x, y, WIDTH - 1, HEIGHT - 1);
		}	  
	}  
}