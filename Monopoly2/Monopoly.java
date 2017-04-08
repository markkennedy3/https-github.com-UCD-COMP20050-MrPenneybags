import java.util.ArrayList;


public class Monopoly {

	private static final int START_MONEY = 1500;
	private static final int GO_MONEY = 200;
	private static final int TAX_MONEY = 200;
	
	private Players players = new Players();
	public Player currPlayer;
	private Dice dice = new Dice();
	private Board board = new Board(dice);
	private UI ui = new UI(players, board);
	private boolean gameOver = false;
	private boolean onlyOneNotBankrupt = false;
	private boolean turnFinished;
	private boolean rollDone;
	private boolean rentOwed;
	private boolean rentPaid;
	private int numOfDoubles;
	
	Monopoly () {
		ui.display();
		return;
	}
		
	public void inputNames () {
		int playerId = 0;
		do {
			ui.inputName(playerId);
			if (!ui.isDone()) {
				boolean duplicate = false;
				for (Player p : players.get()) {
					if (ui.getString().toLowerCase().equals(p.getName().toLowerCase())) {
						duplicate = true;
					}
				}
				if (!duplicate) {
					players.add(new Player(ui.getString(),ui.getTokenName(playerId),playerId));
					playerId++;
				} else {
					ui.displayError(UI.ERR_DUPLICATE);
				}
			}
		} while (!ui.isDone() && players.canAddPlayer());
		return;
	}
	
	public void giveStartMoney () {
		for (Player p : players.get()) {
			p.doTransaction (START_MONEY);
			ui.displayBankTransaction (p);
		}
		return;
	}
	
	public void decideStarter () {
		Players inPlayers = new Players(players), selectedPlayers = new Players();
		boolean tie = false;
		do {
			int highestTotal = 0;
			for (Player p : inPlayers.get()) {
				dice.roll();
				ui.displayDice(p,dice);
				if (dice.getTotal() > highestTotal) {
					tie = false;
					highestTotal = dice.getTotal();
					selectedPlayers.clear();
					selectedPlayers.add(p);
				} else if (dice.getTotal() == highestTotal) {
					tie = true;
					selectedPlayers.add(p);
				}
			}
			if (tie) {
				ui.displayRollDraw();
				inPlayers = new Players(selectedPlayers);
				selectedPlayers.clear();
			}
		} while (tie);
		currPlayer = selectedPlayers.get(0);
		ui.displayRollWinner(currPlayer);
		ui.display();
		return;
	}
	
	private void processRoll () {
		if (!rollDone) {
			if (!rentOwed) {
				dice.roll();
				ui.displayDice(currPlayer, dice);
				currPlayer.move(dice.getTotal());
				ui.display();
				
				if (currPlayer.passedGo()) {
					currPlayer.doTransaction(+GO_MONEY);
					ui.displayPassedGo(currPlayer);
					ui.displayBankTransaction(currPlayer);
				}
				ui.displaySquare(currPlayer, board, dice);
				
				//takes 200 from balance when square is landed on
				
				if(board.getSquare(currPlayer.getPosition()) instanceof Property){
					if(currPlayer.getPosition() == 4 || currPlayer.getPosition() == 38){
						currPlayer.doTransaction(-TAX_MONEY);
						ui.displayBankTransaction(currPlayer);
					}
				}
				
				
				
				if(board.getSquare(currPlayer.getPosition()) instanceof Property){
					if(currPlayer.getPosition() == 2 || currPlayer.getPosition() == 17 || currPlayer.getPosition() == 34){
						Cards.CommunityChest();
						ui.displayLandedOnCommunityChest(currPlayer);
					}
					ui.displaySquare(currPlayer, board, dice);
				}
				
				
				
				if (board.getSquare(currPlayer.getPosition()) instanceof Property && 
						((Property) board.getSquare(currPlayer.getPosition())).isOwned() &&
						!((Property) board.getSquare(currPlayer.getPosition())).getOwner().equals(currPlayer) ) {
							rentOwed = true;
							rentPaid = false;
				} else {
					rentOwed = false;
				}
				if (!dice.isDouble()) {
					rollDone = true;
				}
			} else {
				ui.displayError(UI.ERR_RENT_OWED);	
			}
		} else {
			ui.displayError(UI.ERR_DOUBLE_ROLL);
			numOfDoubles += 1;
		}
		if(numOfDoubles == 3){//If there are 3 doubles in a row
			
			int positionFromJail = currPlayer.getPositionsFromJail();
			currPlayer.moveToJail(positionFromJail);
			currPlayer.inJail = true;
			rollDone = true;
		}
		if(board.getSquare(currPlayer.getPosition()) instanceof Property){
			if(currPlayer.getPosition() == 30){
				int positionFromJail = currPlayer.getPositionsFromJail();
				currPlayer.moveToJail(positionFromJail);
				currPlayer.inJail = true;
				rollDone = true;
			}
		}
		
		return;
	}
	
