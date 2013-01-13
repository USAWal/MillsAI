package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.management.ImmutableDescriptor;

public class Ai {
	
	private long increase(long line) {
		long mask = 2;
		while((line & mask) != 0) {
			line -= mask;
			mask <<= 2;
		}
		line += mask >> 1;
		return line;
	}
	
	public Ai(Data data) {
		wins = new HashSet<Position>();
		losses = new HashSet<Position>();
		draws = new HashSet<Position>();
		random = new Random();
		this.data = data;
		
		for(long topLine = 0; topLine <= 0x2A; topLine = increase(topLine))
			for(long bottomLine = topLine; bottomLine <= 0x2A; bottomLine = increase(bottomLine))
				for(long centerLne = 0; centerLne <= 0x2A; centerLne = increase(centerLne)) {
					Position position = new Position(topLine << 12 | centerLne << 6 | bottomLine);
					if( position.NUMBER_OF_MY_PIECES == 3 && position.NUMBER_OF_OPPONENTS_PIECES == 3) {
						boolean haveIMill = position.hasMill(PieceType.MINE);
						boolean hasOpponentMill = position.hasMill(PieceType.OPPONENTS);
						Position symmetricPosition = new Position(bottomLine << 12 | centerLne << 6 | topLine);
						if(haveIMill && !hasOpponentMill) {
							position.setState(PositionState.WIN);
							symmetricPosition.setState(PositionState.WIN);
							wins.add(position);
							wins.add(symmetricPosition);
						} else if(!haveIMill && hasOpponentMill) {
							position.setState(PositionState.ONLY_TO_LOSS);
							symmetricPosition.setState(PositionState.ONLY_TO_LOSS);
							losses.add(position);
							losses.add(symmetricPosition);
						} else if(!haveIMill && !hasOpponentMill) {
							draws.add(position);
							draws.add(symmetricPosition);
						}
					}
				}
		
		losses_with_mills = losses;
		Set<Position> newLosses = getLosses(draws, losses, PositionState.ONLY_TO_LOSS);
		draws.removeAll(newLosses);
		losses = newLosses;
		
		boolean continueSearch = false;
		do {
			Set<Position> onlyToLosses = new HashSet<Position>();
			for(Position board : draws)
				if(isReachable(board, newLosses, false) && !isReachable(board, wins, false) && !isReachable(board, draws, false))
					onlyToLosses.add(board);
			
			newLosses = getLosses(draws, onlyToLosses, PositionState.TO_LOSS);
			draws.removeAll(newLosses);
			losses.addAll(newLosses);
			continueSearch = newLosses.size() > 0;
			
			onlyToLosses = new HashSet<Position>();
			for(Position board : losses)
				if(isReachable(board, losses, false) && !isReachable(board, wins, false) && !isReachable(board, draws, false))
					onlyToLosses.add(board);
			onlyToLosses = getLosses(draws, onlyToLosses, PositionState.TO_LOSS);
			newLosses.addAll(onlyToLosses);
			draws.removeAll(onlyToLosses);
			losses.addAll(onlyToLosses);
			continueSearch = continueSearch || !onlyToLosses.isEmpty();
			
		} while(continueSearch);
		
		
		Set<Position> toWins = null;
		Set<Position> onlyToWins = null;
		do {
			toWins = getWins(draws, wins);
			draws.removeAll(toWins);
			
			Set<Position> filteredLosses = getWins(losses, wins);
			losses.removeAll(filteredLosses);
			
			onlyToWins = new HashSet<Position>();
			
			for(Position win : toWins)
				if(isReachable(win, toWins, true) && !isReachable(win, losses, true) && !isReachable(win, draws, true)) {
					win.setState(PositionState.ONLY_TO_WIN);
					onlyToWins.add(win);
				}
			
			
			for(Position draw : draws) {
				if((isReachable(draw, toWins, true) || isReachable(draw, filteredLosses, true)) && !isReachable(draw, losses, true) && !isReachable(draw, draws, true)) {
					draw.setState(PositionState.ONLY_TO_WIN);
					onlyToWins.add(draw);
				}
			}
			losses.addAll(filteredLosses);
			draws.addAll(toWins);
			draws.removeAll(onlyToWins);
			wins.addAll(onlyToWins);
		} while(!onlyToWins.isEmpty());

		
		toWins = wins;
		
		do {
			Set<Position> newWins = getWins(draws, toWins);
			toWins = new HashSet<Position>(draws);
			for(Position board : draws) {
				if(!isReachable(board, newWins, true))
					toWins.remove(board);
				else if(board.getState() != PositionState.ONLY_TO_WIN)
					board.setState(PositionState.TO_WIN);
			}
			draws.removeAll(toWins);
			wins.addAll(toWins);
		} while(toWins.size() > 0);
		
		data.addBoard(wins);
		data.addBoard(draws);
		data.addBoard(losses);
		minimaxPositions = new TreeMap<Long, Position>();
		getMove(new Position(0), true);
		getMove(new Position(0), false);
		Set<Position> additionalPositions = new HashSet<Position>();
		for(Long value : minimaxPositions.keySet())
			additionalPositions.add(minimaxPositions.get(value));
		data.addBoard(additionalPositions);
		
		System.out.println("Finish");
	}
	
