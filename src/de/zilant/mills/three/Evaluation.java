package de.zilant.mills.three;

import java.util.ArrayList;
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
import java.util.logging.Logger;

public class Evaluation {
	
	public Evaluation() {
		p = new EnumMap<PositionState, Set<Long>>(PositionState.class);
		p.put(PositionState.WIN,          new TreeSet<Long>());
		p.put(PositionState.ONLY_TO_WIN,  new TreeSet<Long>());
		p.put(PositionState.TO_WIN,       new TreeSet<Long>());
		p.put(PositionState.DRAW,         new TreeSet<Long>());
		p.put(PositionState.TO_LOSS,      new TreeSet<Long>());
		p.put(PositionState.ONLY_TO_LOSS, new TreeSet<Long>());
		p.put(PositionState.LOSS,         new TreeSet<Long>());
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluation was started.");
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\twins and losses.");
		fillWinsDrawsLosses();
		
		Set<Long> losses3x3 = new HashSet<Long>(p.get(PositionState.LOSS));
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\tonly to losses.");
		fillOnlyToLosses();
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\talso to losses");
		fillToLosses();
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\tonly to wins.");
		fillOnlyToWins();

		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluate:\talso to wins.");
		fillToWins();

		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Start minimax evaluation.");
		minimaxPositions = new TreeMap<Long, Position>();
	    startMinimaxWith(new Position(0), true);
	    startMinimaxWith(new Position(0), false);
		for(Position position : minimaxPositions.values())
			p.get(position.getState()).add(position.VALUE);
		p.get(PositionState.LOSS).removeAll(losses3x3);
		
		Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).info("Evaluation is just finished.");
	}
	
	public Map<PositionState, Set<Long>> getPositions() { return p; }
	
	public static void main (String ... args) {
		try {
			Data data = new Data("tmp/database");
			try {
				data.clean();
				Evaluation anEvaluation = new Evaluation();
				data.addBoard(anEvaluation.getPositions());
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
	
	private void fillOnlyToLosses() {
		Collection<Long> newLosses = getReachablePositions(true, p.get(PositionState.DRAW), p.get(PositionState.LOSS));
		p.get(PositionState.DRAW).removeAll(newLosses);
		p.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
				
		while(!newLosses.isEmpty()) {
			Set<Long> onlyToLosses = new HashSet<Long>();
			for(Long position : getReachablePositions(false, p.get(PositionState.DRAW), newLosses))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(true, p.get(PositionState.DRAW), onlyToLosses);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.ONLY_TO_LOSS).addAll(newLosses);
		}		
	}
	
	private void fillToLosses() {
		Collection<Long> newLosses = p.get(PositionState.ONLY_TO_LOSS);
		boolean prolonge = false;
		do {
			Set<Long> onlyToLosses = new HashSet<Long>();
			Set<Long> losses = new HashSet<Long>(p.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(p.get(PositionState.TO_LOSS));
			for(Long position : getReachablePositions(false, losses, losses))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(true, p.get(PositionState.DRAW), onlyToLosses);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.TO_LOSS).addAll(newLosses);
			prolonge = !newLosses.isEmpty();
			
			losses = new HashSet<Long>(p.get(PositionState.ONLY_TO_LOSS));
			losses.addAll(p.get(PositionState.TO_LOSS));
			for(Long position : getReachablePositions(false, p.get(PositionState.DRAW), newLosses))
				if(!isReachable(position, p.get(PositionState.WIN), false) && !isReachable(position, p.get(PositionState.DRAW), false))
					onlyToLosses.add(position);
			newLosses = getReachablePositions(true, p.get(PositionState.DRAW), onlyToLosses);
			p.get(PositionState.DRAW).removeAll(newLosses);
			p.get(PositionState.TO_LOSS).addAll(newLosses);
		} while(!newLosses.isEmpty() || prolonge);		
	}
	
	private void fillOnlyToWins() {
		Collection<Long> toWins = null;
		Set<Long> onlyToWins = null;
		
		do {
			toWins = getReachablePositions(false, p.get(PositionState.DRAW), p.get(PositionState.WIN), p.get(PositionState.ONLY_TO_WIN));
			p.get(PositionState.DRAW).removeAll(toWins);
			
			Collection<Long> filteredLosses = getReachablePositions(false, p.get(PositionState.TO_LOSS), p.get(PositionState.WIN), p.get(PositionState.ONLY_TO_WIN));
			p.get(PositionState.TO_LOSS).removeAll(filteredLosses);
			Collection<Long> filteredOlnlyToLosses = getReachablePositions(false, p.get(PositionState.ONLY_TO_LOSS), p.get(PositionState.WIN), p.get(PositionState.ONLY_TO_WIN));
			p.get(PositionState.ONLY_TO_LOSS).removeAll(filteredOlnlyToLosses);
			
			onlyToWins = new HashSet<Long>();
			
			for(Long win : getReachablePositions(true, toWins, toWins))
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
	}
	
	private void fillToWins() {
		Collection<Long> toWins = getReachablePositions(true, p.get(PositionState.DRAW), getReachablePositions(false, p.get(PositionState.DRAW), p.get(PositionState.WIN), p.get(PositionState.ONLY_TO_WIN)));
		while(!toWins.isEmpty()) {
			p.get(PositionState.DRAW).removeAll(toWins);
			p.get(PositionState.TO_WIN).addAll(toWins);
			toWins = getReachablePositions(true, p.get(PositionState.DRAW), getReachablePositions(false, p.get(PositionState.DRAW), toWins));
		}		
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

	private PositionState startMinimaxWith(Position board, boolean isMax) {
		PieceType type = isMax ? PieceType.MINE : PieceType.OPPONENTS;
		if(board.NUMBER_OF_MY_PIECES + board.NUMBER_OF_OPPONENTS_PIECES > (isMax ? 4 : 5)) {
			
			for(
					int rawState = isMax ? PositionState.WIN.VALUE : PositionState.LOSS.VALUE;
					isMax && rawState >= PositionState.LOSS.VALUE || !isMax && rawState <= PositionState.WIN.VALUE;
					rawState += isMax ? -1 : 1) {
				PositionState positionState = PositionState.getStateOf(rawState);
				Set<Long> boards = p.get(positionState);
				if(!boards.isEmpty()) {
					List<Long> result = getAppropriateMove(board, boards, type);
					if(!result.isEmpty()) return positionState;
				}
			}
			
			return null;
		}
		PositionState result = null;
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
						variant.setState(startMinimaxWith(variant, !isMax));
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
				int evaluatedState = variant.getState().VALUE;
				if(result == null || isMax && evaluatedState > result.VALUE || !isMax && evaluatedState < result.VALUE)
					result = variant.getState();
			}
		}
		
		return result;
	}
	
	private Collection<Long> getReachablePositions(boolean byOpponent, Collection<Long> from, Collection<Long> to) {
		Set<Long> result = new HashSet<Long>();
		for(Long board : from)
			if(isReachable(board, to, byOpponent))
				result.add(board);

		return result;
	}
	
	private Collection<Long> getReachablePositions( boolean byOpponent, Collection<Long> from, Collection<Long> ... toes) {
		Collection<Long> result = new HashSet<Long>();
		for(Collection<Long> to : toes)
			result.addAll(getReachablePositions(byOpponent, from, to));
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
	
}