	private void processPayRent () {
		if (board.getSquare(currPlayer.getPosition()) instanceof Property) {
			Property property = (Property) board.getSquare(currPlayer.getPosition());
			if (property.isOwned()) {
				if (!property.getOwner().equals(currPlayer)) {
					if (!rentPaid) {
						int rent = property.getRent();
						if (currPlayer.getBalance()>=rent) {
							Player owner = property.getOwner();
							currPlayer.doTransaction(-rent);
							owner.doTransaction(+rent);
							ui.displayTransaction(currPlayer, owner);
							rentPaid = true;	
							rentOwed = false;
						} else {
							ui.displayError(UI.ERR_INSUFFICIENT_FUNDS);										
						} 
					} else {
						ui.displayError(UI.ERR_RENT_ALREADY_PAID);									
					}
				} else {
					ui.displayError(UI.ERR_SELF_OWNED);								
				}
			} else {
				ui.displayError(UI.ERR_NOT_OWNED);							
			}
		} else {
			ui.displayError(UI.ERR_NOT_A_PROPERTY);
		}
		return;
	}

	private void processBuy () {
		if (board.getSquare(currPlayer.getPosition()) instanceof Property) {
			Property property = (Property) board.getSquare(currPlayer.getPosition());
			if (!property.isOwned()) {
				if (currPlayer.getBalance() >= property.getPrice()) {				
					currPlayer.doTransaction(-property.getPrice());
					ui.displayBankTransaction(currPlayer);
					currPlayer.addProperty(property);
					ui.displayLatestProperty(currPlayer);
				} else {
					ui.displayError(UI.ERR_INSUFFICIENT_FUNDS);
				}
			} else {
				ui.displayError(UI.ERR_IS_OWNED);
			}
		} else {
			ui.displayError(UI.ERR_NOT_A_PROPERTY);
		}
		return;
	}
	private void PayFromJail(){
		if (board.getSquare(currPlayer.getPosition()) instanceof Property) {
			Property property = (Property) board.getSquare(currPlayer.getPosition());
			if (property.isJail() == true) {
				if (currPlayer.isInJail() == true && property.isFinePaid() == false) {
						int fine = property.getJailFine();
						Player jail = null;
						if (currPlayer.getBalance()>= fine) {
							currPlayer.doTransaction(-fine);
							ui.displayTransaction(currPlayer, jail);
						}
						
				}
				
			}
			
		}
		
	}
	
	
	/*
	private void PayTax(){
		if (board.getSquare(currPlayer.getPosition()) instanceof Property) {
			Property property = (Property) board.getSquare(currPlayer.getPosition());
			if (property.onTax() == true) {
						int taxfine = property.getTaxFine();
						Player tax = null;
						if (currPlayer.getBalance()>= taxfine) {
							currPlayer.doTransaction(-taxfine);
							ui.displayTransaction(currPlayer, tax);
						
						
				}
				
			}
			
		}
		
	}
	*/
	private void processBuild () {
		Property property = ui.getInputProperty();
		if (property.isOwned() && property.getOwner().equals(currPlayer)) {
			if (property instanceof Site) {
				Site site = (Site) property;
				if (currPlayer.isGroupOwner(site)) {
					if (!site.isMortgaged()) {
						int numBuildings = ui.getInputNumber();
						if (numBuildings>0) {
							if (site.canBuild(numBuildings)) {
								int debit = numBuildings*site.getBuildingPrice();
								if (currPlayer.getBalance()>debit) {
									site.build(numBuildings);
									currPlayer.doTransaction(-debit);
									ui.displayBuild(currPlayer,site,numBuildings);
								} else {
									ui.displayError(UI.ERR_INSUFFICIENT_FUNDS);
								}
							} else {
								ui.displayError(UI.ERR_TOO_MANY_BUILDINGS);
							}
						} else {
							ui.displayError(UI.ERR_TOO_FEW_BUILDINGS);
						}
					} else {
						ui.displayError(UI.SITE_IS_MORTGAGED);
					}
				} else {
					ui.displayError(UI.ERR_DOES_NOT_HAVE_GROUP);
				}
			} else {
				ui.displayError(UI.ERR_NOT_A_SITE);
			}
		} else {
			ui.displayError(UI.ERR_NOT_YOURS);
		}
		return;
	}
	
