/**
 * Mars Simulation Project
 * MainMenu.java
 * @version 3.08 2015-01-28
 * @author Lars Næsbye Christensen
 */

package org.mars_sim.msp.javafx;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;

public class SettlementScene {

    public Scene createScene() {
        Group  root  =  new  Group();
        Scene  settlementScene  =  new  Scene(root);
        settlementScene.getStylesheets().addAll("/fxui/css/settlementskin.css");		
        Button continueButton = new Button("Back to Main Menu");
        continueButton.setOnAction(new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent arg0) {
        	//MainMenu.changeScene(1);
        }
    });
        
        root.getChildren().add(continueButton);
        return (settlementScene);
    }
	
	
}
