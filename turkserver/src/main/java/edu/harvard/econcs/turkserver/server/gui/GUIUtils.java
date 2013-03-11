package edu.harvard.econcs.turkserver.server.gui;

import java.awt.Component;
import javax.swing.JOptionPane;

import edu.harvard.econcs.turkserver.mturk.RequesterServiceExt;

public class GUIUtils {

	public static boolean checkRequesterNotNull(RequesterServiceExt req, Component comp) {		
		if( req == null ) {
			JOptionPane.showMessageDialog(comp, "Sorry, MTurk requester was not configured correctly.",
					"Requester Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}		
		return true;
	}

	public static void showException(Exception e, Component comp) {
		JOptionPane.showMessageDialog(comp, e, "Error", JOptionPane.ERROR_MESSAGE);		
	}

}
