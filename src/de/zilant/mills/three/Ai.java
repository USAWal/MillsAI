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
		p = new EnumMap<PositionState, Set<Long>>(PositionState.class);
		p.put(PositionState.WIN,          new TreeSet<Long>());
		p.put(PositionState.ONLY_TO_WIN,  new TreeSet<Long>());
		p.put(PositionState.TO_WIN,       new TreeSet<Long>());
		p.put(PositionState.DRAW,         new TreeSet<Long>());
		p.put(PositionState.TO_LOSS,      new TreeSet<Long>());
		p.put(PositionState.ONLY_TO_LOSS, new TreeSet<Long>());
		p.put(PositionState.LOSS,         new TreeSet<Long>());
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
		
		Set<Long> losses3x3 = new HashSet<Long>(p.get(PositionState.LOSS));
		Set<Long> newLosses = getLosses(p.get(PositionState.DRAW), p.get(PositionState.LOSS));
		p.get(PositionState.DRAW).removeAll(newLosses);
		p.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
				
		while(!newLosses.isEmpty()) {
			Set<Long> onlyToLosses = new HashSet<Long>();
			for(Long position : getWins(p.get(PositionState.DRAW), newLosses))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getLosses(p.get(PositionState.DRAW), onlyToLosses);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.TO_LOSS).addAll(newLosses);
		}
		newLosses = p.get(PositionState.ONLY_TO_LOSS);
		boolean prolonge = false;
		do {
			Set<Long> onlyToLosses = new HashSet<Long>();
			Set<Long> losses = new HashSet<Long>(p.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(p.get(PositionState.TO_LOSS));
			for(Long position : getWins(losses, losses))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getLosses(p.get(PositionState.DRAW), onlyToLosses);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.TO_LOSS).addAll(newLosses);
			prolonge = !newLosses.isEmpty();
			
			losses = new HashSet<Long>(p.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(p.get(PositionState.TO_LOSS));
			for(Long position : getWins(p.get(PositionState.DRAW), newLosses))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getLosses(p.get(PositionState.DRAW), onlyToLosses);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.TO_LOSS).addAll(newLosses);
		} while(!newLosses.isEmpty() || prolonge);
		
		
		Set<Long> toWins = null;
		Set<Long> onlyToWins = null;
		
		do {
			toWins = getWins(p.get(PositionState.DRAW), p.get(PositionState.WIN));
			toWins.addAll(getWins(p.get(PositionState.DRAW), p.get(PositionState.ONLY_TO_WIN)));
			p.get(PositionState.DRAW).removeAll(toWins);
			
			Set<Long> filteredLosses = getWins(p.get(PositionState.TO_LOSS), p.get(PositionState.WIN));
			filteredLosses.addAll(getWins(p.get(PositionState.TO_LOSS), p.get(PositionState.ONLY_TO_WIN)));
			p.get(PositionState.TO_LOSS).removeAll(filteredLosses);
			Set<Long> filteredOlnlyToLosses = getWins(p.get(PositionState.ONLY_TO_LOSS), p.get(PositionState.WIN));
			filteredOlnlyToLosses.addAll(getWins(p.get(PositionState.ONLY_TO_LOSS), p.get(PositionState.ONLY_TO_WIN)));
			p.get(PositionState.ONLY_TO_LOSS).removeAll(filteredOlnlyToLosses);
			
			onlyToWins = new HashSet<Long>();
			
			for(Long win : getLosses(toWins, toWins))
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
			Set<Long> newWins = getWins(p.get(PositionState.DRAW), toWins);
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
		data.addBoard(p);
		
		System.out.println("Finish");
	}
	
	private List<Long> getAppropriateMove(Position board, Collection<Long> boards, PieceType type) {
		List<Long> result = new ArrayList<Long>();
		boolean isMiddleStage = board.NUMBER_OF_MY_PIECES == 3 && board.NUMBER_OF_OPPONENTS_PIECES == 3;
		for(Long to : boards) {
			if(isMiddleStage) {
				if(Position.isReachable(board.VALUE, to, false))
					result.add(to);
			} else {
				long boardId = board.VALUE;
				long toId = to;
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
	
	private Set<Long> getLosses(Set<Long> from, Set<Long> to) {
		Set<Long> result = new HashSet<Long>();
		for(Long board : from)
			if(isReachable(board, to, true))
				result.add(board);

		return result;
	}
	
	private Set<Long> getWins(Set<Long> from, Set<Long> to) {
		Set<Long> result = new HashSet<Long>();
		for(Long board : from)
			if(isReachable(board, to, false))
				result.add(board);
		return result;
	}
	
	private boolean isReachable(Long from, Collection<Long> to, boolean byOpponent) {
		for(Long position : to)
			if(Position.isReachable(from, position, byOpponent))
				return true;
		return false;
	}
	
	Map<PositionState, Set<Long>> p;
	private Map<Long, Position> minimaxPositions;
	private Random random;
	private Data data;
	
}
