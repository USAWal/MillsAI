package de.zilant.mills.three;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.management.ImmutableDescriptor;

import com.sun.corba.se.impl.interceptors.PICurrent;

public class Ai {
	
	public Map<PositionState, Set<Long>> getPositions() { return p; }
	
	public static void main (String ... args) {
		try {
			Data data = new Data("tmp/database");
			try {
				data.clean();
				Ai anAi = new Ai();
				data.addBoard(anAi.getPositions());
			} finally {
				data.release();
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private long increase(long line) {
		long mask = 2;
		while((line & mask) != 0) {
			line -= mask;
			mask <<= 2;
		}
		line += mask >> 1;
		return line;
	}
	
	private void fillWinsDrawsLosses() {
		for(long topLine = 0; topLine <= 0x2A; topLine = increase(topLine))
			for(long bottomLine = topLine; bottomLine <= 0x2A; bottomLine = increase(bottomLine))
				for(long centerLne = 0; centerLne <= 0x2A; centerLne = increase(centerLne)) {
					Position position = new Position(topLine << 12 | centerLne << 6 | bottomLine);
					if( position.NUMBER_OF_MY_PIECES == 3 && position.NUMBER_OF_OPPONENTS_PIECES == 3) {
						boolean haveIMill = position.hasMill(PieceType.MINE);
						boolean hasOpponentMill = position.hasMill(PieceType.OPPONENTS);
						Position symmetricPosition = new Position(bottomLine << 12 | centerLne << 6 | topLine);
						if(haveIMill && !hasOpponentMill) {
							p.get(PositionState.WIN).add(position.VALUE);
							p.get(PositionState.WIN).add(symmetricPosition.VALUE);
						} else if(!haveIMill && hasOpponentMill) {
							p.get(PositionState.LOSS).add(position.VALUE);
							p.get(PositionState.LOSS).add(symmetricPosition.VALUE);
						} else if(!haveIMill && !hasOpponentMill) {
							p.get(PositionState.DRAW).add(position.VALUE);
							p.get(PositionState.DRAW).add(symmetricPosition.VALUE);
						}
					}
				}		
	}
	
	public Ai() {
		p = new EnumMap<PositionState, Set<Long>>(PositionState.class);
		p.put(PositionState.WIN,          new TreeSet<Long>());
		p.put(PositionState.ONLY_TO_WIN,  new TreeSet<Long>());
		p.put(PositionState.TO_WIN,       new TreeSet<Long>());
		p.put(PositionState.DRAW,         new TreeSet<Long>());
		p.put(PositionState.TO_LOSS,      new TreeSet<Long>());
		p.put(PositionState.ONLY_TO_LOSS, new TreeSet<Long>());
		p.put(PositionState.LOSS,         new TreeSet<Long>());
		random = new Random();
		
		fillWinsDrawsLosses();
		
		Set<Long> losses3x3 = new HashSet<Long>(p.get(PositionState.LOSS));
		Set<Long> newLosses = getReachablePositions(p.get(PositionState.DRAW), p.get(PositionState.LOSS), true);
		p.get(PositionState.DRAW).removeAll(newLosses);
		p.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
				
		while(!newLosses.isEmpty()) {
			Set<Long> onlyToLosses = new HashSet<Long>();
			for(Long position : getReachablePositions(p.get(PositionState.DRAW), newLosses, false))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(p.get(PositionState.DRAW), onlyToLosses, true);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
		}
		newLosses = p.get(PositionState.ONLY_TO_LOSS);
		boolean prolonge = false;
		do {
			Set<Long> onlyToLosses = new HashSet<Long>();
			Set<Long> losses = new HashSet<Long>(p.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(p.get(PositionState.TO_LOSS));
			for(Long position : getReachablePositions(losses, losses, false))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(p.get(PositionState.DRAW), onlyToLosses, true);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.TO_LOSS).addAll(newLosses);
			prolonge = !newLosses.isEmpty();
			
			losses = new HashSet<Long>(p.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(p.get(PositionState.TO_LOSS));
			for(Long position : getReachablePositions(p.get(PositionState.DRAW), newLosses, false))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(p.get(PositionState.DRAW), onlyToLosses, true);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.TO_LOSS).addAll(newLosses);
		} while(!newLosses.isEmpty() || prolonge);
		
		
		Set<Long> toWins = null;
		Set<Long> onlyToWins = null;
		
		do {
			toWins = getReachablePositions(p.get(PositionState.DRAW), p.get(PositionState.WIN), false);
			toWins.addAll(getReachablePositions(p.get(PositionState.DRAW), p.get(PositionState.ONLY_TO_WIN), false));
			p.get(PositionState.DRAW).removeAll(toWins);
			
			Set<Long> filteredLosses = getReachablePositions(p.get(PositionState.TO_LOSS), p.get(PositionState.WIN), false);
			filteredLosses.addAll(getReachablePositions(p.get(PositionState.TO_LOSS), p.get(PositionState.ONLY_TO_WIN), false));
			p.get(PositionState.TO_LOSS).removeAll(filteredLosses);
			Set<Long> filteredOlnlyToLosses = getReachablePositions(p.get(PositionState.ONLY_TO_LOSS), p.get(PositionState.WIN), false);
			filteredOlnlyToLosses.addAll(getReachablePositions(p.get(PositionState.ONLY_TO_LOSS), p.get(PositionState.ONLY_TO_WIN), false));
			p.get(PositionState.ONLY_TO_LOSS).removeAll(filteredOlnlyToLosses);
			
			onlyToWins = new HashSet<Long>();
			
			for(Long win : getReachablePositions(toWins, toWins, true))
				if(!isReachable(win, p.get(PositionState.TO_LOSS), true) && !isReachable(win, p.get(PositionState.ONLY_TO_LOSS), true) && !isReachable(win, p.get(PositionState.DRAW), true))
					onlyToWins.add(win);
			
			
			for(Long draw : p.get(PositionState.DRAW))
				if((isReachable(draw, toWins, true) || isReachable(draw, filteredLosses, true) || isReachable(draw, filteredOlnlyToLosses, true)) && !isReachable(draw, p.get(PositionState.TO_LOSS), true) && !isReachable(draw, p.get(PositionState.ONLY_TO_LOSS), true) && !isReachable(draw, p.get(PositionState.DRAW), true))
					onlyToWins.add(draw);
			
			p.get(PositionState.TO_LOSS).addAll(filteredLosses);
			p.get(PositionState.ONLY_TO_LOSS).addAll(filteredOlnlyToLosses);
			p.get(PositionState.DRAW).addAll(toWins);
			p.get(PositionState.DRAW).removeAll(onlyToWins);
			p.get(PositionState.ONLY_TO_WIN).addAll(onlyToWins);
		} while(!onlyToWins.isEmpty());

		
		toWins = new HashSet<Long>(p.get(PositionState.ONLY_TO_WIN));
		toWins.addAll(p.get(PositionState.WIN));
		
		do {
			Set<Long> newWins = getReachablePositions(p.get(PositionState.DRAW), toWins, false);
			toWins = new HashSet<Long>(p.get(PositionState.DRAW));
			for(Long board : p.get(PositionState.DRAW)) {
				if(!isReachable(board, newWins, true))
					toWins.remove(board);
			}
			p.get(PositionState.DRAW).removeAll(toWins);
			p.get(PositionState.TO_WIN).addAll(toWins);
		} while(toWins.size() > 0);
		
		

		minimaxPositions = new TreeMap<Long, Position>();
		getMove(new Position(0), true);
		getMove(new Position(0), false);
		for(Position position : minimaxPositions.values())
			p.get(position.getState()).add(position.VALUE);
		p.get(PositionState.LOSS).removeAll(losses3x3);
		
		System.out.println("Finish");
	}
	
	private boolean isPieceAdded(Long from, Long to, PieceType type) {
		long difference = from ^ to;
		do {
			if(difference == type.VALUE)    return true;
			else if ((difference & 3) != 0) return false;
			else difference >>= 2;
		} while(difference != 0);	
		return false;
	}
	
	private List<Long> getAppropriateMove(Position board, Collection<Long> boards, PieceType type) {
		List<Long> result = new ArrayList<Long>();
		boolean isMiddleStage = board.NUMBER_OF_MY_PIECES == 3 && board.NUMBER_OF_OPPONENTS_PIECES == 3;
		for(Long to : boards) {
			if(isMiddleStage) {
				if(Position.isReachable(board.VALUE, to, false))
					result.add(to);
			} else {
				if(isPieceAdded(board.VALUE, to, type))
					result.add(to);
			}
		}
		return result;
	}

	public Position getMove(Position board, boolean isMax) {
		PieceType type = isMax ? PieceType.MINE : PieceType.OPPONENTS;
		if(board.NUMBER_OF_MY_PIECES + board.NUMBER_OF_OPPONENTS_PIECES > (isMax ? 4 : 5)) {
			
			for(
					int rawState = isMax ? PositionState.WIN.VALUE : PositionState.LOSS.VALUE;
					isMax && rawState >= PositionState.LOSS.VALUE || !isMax && rawState <= PositionState.WIN.VALUE;
					rawState += isMax ? -1 : 1) {
				Set<Long> boards = p.get(PositionState.getStateOf(rawState));
				//if(!isMax && (board.NUMBER_OF_MY_PIECES + board.NUMBER_OF_OPPONENTS_PIECES) == 5 && rawState == PositionState.ONLY_TO_LOSS.VALUE)
				//	boards.addAll(losses_with_mills);
				if(!boards.isEmpty()) {
					List<Long> result = getAppropriateMove(board, boards, type);
					if(!result.isEmpty()) return new Position(result.get(random.nextInt(result.size())), PositionState.getStateOf(rawState));
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
						variant.setState(PositionState.LOSS);
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
		for(int index = PositionState.LOSS.VALUE; index <= PositionState.WIN.VALUE; index++)
			positionsByState.add(new ArrayList<Position>());
		for(Position candidate : boards)
			positionsByState.get(candidate.getState().VALUE - PositionState.LOSS.VALUE).add(candidate);
		
		ListIterator<ArrayList<Position>> iterator = positionsByState.listIterator(isMax ? positionsByState.size() : 0);
		while(isMax && iterator.hasPrevious() || !isMax && iterator.hasNext()) {
			ArrayList<Position> positions = isMax ? iterator.previous() : iterator.next();
			if(!positions.isEmpty()) return positions.get(random.nextInt(positions.size()));
		}
		
		return null;
	}
	
	private Set<Long> getReachablePositions(Set<Long> from, Set<Long> to, boolean byOpponent) {
		Set<Long> result = new HashSet<Long>();
		for(Long board : from)
			if(isReachable(board, to, byOpponent))
				result.add(board);

		return result;
	}
	
	private boolean isReachable(Long from, Collection<Long> to, boolean byOpponent) {
		for(Long position : to)
			if(Position.isReachable(from, position, byOpponent))
				return true;
		return false;
	}
	
	private Map<PositionState, Set<Long>> p;
	private Map<Long, Position> minimaxPositions;
	private Random random;
	
}