	private void processDemolish () {
		Property property = ui.getInputProperty();
		if (property.isOwned() && property.getOwner().equals(currPlayer)) {
			if (property instanceof Site) {
				Site site = (Site) property;
				int numBuildings = ui.getInputNumber();
				if (numBuildings>0) {
					if (site.canDemolish(numBuildings)) {
						site.demolish(numBuildings);
						int credit = numBuildings * site.getBuildingPrice()/2;
						currPlayer.doTransaction(+credit);
						ui.displayDemolish(currPlayer,site,numBuildings);
					} else {
						ui.displayError(UI.ERR_TOO_MANY_BUILDINGS);
					}
				} else {
					ui.displayError(UI.ERR_TOO_FEW_BUILDINGS);
				}
			} else {
				ui.displayError(UI.ERR_NOT_A_SITE);
			}
		} else {
			ui.displayError(UI.ERR_NOT_YOURS);
		}
		return;		
	}
	
	public void processCheat () {
		switch (ui.getInputNumber()) {
			case 1 :       // acquire colour group
				Property property = board.getProperty("kent");
				currPlayer.addProperty(property);		
				property = board.getProperty("whitechapel");
				currPlayer.addProperty(property);
				break;
			case 2 :	   // make zero balance
				currPlayer.doTransaction(-currPlayer.getBalance());
				break;
		}
		return;
	}
	
	public void processBankrupt () {
		ui.displayBankrupt(currPlayer);
		Player tempPlayer = players.getNextPlayer(currPlayer);
		players.remove(currPlayer);
		currPlayer = tempPlayer;
		if (players.numPlayers()==1) {
			gameOver = true;
			onlyOneNotBankrupt = true;
		}
		ui.display();
		return;
	}
	
	public void processMortgage () {
		Property property = ui.getInputProperty();
		if (property.isOwned() && property.getOwner().equals(currPlayer)) {
			if ((property instanceof Site) && !((Site) property).hasBuildings() || (property instanceof Station) || (property instanceof Utility)) {
				if (!property.isMortgaged()) {
					property.setMortgaged();
					currPlayer.doTransaction(+property.getMortgageValue());
					ui.displayMortgage(currPlayer,property);
				} else {
					ui.displayError(UI.ERR_IS_MORTGAGED);
				}
			} else {
				ui.displayError(UI.ERR_HAS_BUILDINGS);
			}
		} else {
			ui.displayError(UI.ERR_NOT_YOURS);
		}
		return;		
	}
	
