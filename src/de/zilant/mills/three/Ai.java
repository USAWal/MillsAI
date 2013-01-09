package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
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
					board.setState(BoardState.IMMEDIATE_WIN);
					wins.add(board);
				} else if(!haveIMill && hasOpponentMill) {
					board.setState(BoardState.IMMEDIATE_LOSS);
					losses.add(board);
				} else if(!haveIMill && !hasOpponentMill)
					draws.add(board);
				
			}
		}
		
		losses_with_mills = losses;
		Set<Board> newLosses = getLosses(draws, losses, BoardState.IMMEDIATE_LOSS);
		draws.removeAll(newLosses);
		losses = newLosses;
		
		boolean continueSearch = false;
		do {
			Set<Board> onlyToLosses = new HashSet<Board>();
			for(Board board : draws)
				if(isReachable(board, newLosses, false) && !isReachable(board, wins, false) && !isReachable(board, draws, false))
					onlyToLosses.add(board);
			
			newLosses = getLosses(draws, onlyToLosses, BoardState.LOSS);
			draws.removeAll(newLosses);
			losses.addAll(newLosses);
			continueSearch = newLosses.size() > 0;
			
			onlyToLosses = new HashSet<Board>();
			for(Board board : losses)
				if(isReachable(board, losses, false) && !isReachable(board, wins, false) && !isReachable(board, draws, false))
					onlyToLosses.add(board);
			onlyToLosses = getLosses(draws, onlyToLosses, BoardState.LOSS);
			newLosses.addAll(onlyToLosses);
			draws.removeAll(onlyToLosses);
			losses.addAll(onlyToLosses);
			continueSearch = continueSearch || !onlyToLosses.isEmpty();
			
		} while(continueSearch);
		
		
		Set<Board> toWins = getWins(draws, wins);
		draws.removeAll(toWins);
		
		Set<Board> filteredLosses = getWins(losses, wins);
		losses.removeAll(filteredLosses);
		toWins.addAll(filteredLosses);
		
		Set<Board> onlyToWins = new HashSet<Board>();
		for(Board draw : draws) {
			if(isReachable(draw, toWins, true) && !isReachable(draw, losses, true) && !isReachable(draw, draws, true)) {
				draw.setState(BoardState.IMMEDIATE_WIN);
				onlyToWins.add(draw);
			}
		}
		toWins.removeAll(filteredLosses);
		losses.addAll(filteredLosses);
		draws.addAll(toWins);
		
		toWins = wins;
		
		do {
			Set<Board> newWins = getWins(draws, toWins);
			toWins = new HashSet<Board>(draws);
			for(Board board : draws) {
				if(board.getId() == 132198)
					System.out.println("pause");
				if(!isReachable(board, newWins, true))
					toWins.remove(board);
				else if(board.getState() != BoardState.IMMEDIATE_WIN)
					board.setState(BoardState.WIN);
			}
			draws.removeAll(toWins);
			wins.addAll(toWins);
		} while(toWins.size() > 0);
		wins.removeAll(onlyToWins);
		wins.addAll(onlyToWins);
		/*
		data.addBoard(wins);
		data.addBoard(draws);
		data.addBoard(losses);
		getMove(new Board(new int[] {0,0,0,0,0,0,0,0,0}), true);
		getMove(new Board(new int[] {0,0,0,0,0,0,0,0,0}), false);
		*/
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
		if(board.getNumberOf(IntersectionType.UNOCCUPIED) < 10) {
			
			for(
					int rawState = isMax ? BoardState.IMMEDIATE_WIN.rawValue : BoardState.IMMEDIATE_LOSS.rawValue;
					isMax && rawState >= BoardState.IMMEDIATE_LOSS.rawValue || !isMax && rawState <= BoardState.IMMEDIATE_WIN.rawValue;
					rawState += isMax ? -1 : 1) {
				List<Board> boards = data.getBoardsByState(BoardState.defineState(rawState));
				if(!isMax && board.getNumberOf(IntersectionType.UNOCCUPIED) == 4 && rawState == BoardState.IMMEDIATE_LOSS.rawValue)
					boards.addAll(losses_with_mills);
				if(!boards.isEmpty()) {
					List<Board> result = getAppropriateMovement(board, boards, type);
					if(!result.isEmpty()) return result.get(random.nextInt(result.size()));
				}
			}
			
			return null;
		}
		List<Board> boards = new ArrayList<Board>();
		for(int position = 0; position < 9; position++) {
			Board variant = board.putTo(position, type);
			if(variant != null) {
				if(variant.hasMill(IntersectionType.OCCUPIED_BY_OPPONENT))
					variant.setState(BoardState.IMMEDIATE_LOSS);
				else
					variant.setState(getMove(variant, !isMax).getState());
				boards.add(variant);
			}
		}
		//if(isMax)
		//	data.addBoard(new HashSet<Board>(boards));
		List<ArrayList<Board>> positionsByState = new ArrayList<ArrayList<Board>>();
		for(int index = BoardState.IMMEDIATE_LOSS.rawValue; index <= BoardState.IMMEDIATE_WIN.rawValue; index++)
			positionsByState.add(new ArrayList<Board>());
		for(Board candidate : boards)
			positionsByState.get(candidate.getState().rawValue - BoardState.IMMEDIATE_LOSS.rawValue).add(candidate);
		
		ListIterator<ArrayList<Board>> iterator = positionsByState.listIterator(isMax ? positionsByState.size() : 0);
		while(isMax && iterator.hasPrevious() || !isMax && iterator.hasNext()) {
			ArrayList<Board> positions = isMax ? iterator.previous() : iterator.next();
			if(!positions.isEmpty()) return positions.get(random.nextInt(positions.size()));
		}
		
		return null;
	}
	
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
	
	private Set<Board> losses_with_mills;
	private Set<Board> wins;
	private Set<Board> losses;
	private Set<Board> draws;
	private Random random;
	private Data data;
	
}
