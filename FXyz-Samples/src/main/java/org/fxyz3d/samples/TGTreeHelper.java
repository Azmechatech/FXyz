/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fxyz3d.samples;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javafx.scene.control.TreeItem;

/**
 *
 * @author ryzen
 */
public class TGTreeHelper {
    
    // This method creates an ArrayList of TreeItems (Products)
    public static ArrayList<TreeItem> getModelTree(Set<String> parts) {
        ArrayList<TreeItem> cars = new ArrayList<TreeItem>();
        parts.forEach(part -> {
            TreeItem partItem = new TreeItem(part);
            cars.add(partItem);
        });
        return cars;
    }
}
