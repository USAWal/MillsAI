package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Ai {
	
	public Ai(Data data) {
		wins = new HashSet<Board>();
		losses = new HashSet<Board>();
		draws = new HashSet<Board>();
		random = new Random();
		this.data = data;
		
		int[] values = new int[9];
		while(!Arrays.equals(values, new int[] {2,2,2,1,1,1,0,0,0}))
		{
			int index = 9;
			int divisionResult = 0;
			do {
				values[--index] += 1;
				divisionResult = values[index] / 3;
				values[index] %= 3;
			} while(divisionResult > 0 && index > 0);

			Board board = new Board(values);
			if(
					board.getNumberOf(IntersectionType.OCCUPIED_BY_ME) == 3 &&
					board.getNumberOf(IntersectionType.OCCUPIED_BY_OPPONENT) == 3) {
				boolean haveIMill = board.hasMill(IntersectionType.OCCUPIED_BY_ME);
				boolean hasOpponentMill = board.hasMill(IntersectionType.OCCUPIED_BY_OPPONENT);
				if(haveIMill && !hasOpponentMill) {
					board.setState(BoardState.WIN);
					wins.add(board);
				} else if(!haveIMill && hasOpponentMill) {
					board.setState(BoardState.LOSS);
					losses.add(board);
				} else if(!haveIMill && !hasOpponentMill)
					draws.add(board);
				
			}
		}
		
		Set<Board> newLosses = getLosses(draws, losses);
		draws.removeAll(newLosses);
		losses = newLosses;
		
		boolean continueSearch = false;
		do {
			Set<Board> onlyToLosses = new HashSet<Board>();
			for(Board board : draws)
				if(isReachable(board, newLosses, false) && !isReachable(board, wins, false) && !isReachable(board, draws, false))
					onlyToLosses.add(board);
			
			newLosses = getLosses(draws, onlyToLosses);
			draws.removeAll(newLosses);
			losses.addAll(newLosses);
			continueSearch = newLosses.size() > 0;
			
			onlyToLosses = new HashSet<Board>();
			for(Board board : losses)
				if(isReachable(board, losses, false) && !isReachable(board, wins, false) && !isReachable(board, draws, false))
					onlyToLosses.add(board);
			onlyToLosses = getLosses(draws, onlyToLosses);
			newLosses.addAll(onlyToLosses);
			draws.removeAll(onlyToLosses);
			losses.addAll(onlyToLosses);
			continueSearch = continueSearch || !onlyToLosses.isEmpty();
			
		} while(continueSearch);
		
		Set<Board> toWins = wins;
		
		do {
			Set<Board> newWins = getWins(draws, toWins);
			toWins = new HashSet<Board>(draws);
			for(Board board : draws) {
				if(!isReachable(board, newWins, true))
					toWins.remove(board);
				else
					board.setState(BoardState.WIN);
			}
			draws.removeAll(toWins);
			wins.addAll(toWins);
		} while(toWins.size() > 0);
		
		//data.addBoard(wins);
		//data.addBoard(draws);
		//data.addBoard(losses);
		
		System.out.println("Finish");
	}
	
	private List<Board> getAppropriateMovement(Board board, List<Board> boards, IntersectionType type) {
		List<Board> result = new ArrayList<Board>();
		boolean isMiddleStage = board.getNumberOf(IntersectionType.OCCUPIED_BY_ME) == 3 && board.getNumberOf(IntersectionType.OCCUPIED_BY_OPPONENT) == 3;
		for(Board to : boards) {
			if(isMiddleStage) {
				if(Board.isReachable(board, to, false))
					result.add(to);
			} else {
				long boardId = board.getId();
				long toId = to.getId();
				long difference = 0;
				for(int index = 0; index < 9; index++) {
					long boardPosition = boardId & 3;
					long toPosition = toId & 3;
					boardId = boardId >> 2;
					toId = toId >> 2;
					if(boardPosition != toPosition) difference = (difference << 2) | boardPosition | toPosition;
				}
				if(difference == type.rawValue)
					result.add(to);
			}
		}
		return result;
	}
	
	public Board getMove(Board board, boolean isMax) {
		IntersectionType type = isMax ? IntersectionType.OCCUPIED_BY_ME : IntersectionType.OCCUPIED_BY_OPPONENT;
		if(9 - board.getNumberOf(IntersectionType.UNOCCUPIED) > 4) {
			List<Board> boards = data.getBoardsByState(isMax ? BoardState.WIN : BoardState.LOSS);
			if(!boards.isEmpty()) {
				List<Board> result = getAppropriateMovement(board, boards, type);
				if(isMax) {
					List<Board> wins = new ArrayList<Board>();
					for(Board to : result)
						if(to.hasMill(IntersectionType.OCCUPIED_BY_ME))
							wins.add(to);
					if(!wins.isEmpty()) result = wins;
				}
				if(!result.isEmpty()) return result.get(random.nextInt(result.size()));
			}
			
			boards = data.getBoardsByState(BoardState.DRAW);
			if(!boards.isEmpty()) {
				List<Board> result = getAppropriateMovement(board, boards, type);
				if(!result.isEmpty()) return result.get(random.nextInt(result.size()));
			}
			
			boards = data.getBoardsByState(isMax ? BoardState.LOSS : BoardState.WIN);
			if(!boards.isEmpty()) {
				List<Board> result = getAppropriateMovement(board, boards, type);
				if(!isMax) {
					List<Board> wins = new ArrayList<Board>();
					for(Board to : result)
						if(to.hasMill(IntersectionType.OCCUPIED_BY_ME))
							wins.add(to);
					if(wins.size() < result.size()) result.removeAll(wins);					
				}
				if(!result.isEmpty()) return result.get(random.nextInt(result.size()));
			}
			
			return null;
		}
		List<Board> boards = new ArrayList<Board>();
		for(int position = 0; position < 9; position++) {
			Board variant = board.putTo(position, type);
			if(variant != null) {
				if(variant.hasMill(IntersectionType.OCCUPIED_BY_OPPONENT))
					variant.setState(BoardState.LOSS);
				else
					variant.setState(getMove(variant, !isMax).getState());
				boards.add(variant);
			}
		}
		List<Board> wins = new ArrayList<Board>();
		List<Board> losses = new ArrayList<Board>();
		List<Board> draws = new ArrayList<Board>();
		for(Board candidate : boards)
			if(candidate.getState() == BoardState.WIN)
				wins.add(candidate);
			else if(candidate.getState() == BoardState.LOSS)
				losses.add(candidate);
			else
				draws.add(candidate);
		List<Board> filtered = new ArrayList<Board>();
		for(Board win : wins)
			if(win.hasMill(IntersectionType.OCCUPIED_BY_ME))
				filtered.add(win);
		if(!filtered.isEmpty() && filtered.size() < wins.size()) {
			if(isMax) wins = filtered;
			else wins.removeAll(filtered);
		}

		List<Board> reulst = isMax ? wins : losses;
		if(!reulst.isEmpty()) return reulst.get(random.nextInt(reulst.size()));
		if(!draws.isEmpty()) return draws.get(random.nextInt(draws.size()));
		reulst = isMax ? losses : wins;
		if(!reulst.isEmpty()) return reulst.get(random.nextInt(reulst.size()));
		
		return null;
	}
	
	private Set<Board> getLosses(Set<Board> from, Set<Board> to) { return getLosses(from, to, BoardState.LOSS); }
	
	private Set<Board> getLosses(Set<Board> from, Set<Board> to, BoardState state) {
		Set<Board> result = new HashSet<Board>();
		for(Board board : from)
			for(Board loss : to)
				if(Board.isReachable(board, loss, true)) {
					board.setState(state);
					result.add(board);
				}
		return result;
	}
	
	private Set<Board> getWins(Set<Board> from, Set<Board> to) {
		Set<Board> result = new HashSet<Board>();
		for(Board board : from)
			for(Board win : to)
				if(Board.isReachable(board, win, false))
					result.add(board);
		return result;
	}
	
	private boolean isReachable(Board from, Collection<Board> to, boolean byOpponent) {
		for(Board board : to)
			if(Board.isReachable(from, board, byOpponent))
				return true;
		return false;
	}
	
	private Set<Board> wins;
	private Set<Board> losses;
	private Set<Board> draws;
	private Random random;
	private Data data;
	
}
