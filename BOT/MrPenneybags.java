public class MrPenneybags implements Bot {
	
	// The public API of YourTeamName must not change
	// You cannot change any other classes
	// YourTeamName may not alter the state of the board or the player objects
	// It may only inspect the state of the board and the player objects

	private BoardAPI board;
	private PlayerAPI player;
	private DiceAPI dice;
	private boolean rolldone = false;
	
	MrPenneybags (BoardAPI board, PlayerAPI player, DiceAPI dice) {
		this.board = board;
		this.player = player;
		this.dice = dice;
		return;
	}
	
	public String getName () {
		return "MrPenneybags";
	}
	
	
	
	
	
	
	public String getCommand () {
		

		
		String command = "roll";
		
		
		if(rolldone==false)
		{
			command="roll";
			rolldone = true;
			
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		else 
		{
			command="done";
			rolldone = false;
			
		}
		
				
		Square position;
		position = board.getSquare(player.getPosition());
		
     	if(position instanceof Property){
		 Property property = (Property) board.getSquare(player.getPosition());
		
		 if (property.isOwned() && property.getOwner() != player){
				command = "pay rent";
				return command;
			}
		
		 else if(!property.isOwned()){
				command = "buy";
				return command;
			}
		}	
		
		return command;
	
		
	}
	
	
	
	
	
	
	
	public String getDecision () {
		
	return " ";
	}
}