	private List<Position> getAppropriateMove(Position board, List<Position> boards, PieceType type) {
		List<Position> result = new ArrayList<Position>();
		boolean isMiddleStage = board.NUMBER_OF_MY_PIECES == 3 && board.NUMBER_OF_OPPONENTS_PIECES == 3;
		for(Position to : boards) {
			if(isMiddleStage) {
				if(Position.isReachable(board, to, false))
					result.add(to);
			} else {
				long boardId = board.VALUE;
				long toId = to.VALUE;
				long difference = 0;
				for(int index = 0; index < 9; index++) {
					long boardPosition = boardId & 3;
					long toPosition = toId & 3;
					boardId = boardId >> 2;
					toId = toId >> 2;
					if(boardPosition != toPosition) difference = (difference << 2) | boardPosition | toPosition;
				}
				if(difference == type.VALUE)
					result.add(to);
			}
		}
		return result;
	}

	public Position getMove(Position board, boolean isMax) {
		PieceType type = isMax ? PieceType.MINE : PieceType.OPPONENTS;
		if(board.NUMBER_OF_MY_PIECES + board.NUMBER_OF_OPPONENTS_PIECES > (isMax ? 4 : 5)) {
			
			for(
					int rawState = isMax ? PositionState.WIN.VALUE : PositionState.ONLY_TO_LOSS.VALUE;
					isMax && rawState >= PositionState.ONLY_TO_LOSS.VALUE || !isMax && rawState <= PositionState.WIN.VALUE;
					rawState += isMax ? -1 : 1) {
				List<Position> boards = data.getBoardsByState(PositionState.getStateOf(rawState));
				if(!isMax && (board.NUMBER_OF_MY_PIECES + board.NUMBER_OF_OPPONENTS_PIECES) == 5 && rawState == PositionState.ONLY_TO_LOSS.VALUE)
					boards.addAll(losses_with_mills);
				if(!boards.isEmpty()) {
					List<Position> result = getAppropriateMove(board, boards, type);
					if(!result.isEmpty()) return result.get(random.nextInt(result.size()));
				}
			}
			
			return null;
		}
		List<Position> boards = new ArrayList<Position>();
		for(int position = 0; position < 9; position++) {
			Position variant = board.putTo(position, type);
			if(variant != null) {
				Position evaluated = minimaxPositions.get(variant.VALUE);
				if(evaluated == null) {
					if(variant.hasMill(PieceType.OPPONENTS))
						variant.setState(PositionState.ONLY_TO_LOSS);
					else if(variant.hasMill(PieceType.MINE))
						variant.setState(PositionState.WIN);
					else
						variant.setState(getMove(variant, !isMax).getState());
					if(isMax) {
						minimaxPositions.put(variant.VALUE, variant);
						long value = (variant.VALUE & 0x3F000) >> 12 | variant.VALUE & 0xFC0 | (variant.VALUE & 0x3F) << 12;
						minimaxPositions.put(value, new Position(value, variant.getState()));
						value = (variant.VALUE & 0x30C30) >> 4 | variant.VALUE & 0xC30C | (variant.VALUE & 0x30C3) << 4;
						minimaxPositions.put(value, new Position(value, variant.getState()));
						value = (value & 0x3F000) >> 12 | value & 0xFC0 | (value & 0x3F) << 12;
						minimaxPositions.put(value, new Position(value, variant.getState()));
					}
				} else
					variant = evaluated;
				boards.add(variant);
			}
		}
		List<ArrayList<Position>> positionsByState = new ArrayList<ArrayList<Position>>();
		for(int index = PositionState.ONLY_TO_LOSS.VALUE; index <= PositionState.WIN.VALUE; index++)
			positionsByState.add(new ArrayList<Position>());
		for(Position candidate : boards)
			positionsByState.get(candidate.getState().VALUE - PositionState.ONLY_TO_LOSS.VALUE).add(candidate);
		
		ListIterator<ArrayList<Position>> iterator = positionsByState.listIterator(isMax ? positionsByState.size() : 0);
		while(isMax && iterator.hasPrevious() || !isMax && iterator.hasNext()) {
			ArrayList<Position> positions = isMax ? iterator.previous() : iterator.next();
			if(!positions.isEmpty()) return positions.get(random.nextInt(positions.size()));
		}
		
		return null;
	}
	
	private Set<Position> getLosses(Set<Position> from, Set<Position> to, PositionState state) {
		Set<Position> result = new HashSet<Position>();
		for(Position board : from)
			if(isReachable(board, to, true)) {
				board.setState(state);
				result.add(board);
			}

		return result;
	}
	
	private Set<Position> getWins(Set<Position> from, Set<Position> to) {
		Set<Position> result = new HashSet<Position>();
		for(Position board : from)
			if(isReachable(board, to, false))
				result.add(board);
		return result;
	}
	
	private boolean isReachable(Position from, Collection<Position> to, boolean byOpponent) {
		for(Position position : to)
			if(Position.isReachable(from, position, byOpponent))
				return true;
		return false;
	}
	
	private Set<Position> losses_with_mills;
	private Set<Position> wins;
	private Set<Position> losses;
	private Set<Position> draws;
	private Map<Long, Position> minimaxPositions;
	private Random random;
	private Data data;
	
}
