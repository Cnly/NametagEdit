package ca.wacos.nametagedit.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable;

import ca.wacos.nametagedit.NametagEdit;

public class DeleteTypeTask extends BukkitRunnable {

    private String table, type, val;
    
    public DeleteTypeTask(String table, String type, String val) {
        this.table = table;
        this.type = type;
        this.val = val;
    }
    
    @Override
    public void run() {
        Connection connection = null;

        try {
            connection = NametagEdit.getInstance().getConnectionPool().getConnection();

            String query = "DELETE FROM `" + table + "` WHERE `" + type + "`=?;";

            PreparedStatement p = connection.prepareStatement(query);
            p.setString(1, val);
            p.execute();
            p.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}