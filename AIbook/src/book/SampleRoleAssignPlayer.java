package book;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class SampleRoleAssignPlayer extends AbstractRoleAssignPlayer {

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public SampleRoleAssignPlayer(){
		setVillagerPlayer(new villager());
	}

}
