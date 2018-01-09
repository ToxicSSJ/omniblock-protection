package net.omniblock.protection.base;

import java.sql.SQLException;

import org.bukkit.entity.Player;

import net.omniblock.network.handlers.base.sql.make.MakeSQLQuery;
import net.omniblock.network.handlers.base.sql.make.MakeSQLUpdate;
import net.omniblock.network.handlers.base.sql.make.MakeSQLUpdate.TableOperation;
import net.omniblock.network.handlers.base.sql.type.TableType;
import net.omniblock.network.handlers.base.sql.util.Resolver;
import net.omniblock.network.handlers.base.sql.util.SQLResultSet;
import net.omniblock.protection.base.object.PlayerMembersWrapper;

public class ProtectionBase {
	
	public static PlayerMembersWrapper getMembers(Player player) {
		return getMembers(player.getName(), false);
	}

	public static PlayerMembersWrapper getMembers(String name) {
		return getMembers(name, false);
	}
	
	public static PlayerMembersWrapper getMembers(String param, boolean isNetworkID) {
		
		MakeSQLQuery msq = new MakeSQLQuery(TableType.PROTECTIONS_DATA)
				.select("p_id")
				.select("p_members")
				.where("p_id", isNetworkID ? param : Resolver.getNetworkIDByName(param));
		
		try {
			
			SQLResultSet sqr = msq.execute();
			if (sqr.next()) {
				return new PlayerMembersWrapper(sqr.get("p_id"), sqr.get("p_members"));
			}
			
		} catch (IllegalArgumentException | SQLException e) {
			e.printStackTrace();
		}

		return null;
		
	}
	
	public static void setMembers(PlayerMembersWrapper playerMembers) {

		MakeSQLUpdate msu = new MakeSQLUpdate(TableType.PROTECTIONS_DATA, TableOperation.UPDATE);
		
		msu.rowOperation("p_members", playerMembers.getData());			
		msu.whereOperation("p_id", playerMembers.getPlayerNetworkID());
		
		try {

			msu.execute();
			return;

		} catch (IllegalArgumentException | SQLException e) {
			e.printStackTrace();
		}

		return;

	}
}
