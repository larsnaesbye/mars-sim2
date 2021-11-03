package org.mars_sim.fxgl.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.mars_sim.fxgl.main.SoftknkioApp;

public class SaveGameData {

    public static void saveData() {
        try {
            DataFile.dataFile().setWritable(true);

            BufferedWriter writer = new BufferedWriter(new FileWriter(DataFile.dataFile()));

            //delete old data
            writer.write("");

            //write current data
            writer.write(SoftknkioApp.matchfield.getPlayer().getNickname());
            writer.newLine();
            writer.write(String.valueOf(SoftknkioApp.matchfield.getPlayer().getScore()));

            for (int i = 0; i < 5; i++) {
                writer.newLine();
                writer.write(String.valueOf(SoftknkioApp.matchfield.getDashboard().getOperations()[i].getLevel()));
            }

            DataFile.dataFile().setWritable(false);
            writer.close();

        } catch (IOException e) {
//            e.printStackTrace();
        }
    }
}