	public void processRedeem () {
		Property property = ui.getInputProperty();
		if (property.isOwned() && property.getOwner().equals(currPlayer)) {
			if (property.isMortgaged()) {
				int price = property.getMortgageRemptionPrice();
				if (currPlayer.getBalance() >= price) {
					property.setNotMortgaged();
					currPlayer.doTransaction(-price);
					ui.displayMortgageRedemption(currPlayer,property);
				} else {
					ui.displayError(UI.ERR_INSUFFICIENT_FUNDS);
				}
			} else {
				ui.displayError(UI.ERR_IS_NOT_MORTGAGED);
			}
		} else {
			ui.displayError(UI.ERR_NOT_YOURS);
		}
		return;			
	}

	private void processDone () {
		if (rollDone) {
			if (!rentOwed || (rentOwed && rentPaid)) {
				turnFinished = true;
			} else {
				ui.displayError(UI.ERR_RENT_OWED);
			}
		} else {
			ui.displayError(UI.ERR_NO_ROLL);
		}
		return;
	}
	
	public void processTurn () {
		turnFinished = false;
		rollDone = false;
		rentOwed = false;
		rentPaid = false;
		do {
			ui.inputCommand(currPlayer);
			switch (ui.getCommandId()) {
				case UI.CMD_ROLL :
					processRoll();
					break;
				case UI.CMD_PAY_RENT :
					processPayRent();
					break;
				case UI.CMD_BUY :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					processBuy();
					break;
					}
				case UI.CMD_BALANCE :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					ui.displayBalance(currPlayer);
					break;
					}
				case UI.CMD_PROPERTY :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					ui.displayProperty(currPlayer);
					break;
					}
				case UI.CMD_BANKRUPT :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					processBankrupt();
					turnFinished = true;
					break;
					}
				case UI.CMD_BUILD :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					processBuild();
					break;
					}
				case UI.CMD_DEMOLISH :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					processDemolish();
					break;
					}
				case UI.CMD_REDEEM :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					processRedeem();
					break;
					}
				case UI.CMD_MORTGAGE :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					processMortgage();
					break;
					}
				case UI.CMD_CHEAT :
					if(currPlayer.isInJail() == true){
						ui.displayError(UI.ERR_IN_JAIL);
					}else{
					processCheat();
					break;
					}
				case UI.CMD_HELP :
					ui.displayCommandHelp();
					break;
				case UI.CMD_DONE :
					processDone();
					break;
				case UI.CMD_QUIT : 
					turnFinished = true;
					gameOver = true;
					break;
			}
			if(board.getSquare(currPlayer.getPosition()) instanceof Property){
				Property property = (Property) board.getSquare(currPlayer.getPosition());
				if(property.isGoToJail()){
					//currPlayer.
				}
				
			}
		} while (!turnFinished);
		return;
	}
	
	public void nextPlayer () {
		currPlayer = players.getNextPlayer(currPlayer);
		return;
	}
	
	public void decideWinner () {
		if (onlyOneNotBankrupt) {
			ui.displayWinner(currPlayer);			
		} else {
			ArrayList<Player> playersWithMostAssets = new ArrayList<Player>();
			int mostAssets = players.get(0).getAssets();
			for (Player player : players.get()) {
				ui.displayAssets(player);
				if (player.getAssets() > mostAssets) {
					playersWithMostAssets.clear(); 
					playersWithMostAssets.add(player);
				} else if (player.getAssets() == mostAssets) {
					playersWithMostAssets.add(player);
				}
			}
			if (playersWithMostAssets.size() == 1) {
				ui.displayWinner(playersWithMostAssets.get(0));
			} else {
				ui.displayDraw(playersWithMostAssets);
			}
		}
		return;
	}
	
	public void displayGameOver () {
		ui.displayGameOver ();
		return;
	}
	
	public boolean isGameOver () {
		return gameOver;
	}
}